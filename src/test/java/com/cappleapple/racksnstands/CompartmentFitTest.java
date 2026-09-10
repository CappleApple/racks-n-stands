package com.cappleapple.racksnstands;

import com.cappleapple.racksnstands.display.CompartmentFit;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

class CompartmentFitTest {
    @Test void rotatedAsymmetricWeaponsFitAllCompartmentsWithoutChangingProportions() {
        for(boolean shelf:new boolean[]{false,true}) for(int slot=0;slot<6;slot++)
            for(float angle:new float[]{0,45,90,135,180}) {
                var transform=new Matrix4f().rotateXYZ(.35F,1.57F,(float)Math.toRadians(angle))
                    .scale(.4F).translate(.2F,-.6F,.3F);
                var points=new ArrayList<Vector3f>();
                AABB bounds=null;
                for(float x:new float[]{-.25F,.5F}) for(float y:new float[]{-2,3}) for(float z:new float[]{-.6F,.2F}) {
                    var p=transform.transformPosition(new Vector3f(x,y,z));points.add(p);
                    var b=new AABB(p.x,p.y,p.z,p.x,p.y,p.z);bounds=bounds==null?b:bounds.minmax(b);
                }
                var fit=CompartmentFit.of(bounds,shelf,slot);
                double anchorX=shelf?(3+(slot%3)*5)/16.0:(slot%3+.5)/3;
                double anchorY=shelf?(slot<3?8.5:2)/16.0+.001:(slot<3?.80:.35);
                double anchorZ=shelf?.7375:.70-slot*.002;
                AABB rendered=null;
                for(var p:points) {
                    double x=anchorX+fit.x()+p.x*fit.scale(),y=anchorY+fit.y()+p.y*fit.scale(),z=anchorZ+fit.z()+p.z*fit.scale();
                    var b=new AABB(x,y,z,x,y,z);rendered=rendered==null?b:rendered.minmax(b);
                    if(shelf) {
                        assertTrue(x>(1+slot%3*5)/16.0&&x<(5+slot%3*5)/16.0,"Clear of vertical dividers");
                        assertTrue(y>(slot<3?8.5:2)/16.0&&y<(slot<3?15:7.5)/16.0,"Clear of both shelf boards");
                        assertTrue(z>9/16.0&&z<14/16.0,"Between front edge and backboard");
                    } else {
                        assertTrue(x>slot%3/3.0&&x<(slot%3+1)/3.0,"Inside rack column");
                        assertTrue(y>=(slot<3?.60:.15)-1e-7&&y<=(slot<3?1:.55)+1e-7,"Inside rack row");
                        assertTrue(z<13/16.0,"Clear of projecting wood trim");
                    }
                }
                assertEquals(fit.scale(),rendered.getXsize()/bounds.getXsize(),1e-7);
                assertEquals(fit.scale(),rendered.getYsize()/bounds.getYsize(),1e-7);
                assertEquals(fit.scale(),rendered.getZsize()/bounds.getZsize(),1e-7);
                if(shelf) assertEquals(anchorY,rendered.minY,1e-7,"Bottom rests on the board after rotation");
            }
    }

    @Test void depthLimitsAThickModelEvenWhenItsFrontFits() {
        var bounds=new AABB(-.05,-.05,-1,.05,.05,1);
        var shelf=CompartmentFit.of(bounds,true,0);
        assertEquals(.265,shelf.scale()*bounds.getZsize(),1e-7);
        var rack=CompartmentFit.of(bounds,false,0);
        assertEquals(.21,rack.scale()*bounds.getZsize(),1e-7);
    }

    @Test void smallItemsKeepTheirSizeAndShelfContact() {
        var fit=CompartmentFit.of(new AABB(.2,.4,-.1,.3,.5,0),true,3);
        assertEquals(1,fit.scale());
        assertEquals(0,.4+fit.y(),1e-7);
        assertEquals(0,.25+fit.x(),1e-7);
    }
}
