package com.cappleapple.racksnstands.display.render;

import com.cappleapple.racksnstands.blockentity.FixtureBlockEntity;
import com.cappleapple.racksnstands.api.DisplayTransform;
import com.cappleapple.racksnstands.display.CompartmentFit;
import net.minecraft.world.phys.AABB;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemStack;
import java.util.WeakHashMap;

/** Cache geometry bounds, not rendered pixels: animated models/textures still render normally each frame. */
final class PreviewLayout {
    private record Layout(BakedModel model,int revision,DisplayTransform transform,CompartmentFit fit) {}
    private final WeakHashMap<FixtureBlockEntity,Layout[]> cache=new WeakHashMap<>();
    private record Pixels(float u,float v,float uu,float vv) {}
    private static final WeakHashMap<net.minecraft.client.renderer.texture.SpriteContents,Pixels> PIXELS=new WeakHashMap<>();
    private static Pixels opaque(net.minecraft.client.renderer.texture.SpriteContents sprite) {
        return PIXELS.computeIfAbsent(sprite,image -> {
            int minX=image.width(),minY=image.height(),maxX=0,maxY=0;
            for(int frame:image.getUniqueFrames().toArray()) for(int y=0;y<image.height();y++) for(int x=0;x<image.width();x++) if(!image.isTransparent(frame,x,y)) {
                minX=Math.min(minX,x);minY=Math.min(minY,y);maxX=Math.max(maxX,x+1);maxY=Math.max(maxY,y+1);
            }
            return new Pixels((float)minX/image.width(),(float)minY/image.height(),(float)maxX/image.width(),(float)maxY/image.height());
        });
    }
    private static final class Bounds {
        double minX=Double.POSITIVE_INFINITY,minY=minX,minZ=minX,maxX=Double.NEGATIVE_INFINITY,maxY=maxX,maxZ=maxX;
        VertexConsumer consumer() {
            return new VertexConsumer() {
                public VertexConsumer addVertex(float x,float y,float z) {
                    minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);minZ=Math.min(minZ,z);maxZ=Math.max(maxZ,z);return this;
                }
                @Override public void putBulkData(PoseStack.Pose pose,net.minecraft.client.renderer.block.model.BakedQuad quad,float[] brightness,float red,float green,float blue,float alpha,int[] light,int overlay,boolean readAlpha) {
                    // Generated front/back quads include the whole transparent canvas. Clip their UV
                    // rectangle to opaque pixels; side quads already follow the item's actual outline.
                    var sprite=quad.getSprite();var pixels=opaque(sprite.contents());var data=quad.getVertices();int stride=data.length/4;
                    float[] u=new float[4],v=new float[4];
                    float lowU=Float.POSITIVE_INFINITY,lowV=lowU,highU=Float.NEGATIVE_INFINITY,highV=highU;
                    for(int i=0;i<4;i++) { u[i]=Float.intBitsToFloat(data[i*stride+4]);v[i]=Float.intBitsToFloat(data[i*stride+5]);lowU=Math.min(lowU,u[i]);highU=Math.max(highU,u[i]);lowV=Math.min(lowV,v[i]);highV=Math.max(highV,v[i]); }
                    float du=u[1]-u[0],dv=v[1]-v[0],eu=u[3]-u[0],ev=v[3]-v[0],det=du*ev-eu*dv;
                    if(Math.abs(det)<1e-12) { VertexConsumer.super.putBulkData(pose,quad,brightness,red,green,blue,alpha,light,overlay,readAlpha);return; }
                    lowU=Math.max(lowU,sprite.getU(pixels.u));highU=Math.min(highU,sprite.getU(pixels.uu));lowV=Math.max(lowV,sprite.getV(pixels.v));highV=Math.min(highV,sprite.getV(pixels.vv));
                    if(highU<lowU||highV<lowV) return;
                    for(int corner=0;corner<4;corner++) {
                        float uu=(corner%2==0?lowU:highU)-u[0],vv=(corner<2?lowV:highV)-v[0];
                        float a=Math.clamp((uu*ev-eu*vv)/det,0,1),b=Math.clamp((du*vv-uu*dv)/det,0,1);
                        var point=new org.joml.Vector3f();
                        for(int axis=0;axis<3;axis++) { float origin=Float.intBitsToFloat(data[axis]);point.setComponent(axis,origin+a*(Float.intBitsToFloat(data[stride+axis])-origin)+b*(Float.intBitsToFloat(data[3*stride+axis])-origin)); }
                        pose.pose().transformPosition(point);addVertex(point.x,point.y,point.z);
                    }
                }
                public VertexConsumer setColor(int r,int g,int b,int a) { return this; }
                public VertexConsumer setUv(float u,float v) { return this; }
                public VertexConsumer setUv1(int u,int v) { return this; }
                public VertexConsumer setUv2(int u,int v) { return this; }
                public VertexConsumer setNormal(float x,float y,float z) { return this; }
            };
        }
    }
    static void orient(PoseStack pose,DisplayTransform transform) {
        pose.mulPose(transform.quaternion());pose.scale(transform.sx(),transform.sy(),transform.sz());
        var shift=transform.modelTranslation();pose.translate(shift.get(0),shift.get(1),shift.get(2));
    }
    void apply(FixtureBlockEntity fixture,int slot,ItemStack stack,BakedModel model,ItemRenderer renderer,PoseStack pose,boolean shelf) {
        var slots=cache.computeIfAbsent(fixture,f -> new Layout[f.slotCount()]);
        var layout=slots[slot];var transform=fixture.resolvedTransform(slot);
        if(layout==null||layout.model!=model||layout.revision!=fixture.revision()||layout.transform!=transform) {
            var bounds=new Bounds();
            // One temporary measurement stack per visible update, never per rendered frame.
            // Disable glint only for measurement so wrapper buffers cannot hide the original quads.
            var measurement=stack.copy();measurement.set(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE,false);
            var oriented=new PoseStack();orient(oriented,transform);
            renderer.render(measurement,transform.context,false,oriented,type -> bounds.consumer(),15728880,0,model);
            if(!Double.isFinite(bounds.minX)||!Double.isFinite(bounds.maxX)) {
                bounds.minX=bounds.minY=bounds.minZ=-.5;bounds.maxX=bounds.maxY=bounds.maxZ=.5;
            }
            var fit=CompartmentFit.of(new AABB(bounds.minX,bounds.minY,bounds.minZ,bounds.maxX,bounds.maxY,bounds.maxZ),shelf,slot);
            layout=new Layout(model,fixture.revision(),transform,fit);
            slots[slot]=layout;
        }
        pose.translate(layout.fit.x(),layout.fit.y(),layout.fit.z());
        float scale=(float)layout.fit.scale();pose.scale(scale,scale,scale);
    }
}
