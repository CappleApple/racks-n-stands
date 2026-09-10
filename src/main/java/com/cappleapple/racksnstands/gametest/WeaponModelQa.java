package com.cappleapple.racksnstands.gametest;

import com.cappleapple.racksnstands.display.render.HandDisplayModel;
import com.google.gson.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;
import java.nio.file.Files;
import java.util.*;

/** Opt-in development geometry measurements; excluded from the distributed mod. */
final class WeaponModelQa {
    private static final class Bounds {
        final double[] low={Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY},high={Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY};
        int vertices;
        VertexConsumer consumer() {
            return new VertexConsumer() {
                public VertexConsumer addVertex(float x,float y,float z) {
                    double[] p={x,y,z};for(int i=0;i<3;i++) { low[i]=Math.min(low[i],p[i]);high[i]=Math.max(high[i],p[i]); }vertices++;return this;
                }
                public VertexConsumer setColor(int r,int g,int b,int a) { return this; }
                public VertexConsumer setUv(float u,float v) { return this; }
                public VertexConsumer setUv1(int u,int v) { return this; }
                public VertexConsumer setUv2(int u,int v) { return this; }
                public VertexConsumer setNormal(float x,float y,float z) { return this; }
            };
        }
    }
    static void capture(Minecraft client) throws Exception {
        var all=new JsonObject();var renderer=client.getItemRenderer();
        for(var item:BuiltInRegistries.ITEM) {
            var id=BuiltInRegistries.ITEM.getKey(item);if(!Set.of("cataclysm","too_many_bows","irons_spellbooks").contains(id.getNamespace())) continue;
            var stack=new ItemStack(item);var model=renderer.getModel(stack,client.level,client.player,42);
            if(item instanceof BlockItem||item instanceof ArmorItem||!(model.isCustomRenderer()||item instanceof BowItem||item instanceof SwordItem||item instanceof DiggerItem||item instanceof TridentItem||id.getNamespace().equals("irons_spellbooks"))) continue;
            try {
                var hand=HandDisplayModel.select(model,stack,ItemDisplayContext.THIRD_PERSON_RIGHT_HAND);var bounds=new Bounds();
                renderer.render(stack,ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,false,new PoseStack(),type -> bounds.consumer(),15728880,0,hand);
                var entry=new JsonObject();entry.addProperty("custom",hand.isCustomRenderer());entry.addProperty("vertices",bounds.vertices);entry.addProperty("class",item.getClass().getName());
                if(bounds.vertices>0) {
                    entry.add("low",new Gson().toJsonTree(bounds.low));entry.add("high",new Gson().toJsonTree(bounds.high));
                    var center=new double[3];for(int i=0;i<3;i++) center[i]=(bounds.low[i]+bounds.high[i])*.5;entry.add("center",new Gson().toJsonTree(center));
                }
                all.add(id.toString(),entry);
            } catch(Exception e) { var error=new JsonObject();error.addProperty("error",e.toString());all.add(id.toString(),error); }
        }
        Files.writeString(client.gameDirectory.toPath().resolve("weapon-model-bounds.json"),new GsonBuilder().setPrettyPrinting().create().toJson(all));
        com.cappleapple.racksnstands.RacksNStands.LOGGER.info("QA measured {} companion hand models",all.size());
    }
}
