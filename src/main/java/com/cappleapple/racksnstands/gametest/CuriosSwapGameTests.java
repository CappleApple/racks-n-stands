package com.cappleapple.racksnstands.gametest;

import com.cappleapple.racksnstands.RacksNStands;
import com.cappleapple.racksnstands.blockentity.FixtureBlockEntity;
import com.cappleapple.racksnstands.compat.curios.CuriosBridge;
import com.cappleapple.racksnstands.config.FixtureConfig;
import com.cappleapple.racksnstands.interaction.FixtureInteraction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(RacksNStands.MOD_ID) @PrefixGameTestTemplate(false)
public final class CuriosSwapGameTests {
    @GameTest(templateNamespace="minecraft",template="bastion/mobs/empty")
    public static void emptyHandCuriosEquipSwapAndRestrictions(GameTestHelper h) {
        if(!CuriosBridge.available()) { h.succeed();return; }
        Installed.run(h);
    }
    private static final class Installed {
        static void run(GameTestHelper h) {
            var pos=new BlockPos(1,2,1);h.setBlock(pos,RacksNStands.FIXTURES.get("curio_cabinet").get());
            var f=(FixtureBlockEntity)h.getBlockEntity(pos);var p=h.makeMockPlayer(GameType.SURVIVAL);
            var handler=top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(p).orElseThrow().getCurios().get("ring").getStacks();
            f.insert(0,new ItemStack(Items.FEATHER),false);
            h.assertTrue(FixtureInteraction.swapEquipped(f,0,p)&&handler.getStackInSlot(0).is(Items.FEATHER)&&f.displayedStack(0).isEmpty()&&p.getMainHandItem().isEmpty(),"Empty-hand action equips into the real functional ring slot");
            for(int i=0;i<handler.getSlots();i++) handler.setStackInSlot(i,new ItemStack(Items.FEATHER));
            var sword=new ItemStack(Items.IRON_SWORD);sword.setDamageValue(100);f.insert(0,sword,false);
            h.assertTrue(FixtureInteraction.swapEquipped(f,0,p)&&ItemStack.matches(handler.getStackInSlot(0),sword)&&f.displayedStack(0).is(Items.FEATHER),"Occupied Curios slot returns its old item to the display");
            var locked=sword.copy();locked.enchant(h.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.BINDING_CURSE),1);for(int i=0;i<handler.getSlots();i++) handler.setStackInSlot(i,locked.copy());
            h.assertTrue(!FixtureInteraction.swapEquipped(f,0,p)&&ItemStack.matches(handler.getStackInSlot(0),locked)&&f.displayedStack(0).is(Items.FEATHER),"Binding curse blocks the entire exchange");
            for(int i=0;i<handler.getSlots();i++) handler.setStackInSlot(i,ItemStack.EMPTY);
            java.util.function.Consumer<top.theillusivec4.curios.api.event.CurioCanEquipEvent> deny=e -> { if(e.getSlotContext().entity()==p)e.setEquipResult(net.neoforged.neoforge.common.util.TriState.FALSE); };
            NeoForge.EVENT_BUS.addListener(deny);
            try { h.assertTrue(!FixtureInteraction.swapEquipped(f,0,p)&&f.displayedStack(0).is(Items.FEATHER)&&handler.getStackInSlot(0).isEmpty(),"Curios equip denial leaves both inventories unchanged"); }
            finally { NeoForge.EVENT_BUS.unregister(deny); }
            boolean full=FixtureConfig.FULL_STACKS.get();
            try {
                f.extract(0,64,false);FixtureConfig.FULL_STACKS.set(true);f.insert(0,new ItemStack(Items.FEATHER,16),false);
                h.assertTrue(FixtureInteraction.swapEquipped(f,0,p)&&f.displayedStack(0).getCount()==15&&handler.getStackInSlot(0).getCount()==1,"Equipping a stacked curio transfers one and leaves the remainder on display");
            } finally { FixtureConfig.FULL_STACKS.set(full); }
            h.succeed();
        }
    }
}
