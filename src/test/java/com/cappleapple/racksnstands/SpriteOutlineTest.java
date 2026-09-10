package com.cappleapple.racksnstands;

import com.cappleapple.racksnstands.display.CompartmentFit;
import com.cappleapple.racksnstands.display.SpriteOutline;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SpriteOutlineTest {
    @Test void transparentCornersDoNotShrinkAnUprightDiagonalWeapon() {
        int[] left=new int[16],right=new int[16];
        for(int y=0;y<16;y++) { left[y]=y;right[y]=y+1; }
        var outline=SpriteOutline.fromRows(16,16,left,right);
        AABB bounds=null;double rotation=Math.PI/4;
        for(var point:outline) {
            double u=(point.u()-.5)*.5,v=(point.v()-.5)*.5;
            double x=u*Math.cos(rotation)-v*Math.sin(rotation),y=u*Math.sin(rotation)+v*Math.cos(rotation);
            var corner=new AABB(x,y,-.02,x,y,.02);bounds=bounds==null?corner:bounds.minmax(corner);
        }
        assertTrue(bounds.getXsize()<.05,"Only the thin visible blade contributes to width");
        assertEquals(1,CompartmentFit.of(bounds,false,0,4).scale(),"The full-size narrow weapon fits a one-row rack");
    }
    @Test void fullTexturesRetainTheirCompleteFaceAndPartialUvsClipCorrectly() {
        int[] left=new int[16],right=new int[16];java.util.Arrays.fill(right,16);
        var outline=SpriteOutline.fromRows(16,16,left,right);assertEquals(4,outline.size());
        var clipped=SpriteOutline.clip(outline,.25F,.125F,.75F,.875F);assertEquals(4,clipped.size());
        assertEquals(.25,clipped.stream().mapToDouble(SpriteOutline.Point::u).min().orElseThrow());
        assertEquals(.75,clipped.stream().mapToDouble(SpriteOutline.Point::u).max().orElseThrow());
        assertEquals(.125,clipped.stream().mapToDouble(SpriteOutline.Point::v).min().orElseThrow());
        assertEquals(.875,clipped.stream().mapToDouble(SpriteOutline.Point::v).max().orElseThrow());
    }
    @Test void fullyTransparentSpritesContributeNoBounds() {
        int[] left={4,4,4,4},right={0,0,0,0};
        assertTrue(SpriteOutline.fromRows(4,4,left,right).isEmpty());
    }
}
