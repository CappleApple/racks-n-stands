package com.cappleapple.racksnstands.display;

import java.util.*;

/** Convex visible-pixel boundary: sufficient for bounds after any affine item transform. */
public final class SpriteOutline {
    public record Point(float u,float v) {}
    public static List<Point> fromRows(int width,int height,int[] left,int[] right) {
        var points=new ArrayList<Point>();
        for(int y=0;y<height;y++) if(left[y]<right[y]) {
            float u=(float)left[y]/width,uu=(float)right[y]/width,v=(float)y/height,vv=(float)(y+1)/height;
            points.add(new Point(u,v));points.add(new Point(uu,v));points.add(new Point(u,vv));points.add(new Point(uu,vv));
        }
        points.sort(Comparator.comparingDouble(Point::u).thenComparingDouble(Point::v));
        var unique=points.stream().distinct().toList();if(unique.size()<3) return unique;
        var hull=new ArrayList<Point>();
        for(var point:unique) append(hull,point,0);
        int lower=hull.size();
        for(int i=unique.size()-2;i>=0;i--) append(hull,unique.get(i),lower-1);
        hull.removeLast();return List.copyOf(hull);
    }
    private static void append(List<Point> hull,Point p,int start) {
        while(hull.size()-start>=2) {
            var a=hull.get(hull.size()-2);var b=hull.getLast();
            if((b.u-a.u)*(p.v-a.v)-(b.v-a.v)*(p.u-a.u)>0) break;
            hull.removeLast();
        }
        hull.add(p);
    }
    /** Clip the visible boundary to the UV rectangle used by an individual baked face. */
    public static List<Point> clip(List<Point> polygon,float minU,float minV,float maxU,float maxV) {
        for(int side=0;side<4&&!polygon.isEmpty();side++) {
            var output=new ArrayList<Point>();var previous=polygon.getLast();
            float before=distance(previous,side,minU,minV,maxU,maxV);
            for(var current:polygon) {
                float after=distance(current,side,minU,minV,maxU,maxV);
                if((before>=0)!=(after>=0)) {
                    float t=before/(before-after);
                    output.add(new Point(previous.u+t*(current.u-previous.u),previous.v+t*(current.v-previous.v)));
                }
                if(after>=0) output.add(current);
                previous=current;before=after;
            }
            polygon=output;
        }
        return polygon;
    }
    private static float distance(Point p,int side,float minU,float minV,float maxU,float maxV) {
        return switch(side) { case 0 -> p.u-minU;case 1 -> maxU-p.u;case 2 -> p.v-minV;default -> maxV-p.v; };
    }
}
