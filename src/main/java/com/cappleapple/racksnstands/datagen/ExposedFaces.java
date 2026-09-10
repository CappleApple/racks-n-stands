package com.cappleapple.racksnstands.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

/** Removes buried and coincident faces at datagen time, without runtime geometry work. */
public final class ExposedFaces {
    private record Face(String name, int axis, int u, int v, boolean positive) {}
    private record Rect(double u0, double v0, double u1, double v1) {
        List<Rect> subtract(Rect other) {
            double a=Math.max(u0,other.u0), b=Math.max(v0,other.v0);
            double c=Math.min(u1,other.u1), d=Math.min(v1,other.v1);
            if(a>=c || b>=d) return List.of(this);
            var result=new ArrayList<Rect>(4);
            if(u0<a) result.add(new Rect(u0,v0,a,v1));
            if(c<u1) result.add(new Rect(c,v0,u1,v1));
            if(v0<b) result.add(new Rect(a,v0,c,b));
            if(d<v1) result.add(new Rect(a,d,c,v1));
            return result;
        }
    }
    private static final List<Face> FACES=List.of(
        new Face("west",0,2,1,false),new Face("east",0,2,1,true),
        new Face("down",1,0,2,false),new Face("up",1,0,2,true),
        new Face("north",2,0,1,false),new Face("south",2,0,1,true));

    public static JsonArray build(JsonArray cubes) {
        var output=new JsonArray();
        for(int i=0;i<cubes.size();i++) {
            var cube=cubes.get(i).getAsJsonObject();
            double[] lo=coordinates(cube,"from"), hi=coordinates(cube,"to");
            var retained=cube.deepCopy();
            var retainedFaces=new JsonObject();retained.add("faces",retainedFaces);
            for(var face:FACES) {
                double plane=face.positive?hi[face.axis]:lo[face.axis];
                var original=new Rect(lo[face.u],lo[face.v],hi[face.u],hi[face.v]);
                List<Rect> pieces=List.of(original);
                for(int j=0;j<cubes.size() && !pieces.isEmpty();j++) {
                    if(i==j) continue;
                    var other=cubes.get(j).getAsJsonObject();
                    double[] min=coordinates(other,"from"),max=coordinates(other,"to");
                    if(plane<min[face.axis] || plane>max[face.axis]) continue;
                    boolean sameSurface=plane==(face.positive?max[face.axis]:min[face.axis]);
                    // Later material wins only on coincident outward faces. Both
                    // opposite faces disappear where solids touch at a joint.
                    if(sameSurface && j<i) continue;
                    var mask=new Rect(min[face.u],min[face.v],max[face.u],max[face.v]);
                    var remaining=new ArrayList<Rect>();
                    for(var piece:pieces) remaining.addAll(piece.subtract(mask));
                    pieces=remaining;
                }
                var definition=cube.getAsJsonObject("faces").getAsJsonObject(face.name);
                if(pieces.size()==1 && pieces.getFirst().equals(original)) {
                    retainedFaces.add(face.name,definition.deepCopy());
                    continue;
                }
                for(var piece:pieces) {
                    double[] min=lo.clone(),max=hi.clone();
                    min[face.u]=piece.u0;max[face.u]=piece.u1;
                    min[face.v]=piece.v0;max[face.v]=piece.v1;
                    var visible=definition.deepCopy();
                    var uv=definition.getAsJsonArray("uv");
                    double u0=(piece.u0-original.u0)/(original.u1-original.u0);
                    double u1=(piece.u1-original.u0)/(original.u1-original.u0);
                    double v0=(piece.v0-original.v0)/(original.v1-original.v0);
                    double v1=(piece.v1-original.v0)/(original.v1-original.v0);
                    if(face.name.equals("north") || face.name.equals("east")) { double old=u0;u0=1-u1;u1=1-old; }
                    if(!face.name.equals("up")) { double old=v0;v0=1-v1;v1=1-old; }
                    visible.add("uv",FixtureData.array(lerp(uv,0,2,u0),lerp(uv,1,3,v0),lerp(uv,0,2,u1),lerp(uv,1,3,v1)));
                    var faces=new JsonObject();faces.add(face.name,visible);
                    var element=new JsonObject();
                    element.add("from",FixtureData.array(min[0],min[1],min[2]));
                    element.add("to",FixtureData.array(max[0],max[1],max[2]));
                    element.add("faces",faces);output.add(element);
                }
            }
            if(!retainedFaces.isEmpty()) output.add(retained);
        }
        return output;
    }
    private static double[] coordinates(JsonObject cube,String field) {
        var a=cube.getAsJsonArray(field);
        return new double[]{a.get(0).getAsDouble(),a.get(1).getAsDouble(),a.get(2).getAsDouble()};
    }
    private static double lerp(JsonArray uv,int from,int to,double fraction) {
        double start=uv.get(from).getAsDouble();
        return start+(uv.get(to).getAsDouble()-start)*fraction;
    }
}
