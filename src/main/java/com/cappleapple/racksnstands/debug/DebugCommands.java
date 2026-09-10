package com.cappleapple.racksnstands.debug;
import com.cappleapple.racksnstands.*;
import com.cappleapple.racksnstands.block.*;
import com.cappleapple.racksnstands.blockentity.FixtureBlockEntity;
import com.cappleapple.racksnstands.compat.curios.CuriosBridge;
import com.cappleapple.racksnstands.config.FixtureConfig;
import com.cappleapple.racksnstands.display.Classification;
import com.cappleapple.racksnstands.interaction.FixtureInteraction;
import com.cappleapple.racksnstands.registry.FixtureCatalog;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import java.util.*;

public final class DebugCommands {
    public static void register(RegisterCommandsEvent event) {
        var root=Commands.literal("racksnstands").requires(s -> s.hasPermission(2));
        var debug=Commands.literal("debug");
        debug.then(Commands.literal("config").executes(c -> {
            c.getSource().sendSuccess(() -> Component.literal("Live config: max_level="+FixtureConfig.MAX_LEVEL.get()+" percent_per_level="+FixtureConfig.PERCENT.get()+" interval_ticks="+FixtureConfig.intervalTicks()+" calculation_period_seconds="+FixtureConfig.CALCULATION_PERIOD_SECONDS.get()+" automation="+FixtureConfig.AUTOMATION.get()+" allow_full_stacks="+FixtureConfig.FULL_STACKS.get()+" armor_quick_swap="+FixtureConfig.SWAP.get()+" unloaded="+FixtureConfig.UNLOADED.get()+" particles="+FixtureConfig.PARTICLES.get()+" sound="+FixtureConfig.SOUND.get()+" sound_interval_percent="+FixtureConfig.SOUND_INTERVAL_PERCENT.get()+" sound_volume="+FixtureConfig.SOUND_VOLUME.get()+" sound_pitch="+FixtureConfig.SOUND_PITCH.get()),false);return 1;
        }));
        for(String mode:List.of("fixture","repairing","classification")) debug.then(Commands.literal(mode).executes(c -> inspect(c.getSource())));
        debug.then(Commands.literal("scene").then(Commands.argument("count",IntegerArgumentType.integer(1,1000)).executes(c -> scene(c.getSource(),IntegerArgumentType.getInteger(c,"count")))));
        root.then(debug).then(Commands.literal("reload_displays").executes(c -> { c.getSource().getServer().getCommands().performPrefixedCommand(c.getSource(),"reload");return 1; }));
        event.getDispatcher().register(root);
    }
    private static int inspect(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player=source.getPlayerOrException();var ray=player.pick(8,0,false);
        if(!(ray instanceof BlockHitResult hit)) return 0;
        var state=source.getLevel().getBlockState(hit.getBlockPos());
        if(!(state.getBlock() instanceof FixtureBlock)) { source.sendFailure(Component.literal("Look at a fixture."));return 0; }
        if(!(source.getLevel().getBlockEntity(FixtureBlock.base(hit.getBlockPos(),state)) instanceof FixtureBlockEntity fixture)) return 0;
        int index=FixtureInteraction.slotAt(fixture.profile(),FixtureInteraction.local(hit.getLocation().subtract(Vec3.atLowerCornerOf(fixture.getBlockPos())),state.getValue(FixtureBlock.FACING)));
        var slot=fixture.profile().slots().get(index);var stored=fixture.displayedStack(index);var candidate=player.getMainHandItem().isEmpty()?stored:player.getMainHandItem();
        source.sendSuccess(() -> Component.literal("Fixture="+fixture.profileId()+" slot="+index+" stored="+stored+" categories="+Classification.categories(candidate,source.getLevel())
            +"\nAccepted tags="+slot.filter().acceptedTags()+" denied tags="+slot.filter().deniedTags()+" allowed items="+slot.filter().allowedItems()+" denied items="+slot.filter().deniedItems()
            +"\nEquipment="+slot.equipment()+" Curios accepted="+slot.filter().curioSlots()+" compatible="+CuriosBridge.slots(candidate,source.getLevel())+" accepts="+fixture.accepts(index,candidate)
            +"\nRotation="+fixture.resolvedTransform(index).rotation()+" scale="+fixture.resolvedTransform(index).sx()
            +"\nRepairing="+fixture.repairingLevel()+" last="+fixture.lastRepairTime()+" credit="+fixture.credit(index)+" remaining ticks="+fixture.repairProgress(index,source.getLevel().getGameTime()).remainingTicks()
            +" unloaded="+FixtureConfig.UNLOADED.get()+" visible revision="+fixture.revision()),false);
        return 1;
    }
    private static int scene(CommandSourceStack source,int count) {
        // All checks precede edits. Repeating at an occupied origin safely refuses the operation.
        BlockPos origin=BlockPos.containing(source.getPosition()).offset(4,0,4);int width=(int)Math.ceil(Math.sqrt(count));
        var kinds=FixtureCatalog.ALL.stream().filter(FixtureCatalog.Kind::available).toList();
        for(int n=0;n<count;n++) {
            BlockPos pos=origin.offset((n%width)*2,0,(n/width)*2);
            if(!source.getLevel().isEmptyBlock(pos)||!source.getLevel().isEmptyBlock(pos.above())) { source.sendFailure(Component.literal("Scene area must be empty at "+pos));return 0; }
        }
        for(int n=0;n<count;n++) {
            var kind=kinds.get(n%kinds.size());var block=RacksNStands.FIXTURES.get(kind.id()).get();BlockPos pos=origin.offset((n%width)*2,0,(n/width)*2);
            source.getLevel().setBlock(pos,block.defaultBlockState(),3);
            if(kind.tall()) source.getLevel().setBlock(pos.above(),block.defaultBlockState().setValue(FixtureBlock.HALF,DoubleBlockHalf.UPPER),3);
            var fixture=(FixtureBlockEntity)source.getLevel().getBlockEntity(pos);ItemStack host=new ItemStack(block);
            if(n%2==0) host.enchant(source.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(RacksNStands.id("repairing")).orElseThrow(),1+n%10);
            fixture.initializeFrom(host);
            if(n%4==0) continue;
            Item[] candidates={Items.DIAMOND_HELMET,Items.DIAMOND_CHESTPLATE,Items.DIAMOND_LEGGINGS,Items.DIAMOND_BOOTS,Items.IRON_PICKAXE,Items.IRON_SWORD,Items.BOW,Items.SHIELD,Items.BLAZE_ROD,Items.TRIDENT,Items.AMETHYST_SHARD};
            for(int slot=0;slot<fixture.slotCount();slot++) for(Item item:candidates) {
                var stack=new ItemStack(item);if(n%3==0&&stack.isDamageableItem()) stack.setDamageValue(stack.getMaxDamage()-1);
                if(fixture.accepts(slot,stack)) { fixture.insert(slot,stack,false);break; }
            }
        }
        source.sendSuccess(() -> Component.literal("Created "+count+" mixed fixtures at "+origin+". Use /jfr start and /jfr stop for profiling."),true);
        return count;
    }
}
