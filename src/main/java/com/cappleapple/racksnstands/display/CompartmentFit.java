package com.cappleapple.racksnstands.display;

import net.minecraft.world.phys.AABB;

/** Uniform fit of already oriented geometry, in fixture coordinates. */
public record CompartmentFit(double scale,double x,double y,double z) {
    public static CompartmentFit of(AABB bounds,boolean shelf,int slot) { return of(bounds,shelf,slot,6); }
    public static CompartmentFit of(AABB bounds,boolean shelf,int slot,int slots) {
        // Shelf clearances: 4px wide, 6.5px/5.5px high, and 5px deep.
        // Leave a small margin around the frame; the anchor already sits above the board.
        double width=shelf?.24:1.0/(slots==6?3:slots)-.04;
        double height=shelf?(slot<3?.395:.332):(slots==6?.40:.78);
        double depth=shelf?.265:.21;
        double scale=Math.min(1,Math.min(width/Math.max(.001,bounds.getXsize()),
            Math.min(height/Math.max(.001,bounds.getYsize()),depth/Math.max(.001,bounds.getZsize()))));
        return new CompartmentFit(scale,-scale*(bounds.minX+bounds.maxX)/2,
            -scale*(shelf?bounds.minY:(bounds.minY+bounds.maxY)/2),-scale*(bounds.minZ+bounds.maxZ)/2);
    }
}
