package com.cappleapple.racksnstands.gametest;

import com.cappleapple.racksnstands.RacksNStands;
import com.cappleapple.racksnstands.block.FixtureBlock;
import com.cappleapple.racksnstands.blockentity.FixtureBlockEntity;
import com.cappleapple.racksnstands.material.MaterialPalette;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.client.model.data.ModelData;
import java.nio.file.*;
import java.util.*;

/** Explicit local QA actions, excluded from the released JAR along with all GameTests. */
public final class MaterialVisualQa {
    private static int distance=-1;
    public static void tick() {
        var client=Minecraft.getInstance();
        int current=com.cappleapple.racksnstands.config.FixtureConfig.RENDER_DISTANCE.get();
        if(current!=distance) { distance=current;RacksNStands.LOGGER.info("QA live client render distance={}",current); }
        var path=client.gameDirectory.toPath().resolve("material-qa-action.txt");
        if(!Files.isRegularFile(path)) return;
        try {
            var action=Files.readString(path).trim();Files.delete(path);
            if(action.equals("sample")) {
                var ray=client.player.pick(client.player.blockInteractionRange(),1,false);
                if(!(ray instanceof BlockHitResult hit)) throw new IllegalStateException("No block hit");
                client.player.setShiftKeyDown(true);
                client.player.connection.send(new net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket(client.player,net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.PRESS_SHIFT_KEY));
                var result=client.gameMode.useItemOn(client.player,InteractionHand.MAIN_HAND,hit);
                client.player.connection.send(new net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket(client.player,net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.RELEASE_SHIFT_KEY));
                client.player.setShiftKeyDown(false);RacksNStands.LOGGER.info("QA sampled {} at {} result={}",client.player.getMainHandItem(),hit.getLocation(),result);
            } else if(action.equals("weapons")) WeaponModelQa.capture(client);
            else if(action.equals("models")) check(client);
            else if(action.equals("repair")) {
                var hit=(net.minecraft.world.phys.BlockHitResult)client.player.pick(client.player.blockInteractionRange(),1,false);
                var target=com.cappleapple.racksnstands.display.RepairTarget.find(client.level,hit);
                require(target!=null&&target.hasDurability(),"No repairable target");
                RacksNStands.LOGGER.info("QA REPAIR HUD: jade={} slot={} item={} damage={} percent={} etaTicks={}",net.neoforged.fml.ModList.get().isLoaded("jade"),target.slot(),target.stack().getHoverName().getString(),target.stack().getDamageValue(),target.progress().completion(),target.progress().remainingTicks());
            }
        } catch(Exception error) { RacksNStands.LOGGER.error("Material QA failed",error); }
    }
    private static void require(boolean condition,String message) { if(!condition) throw new IllegalStateException(message); }
    private static void check(Minecraft client) {
        for(int x:new int[]{126,128,130,132,134,136,138}) {
            var pos=new BlockPos(x+114,65,54);var fixture=(FixtureBlockEntity)client.level.getBlockEntity(pos);
            require(fixture!=null,"Fixture missing at "+pos);
            var model=client.getBlockRenderer().getBlockModel(fixture.getBlockState());
            var data=model.getModelData(client.level,pos,fixture.getBlockState(),ModelData.EMPTY);
            var quads=model.getQuads(fixture.getBlockState(),null,RandomSource.create(42),data,null);
            require(!quads.isEmpty(),"No world quads at "+pos);
            require(quads.stream().noneMatch(q -> q.isTinted()),"Material metadata leaked to tint indices");
            int layers=0;
            for(var type:model.getRenderTypes(fixture.getBlockState(),RandomSource.create(42),data)) layers+=model.getQuads(fixture.getBlockState(),null,RandomSource.create(42),data,type).size();
            require(layers==quads.size(),"Render layer count mismatch");
            var sprites=new TreeSet<String>();for(var quad:quads) sprites.add(quad.getSprite().contents().name().toString());
            if(x==128) require(sprites.contains("minecraft:block/gold_block")&&sprites.contains("minecraft:block/bricks"),"Applied materials not visible in client mesh");
            if(x==130) require(sprites.contains("minecraft:block/oak_log_top")&&sprites.contains("minecraft:block/oak_log"),"Log face textures lost: "+sprites+" palette="+fixture.materials());
            if(x==132) require(quads.stream().anyMatch(q -> q.getVertices()[3]!=-1),"Grass biome tint lost");
            if(x==134) require(model.getRenderTypes(fixture.getBlockState(),RandomSource.create(42),data).contains(net.minecraft.client.renderer.RenderType.translucent()),"Glass translucency lost");
            RacksNStands.LOGGER.info("QA world material mesh {}: {} quads {}",x,quads.size(),sprites);
            if(((FixtureBlock)fixture.getBlockState().getBlock()).kind().tall()) {
                var upper=fixture.getBlockState().setValue(FixtureBlock.HALF,DoubleBlockHalf.UPPER);var top=client.getBlockRenderer().getBlockModel(upper);
                var topData=top.getModelData(client.level,pos.above(),upper,ModelData.EMPTY);
                require(top.getQuads(upper,null,RandomSource.create(42),topData,null).stream().anyMatch(q -> q.getSprite().contents().name().getPath().equals("block/oak_log")),"Upper mannequin palette did not follow lower block entity");
            }
        }
        var stack=new ItemStack(RacksNStands.FIXTURES.get("sword_floor_stand").get());
        stack.set(RacksNStands.MATERIALS.get(),MaterialPalette.EMPTY.with("dark",Blocks.BRICKS.defaultBlockState()).with("metal",Blocks.GOLD_BLOCK.defaultBlockState()));
        var model=client.getItemRenderer().getModel(stack,client.level,client.player,42);
        for(var context:List.of(ItemDisplayContext.GUI,ItemDisplayContext.GROUND,ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)) {
            var transformed=model.applyTransform(context,new PoseStack(),false);
            var quads=transformed.getQuads(null,null,RandomSource.create(42));
            require(quads.stream().anyMatch(q -> q.getSprite().contents().name().getPath().equals("block/gold_block")),"Item appearance lost in "+context);
            require(quads.stream().anyMatch(q -> q.getSprite().contents().name().getPath().equals("block/bricks")),"Item base material lost");
        }
        RacksNStands.LOGGER.info("QA MATERIAL MODEL CHECKS PASSED: world, layers, tint, upper half, GUI, ground and hand");
    }
}
