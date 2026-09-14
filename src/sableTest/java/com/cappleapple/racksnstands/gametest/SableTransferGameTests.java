package com.cappleapple.racksnstands.gametest;

import com.cappleapple.racksnstands.RacksNStands;
import com.cappleapple.racksnstands.block.FixtureBlock;
import com.cappleapple.racksnstands.blockentity.FixtureBlockEntity;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Uses the installed Sable implementation; excluded from the published mod JAR. */
@GameTestHolder(RacksNStands.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SableTransferGameTests {
    private static final String EMPTY = "bastion/mobs/empty";
    private static final BlockPos POS = new BlockPos(1, 2, 1);

    @GameTest(templateNamespace = "minecraft", template = EMPTY, batch = "sable_transfer_rack_round_trip")
    public static void rackRoundTripPreservesContentsAndMetadata(GameTestHelper h) {
        roundTrip(h, "tool_rack", false);
    }

    @GameTest(templateNamespace = "minecraft", template = EMPTY, batch = "sable_transfer_mannequin_lower_first")
    public static void mannequinLowerFirstRoundTripPreservesBothHalves(GameTestHelper h) {
        roundTrip(h, "armor_mannequin", false);
    }

    @GameTest(templateNamespace = "minecraft", template = EMPTY, batch = "sable_transfer_mannequin_upper_first")
    public static void mannequinUpperFirstRoundTripPreservesBothHalves(GameTestHelper h) {
        roundTrip(h, "armor_mannequin", true);
    }

    @GameTest(templateNamespace = "minecraft", template = EMPTY, batch = "sable_transfer_rack_break")
    public static void ordinaryRackBreakOnShipDropsContentsOnce(GameTestHelper h) {
        breakOnShip(h, "tool_rack");
    }

    @GameTest(templateNamespace = "minecraft", template = EMPTY, batch = "sable_transfer_mannequin_break")
    public static void ordinaryMannequinUpperBreakOnShipDropsContentsOnce(GameTestHelper h) {
        breakOnShip(h, "armor_mannequin");
    }

    private static void roundTrip(GameTestHelper h, String id, boolean upperFirst) {
        var fixture = fixture(h, id);
        var sourcePos = fixture.getBlockPos();
        var expected = fixture.saveWithFullMetadata(h.getLevel().registryAccess());
        var expectedItems = contents(fixture);
        var originalEntities = itemEntityIds(h.getLevel());
        var sourceBlocks = blocks(sourcePos, fixture, upperFirst);
        var ship = SubLevelAssemblyHelper.assembleBlocks(h.getLevel(), sourcePos, sourceBlocks, BoundingBox3i.from(sourceBlocks));
        var plotPos = ship.getPlot().getCenterBlock();
        assertMoved(h, sourcePos, plotPos, fixture, expected, expectedItems, originalEntities);
        // Sable applies physics and block-topology updates on the following server ticks.
        h.runAfterDelay(2, () -> {
            var aboard = (FixtureBlockEntity) h.getLevel().getBlockEntity(plotPos);
            var beforeReturn = aboard.saveWithFullMetadata(h.getLevel().registryAccess());
            var returningItems = contents(aboard);
            // Retain the support aboard so moving the fixtures does not delete a live physics plot.
            var shipBlocks = sourceBlocks.stream().filter(pos -> !pos.equals(sourcePos.below()))
                    .map(pos -> pos.subtract(sourcePos).offset(plotPos)).toList();
            h.getLevel().setBlock(sourcePos.below(), Blocks.STONE.defaultBlockState(), 3);
            var transform = new SubLevelAssemblyHelper.AssemblyTransform(plotPos, sourcePos, 0, Rotation.NONE, h.getLevel());
            // Create Aeronautics' SimAssemblyHelper uses this same operation when disassembling.
            SubLevelAssemblyHelper.moveBlocks(h.getLevel(), transform, shipBlocks);
            assertMoved(h, plotPos, sourcePos, fixture, beforeReturn, returningItems, originalEntities);
            assertOrdinaryBreak(h, sourcePos, fixture, returningItems, originalEntities);
            finishAfterPhysicsUpdates(h, ship, originalEntities);
        });
    }

    private static void breakOnShip(GameTestHelper h, String id) {
        var fixture = fixture(h, id);
        var sourcePos = fixture.getBlockPos();
        var expected = fixture.saveWithFullMetadata(h.getLevel().registryAccess());
        var expectedItems = contents(fixture);
        var originalEntities = itemEntityIds(h.getLevel());
        var sourceBlocks = blocks(sourcePos, fixture, false);
        var ship = SubLevelAssemblyHelper.assembleBlocks(h.getLevel(), sourcePos, sourceBlocks, BoundingBox3i.from(sourceBlocks));
        var plotPos = ship.getPlot().getCenterBlock();
        assertMoved(h, sourcePos, plotPos, fixture, expected, expectedItems, originalEntities);
        h.runAfterDelay(2, () -> {
            var aboard = (FixtureBlockEntity) h.getLevel().getBlockEntity(plotPos);
            assertOrdinaryBreak(h, plotPos, fixture, contents(aboard), originalEntities);
            finishAfterPhysicsUpdates(h, ship, originalEntities);
        });
    }

    private static void finishAfterPhysicsUpdates(GameTestHelper h, ServerSubLevel ship, Set<UUID> originalEntities) {
        h.runAfterDelay(2, () -> {
            removeShip(h, ship);
            newItemEntities(h.getLevel(), originalEntities).forEach(ItemEntity::discard);
            h.runAfterDelay(2, h::succeed);
        });
    }
    private static FixtureBlockEntity fixture(GameTestHelper h, String id) {
        var block = RacksNStands.FIXTURES.get(id).get();
        h.setBlock(POS.below(), Blocks.STONE);
        h.setBlock(POS, block);
        if (block.kind().tall()) h.setBlock(POS.above(), block.defaultBlockState().setValue(FixtureBlock.HALF, DoubleBlockHalf.UPPER));
        var fixture = (FixtureBlockEntity) h.getBlockEntity(POS);
        var host = new ItemStack(block);
        host.enchant(h.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(RacksNStands.id("repairing")).orElseThrow(), 3);
        fixture.initializeFrom(host);
        h.assertTrue(fixture.setMaterial("wood", Blocks.BLUE_TERRACOTTA.defaultBlockState()), "Fixture accepts the test appearance");
        Item[] armor = {Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS};
        for (int slot = 0; slot < fixture.slotCount(); slot++) {
            var item = new ItemStack(block.kind().tall() ? armor[slot] : Items.DIAMOND_PICKAXE);
            item.setDamageValue(60 + slot);
            item.set(DataComponents.CUSTOM_NAME, Component.literal("Sable transfer slot " + slot));
            item.enchant(h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING), 2);
            h.assertTrue(fixture.insert(slot, item, false).isEmpty(), "Test gear fills every storage slot");
        }
        var saved = fixture.saveWithFullMetadata(h.getLevel().registryAccess());
        var fractions = new ListTag();
        var sounds = new ListTag();
        for (int slot = 0; slot < fixture.slotCount(); slot++) {
            fractions.add(DoubleTag.valueOf(.25 + slot * .05));
            sounds.add(DoubleTag.valueOf(12.5 + slot));
        }
        saved.put("RepairFractions", fractions);
        saved.put("RepairSoundProgress", sounds);
        fixture.loadWithComponents(saved, h.getLevel().registryAccess());
        return fixture;
    }

    private static List<BlockPos> blocks(BlockPos pos, FixtureBlockEntity fixture, boolean upperFirst) {
        var result = new ArrayList<BlockPos>();
        // The support keeps the ship alive while testing ordinary fixture destruction.
        result.add(pos.below());
        boolean tall = ((FixtureBlock) fixture.getBlockState().getBlock()).kind().tall();
        if (tall && upperFirst) result.add(pos.above());
        result.add(pos);
        if (tall && !upperFirst) result.add(pos.above());
        return result;
    }

    private static List<ItemStack> contents(FixtureBlockEntity fixture) {
        var result = new ArrayList<ItemStack>();
        for (int slot = 0; slot < fixture.slotCount(); slot++) result.add(fixture.displayedStack(slot).copy());
        return result;
    }

    private static void assertMoved(GameTestHelper h, BlockPos source, BlockPos destination,
                                    FixtureBlockEntity original, CompoundTag expected,
                                    List<ItemStack> expectedItems, Set<UUID> originalEntities) {
        var level = h.getLevel();
        var block = (FixtureBlock) original.getBlockState().getBlock();
        h.assertTrue(level.isEmptyBlock(source) && level.getBlockEntity(source) == null, "Transfer removes the original storage block");
        h.assertTrue(level.getBlockState(destination).is(block), "Transfer " + source + " -> " + destination + " retains " + block + "; actual=" + level.getBlockState(destination) + ", upper=" + level.getBlockState(destination.above()) + ", below=" + level.getBlockState(destination.below()));
        h.assertTrue(level.getBlockEntity(destination) instanceof FixtureBlockEntity, "Transfer creates destination storage");
        var moved = (FixtureBlockEntity) level.getBlockEntity(destination);
        h.assertTrue(moved.repairingLevel() == 3, "Repairing enchantment survives the transfer");
        var actual = moved.saveWithFullMetadata(level.registryAccess());
        for (String key : List.of("Materials", "RepairFractions", "RepairSoundProgress")) {
            h.assertTrue(expected.get(key).equals(actual.get(key)), "Transfer preserves " + key);
        }
        for (int slot = 0; slot < expectedItems.size(); slot++) {
            h.assertTrue(ItemStack.matches(expectedItems.get(slot), moved.displayedStack(slot)), "Transfer preserves slot " + slot + " and all item components");
        }
        if (block.kind().tall()) {
            h.assertTrue(level.isEmptyBlock(source.above()), "Transfer removes the original upper half");
            var upper = level.getBlockState(destination.above());
            h.assertTrue(upper.is(block) && upper.getValue(FixtureBlock.HALF) == DoubleBlockHalf.UPPER, "Transfer preserves the destination upper half");
        }
        h.assertTrue(newItemEntities(level, originalEntities).isEmpty(), "Transfer spawns no gear or fixture item entities");
    }

    private static void assertOrdinaryBreak(GameTestHelper h, BlockPos pos, FixtureBlockEntity original,
                                            List<ItemStack> expectedItems, Set<UUID> originalEntities) {
        var block = (FixtureBlock) original.getBlockState().getBlock();
        h.assertTrue(h.getLevel().destroyBlock(block.kind().tall() ? pos.above() : pos, true), "Ordinary breaking removes the fixture");
        h.assertTrue(h.getLevel().isEmptyBlock(pos), "Ordinary breaking removes storage");
        if (block.kind().tall()) h.assertTrue(h.getLevel().isEmptyBlock(pos.above()), "Ordinary breaking removes both mannequin halves");
        var drops = newItemEntities(h.getLevel(), originalEntities);
        for (var item : expectedItems) {
            int count = drops.stream().filter(entity -> ItemStack.isSameItemSameComponents(item, entity.getItem())).mapToInt(entity -> entity.getItem().getCount()).sum();
            h.assertTrue(count == item.getCount(), "Ordinary breaking drops each stored item exactly once");
        }
        h.assertTrue(drops.stream().filter(entity -> entity.getItem().is(block.asItem())).mapToInt(entity -> entity.getItem().getCount()).sum() == 1,
                "Ordinary breaking drops exactly one fixture");
        h.assertTrue(drops.stream().mapToInt(entity -> entity.getItem().getCount()).sum() == expectedItems.stream().mapToInt(ItemStack::getCount).sum() + 1,
                "Ordinary breaking produces no additional items");
    }

    private static Set<UUID> itemEntityIds(ServerLevel level) {
        var ids = new HashSet<UUID>();
        for (var entity : level.getAllEntities()) if (entity instanceof ItemEntity) ids.add(entity.getUUID());
        return ids;
    }

    private static List<ItemEntity> newItemEntities(ServerLevel level, Set<UUID> originalEntities) {
        var items = new ArrayList<ItemEntity>();
        for (var entity : level.getAllEntities()) {
            if (entity instanceof ItemEntity item && !item.isRemoved() && !originalEntities.contains(item.getUUID())) items.add(item);
        }
        return items;
    }

    private static void removeShip(GameTestHelper h, ServerSubLevel ship) {
        var container = SubLevelContainer.getContainer(h.getLevel());
        if (ship != null && container.getSubLevel(ship.getUniqueId()) == ship) container.removeSubLevel(ship, SubLevelRemovalReason.REMOVED);
    }
}