package com.cappleapple.racksnstands.block;
import com.cappleapple.racksnstands.registry.FixtureCatalog;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.*;
import java.util.List;

/** Shared furniture solids used by both baked models and collision/selection shapes. */
public final class FixtureGeometry {
    public record Part(double x,double y,double z,double xx,double yy,double zz,String texture) {}
    private static Part part(double x,double y,double z,double xx,double yy,double zz,String texture) { return new Part(x,y,z,xx,yy,zz,texture); }
    public static List<Part> parts(FixtureCatalog.Kind kind) {
        var cubes=new java.util.ArrayList<Part>();String style=kind.style();
        if(kind.wall()&&!style.equals("curio_cabinet")) {
            cubes.add(part(1,1,0,15,15,1.5,"wood"));
            cubes.add(part(0,0,0,16,2,3,"dark"));cubes.add(part(0,14,0,16,16,3,"dark"));
            if(kind.slots()>1) {
                int columns=kind.slots()==6?3:kind.slots();
                for(int i=0;i<columns;i++) { double x=(i+.5)*16/columns;cubes.add(part(x-.5,4,1,x+.5,7,4,"metal"));if(kind.slots()==6)cubes.add(part(x-.5,11,1,x+.5,14,4,"metal")); }
            } else cubes.add(part(7,5,1,9,10,4,"metal"));
        } else if(style.equals("pedestal")) {
            // Full-height pedestal: broad plinth, narrow shaft, stepped capital and open blade slot.
            cubes.add(part(0,0,0,16,2,16,"base"));
            cubes.add(part(1,2,1,15,4,15,"stone"));
            cubes.add(part(3,4,3,13,6,13,"stone"));
            for(double[] tier:new double[][]{{3,6,13,12},{2,12,14,14},{0,14,16,16}}) {
                double lo=tier[0],bottom=tier[1],hi=tier[2],top=tier[3];
                String material=bottom==14?"trim":"stone";
                cubes.add(part(lo,bottom,lo,5,top,hi,material));cubes.add(part(11,bottom,lo,hi,top,hi,material));
                cubes.add(part(5,bottom,lo,11,top,7,material));cubes.add(part(5,bottom,9,11,top,hi,material));
            }
        } else {
            if(style.equals("curio_cabinet")) cubes.add(part(0,0,2,16,2,9,"dark"));
            else cubes.add(part(2,0,2,14,2,14,"dark"));
            if(kind.tall()) {
                cubes.add(part(7,2,7,9,25,9,"wood"));cubes.add(part(4,22,6,12,26,10,"wood"));
                cubes.add(part(1,23,7,15,25,9,"wood"));cubes.add(part(7,25,7,9,28,9,"wood"));cubes.add(part(4.5,27.8,4.5,11.5,34.8,11.5,"wood"));
                cubes.add(part(5,2,7,7,15,9,"wood"));cubes.add(part(9,2,7,11,15,9,"wood"));
            } else if(style.equals("table")) { cubes.add(part(1,2,1,15,3,15,"wood"));cubes.add(part(3,3,3,13,3.5,13,"cloth")); }
            else if(style.equals("armor")) {
                switch(kind.equipment()) {
                    case "head" -> {
                        cubes.add(part(7,2,7,9,10,9,"metal"));
                        cubes.add(part(5.25,9.6,5.25,10.75,15,10.75,"wood"));
                    }
                    case "chest" -> {
                        cubes.add(part(7,2,7,9,5.5,9,"metal"));
                        cubes.add(part(5.2,5,6.6,10.8,14.8,9.4,"cloth"));
                        cubes.add(part(7,14.8,7,9,16,9,"wood"));
                    }
                    case "legs" -> {
                        cubes.add(part(5,2,6.8,7.2,13,9.2,"wood"));
                        cubes.add(part(8.8,2,6.8,11,13,9.2,"wood"));
                        cubes.add(part(5,12,6.8,11,14.5,9.2,"wood"));
                    }
                    case "feet" -> {
                        cubes.add(part(5,2,6.8,7.2,5.3,9.2,"wood"));
                        cubes.add(part(8.8,2,6.8,11,5.3,9.2,"wood"));
                    }
                }
            } else if(style.equals("curio_bust")||style.equals("curio_belt")) {
                cubes.add(part(7,2,7,9,10,9,"metal"));
                cubes.add(part(4,7,5,12,13,11,"cloth"));cubes.add(part(6,13,6,10,15,10,"cloth"));
            } else if(style.equals("curio_cabinet")) {
                cubes.add(part(0,0,7,16,16,9,"wood"));cubes.add(part(0,0,2,1,16,9,"dark"));cubes.add(part(15,0,2,16,16,9,"dark"));
                cubes.add(part(0,15,2,16,16,9,"dark"));cubes.add(part(1,7.5,2,15,8.5,9,"dark"));
                for(int x:List.of(5,10)) cubes.add(part(x,1,3,x+1,15,8,"wood"));
            } else if(style.equals("curio_ring")||style.equals("curio_bracelet")) {
                cubes.add(part(3,2,5,13,4,11,"cloth"));
                if(style.endsWith("bracelet")) { cubes.add(part(7,4,7,9,8,9,"metal"));cubes.add(part(2,7,7,14,9,9,"wood")); }
                else { cubes.add(part(3.5,4,7,4.5,9,8,"metal"));cubes.add(part(11.5,4,7,12.5,9,8,"metal")); }
            } else if(style.equals("floor")) {
                cubes.add(part(4,2,7.25,5,11,8.75,"metal"));cubes.add(part(11,2,7.25,12,11,8.75,"metal"));
            } else {
                cubes.add(part(6,2,6,10,8,10,"wood"));cubes.add(part(3,8,3,13,9,13,"dark"));
            }
        }
        if(style.equals("curio_cabinet")) for(int n=0;n<cubes.size();n++) {
            var part=cubes.get(n);cubes.set(n,new Part(part.x(),part.y(),part.z()+7,part.xx(),part.yy(),part.zz()+7,part.texture()));
        }
        else if(kind.wall()) for(int n=0;n<cubes.size();n++) {
            var part=cubes.get(n);cubes.set(n,new Part(part.x(),part.y(),16-part.zz(),part.xx(),part.yy(),16-part.z(),part.texture()));
        }
        return java.util.List.copyOf(cubes);
    }
    /** Include the full worn torso and sleeves in chest targeting, while keeping model solids separate. */
    public static List<Part> selectionParts(FixtureCatalog.Kind kind) {
        var result=new java.util.ArrayList<>(parts(kind));
        if(kind.tall()) result.add(part(1,14.4,4,15,27.2,12,"wood"));
        return List.copyOf(result);
    }
    public static VoxelShape shape(List<Part> parts,Direction facing,boolean upper) {
        int turns=switch(facing) { case EAST -> 1;case SOUTH -> 2;case WEST -> 3;default -> 0; };
        double bottom=upper?16:0,top=upper?Double.POSITIVE_INFINITY:16;
        VoxelShape shape=Shapes.empty();
        for(var part:parts) {
            double y=Math.max(bottom,part.y()),yy=Math.min(top,part.yy());
            if(yy<=y) continue;
            double x=part.x(),xx=part.xx(),z=part.z(),zz=part.zz();
            for(int n=0;n<turns;n++) { double nextX=16-zz,nextXX=16-z;z=x;zz=xx;x=nextX;xx=nextXX; }
            shape=Shapes.or(shape,Shapes.box(x/16,(y-bottom)/16,z/16,xx/16,(yy-bottom)/16,zz/16));
        }
        return shape.optimize();
    }
}
