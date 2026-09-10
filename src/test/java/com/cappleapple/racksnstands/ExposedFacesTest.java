package com.cappleapple.racksnstands;

import com.cappleapple.racksnstands.datagen.ExposedFaces;
import com.cappleapple.racksnstands.datagen.FixtureData;
import com.google.gson.JsonArray;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ExposedFacesTest {
    private static com.google.gson.JsonObject cube(double x,double end) {
        var faces=new com.google.gson.JsonObject();
        for(var name:java.util.List.of("west","east","up","down","north","south"))
            faces.add(name,FixtureData.object("texture","#wood","uv",FixtureData.array(0,0,16,16)));
        return FixtureData.object("from",FixtureData.array(x,0,0),"to",FixtureData.array(end,1,1),"faces",faces);
    }
    private static double surface(JsonArray elements) {
        double area=0;
        for(var e:elements) {
            var element=e.getAsJsonObject();var a=element.getAsJsonArray("from");var b=element.getAsJsonArray("to");
            double x=b.get(0).getAsDouble()-a.get(0).getAsDouble();
            double y=b.get(1).getAsDouble()-a.get(1).getAsDouble();
            double z=b.get(2).getAsDouble()-a.get(2).getAsDouble();
            for(var face:element.getAsJsonObject("faces").keySet())
                area+=switch(face) { case "west","east" -> y*z;case "up","down" -> x*z;default -> x*y; };
        }
        return area;
    }
    @Test void touchingSolidsRemoveBothBuriedFaces() {
        assertEquals(10,surface(ExposedFaces.build(FixtureData.array(cube(0,1),cube(1,2)))),.00001);
    }
    @Test void intersectingSolidsHaveOnlyTheirUnionSurface() {
        assertEquals(8,surface(ExposedFaces.build(FixtureData.array(cube(0,1),cube(.5,1.5)))),.00001);
    }
    @Test void duplicateSolidsDoNotDuplicateAnyFace() {
        assertEquals(6,surface(ExposedFaces.build(FixtureData.array(cube(0,1),cube(0,1)))),.00001);
    }
    @Test void croppedTopAndBottomKeepTheirOriginalTextureCoordinates() {
        var base=cube(0,16);base.add("to",FixtureData.array(16,2,16));
        var support=cube(4,5);support.add("from",FixtureData.array(4,2,7));support.add("to",FixtureData.array(5,11,9));
        // Mirror the same support underneath to exercise the opposite V orientation too.
        var below=support.deepCopy();below.add("from",FixtureData.array(4,-9,7));below.add("to",FixtureData.array(5,0,9));
        var result=ExposedFaces.build(FixtureData.array(base,support,below));
        int pieces=0;
        for(var value:result) {
            var element=value.getAsJsonObject();var from=element.getAsJsonArray("from");var to=element.getAsJsonArray("to");
            for(var face:java.util.List.of("up","down")) {
                if(!element.getAsJsonObject("faces").has(face)||!element.getAsJsonObject("faces").getAsJsonObject(face).get("texture").getAsString().equals("#wood")) continue;
                double plane=(face.equals("up")?to:from).get(1).getAsDouble();if(plane!=(face.equals("up")?2:0)) continue;
                var uv=element.getAsJsonObject("faces").getAsJsonObject(face).getAsJsonArray("uv");
                assertEquals(from.get(0).getAsDouble(),uv.get(0).getAsDouble(),.00001);
                assertEquals(to.get(0).getAsDouble(),uv.get(2).getAsDouble(),.00001);
                assertEquals(face.equals("up")?from.get(2).getAsDouble():16-to.get(2).getAsDouble(),uv.get(1).getAsDouble(),.00001);
                assertEquals(face.equals("up")?to.get(2).getAsDouble():16-from.get(2).getAsDouble(),uv.get(3).getAsDouble(),.00001);pieces++;
            }
        }
        assertEquals(8,pieces);
    }
}
