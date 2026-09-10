package com.cappleapple.racksnstands.gametest;

import com.cappleapple.racksnstands.RacksNStands;
import com.cappleapple.ritualsnotrolls.Config;
import com.cappleapple.ritualsnotrolls.data.Definitions;
import com.cappleapple.ritualsnotrolls.knowledge.Knowledge;
import com.cappleapple.ritualsnotrolls.knowledge.KnowledgeData;
import com.cappleapple.ritualsnotrolls.ritual.RitualMath;
import com.cappleapple.ritualsnotrolls.ritual.RitualNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.items.ItemStackHandler;
import java.util.*;

/** Compiled only for -PritualsModsDir development runs, never included in the released JAR. */
@GameTestHolder(RacksNStands.MOD_ID) @PrefixGameTestTemplate(false)
public final class PassiveRepairingGameTests {
    @GameTest(templateNamespace="minecraft",template="bastion/mobs/empty",timeoutTicks=100)
    public static void sevenKnowledgeMaterialsReachFiveWithoutCatalysts(GameTestHelper h) {
        var id=RacksNStands.id("repairing");var definition=Definitions.SERVER.get(id);
        h.assertTrue(definition!=null&&definition.materials().size()==7,"The built-in definition loads seven affinities through Rituals Not Rolls");
        var book=Knowledge.book(new KnowledgeData(id,List.of(),true));
        List<RitualNetwork.Pedestal> pedestals=new ArrayList<>();int n=0;
        for(var affinity:definition.materials()) {
            h.assertTrue(Knowledge.add(book,Knowledge.page(id,affinity.id()),false),"Every affinity has valid knowledge-page data");
            var item=BuiltInRegistries.ITEM.get(affinity.item().orElseThrow());h.assertTrue(item!=Items.AIR,"Material exists in the installed registry");
            var stack=new ItemStack(item);var handler=new ItemStackHandler(3);handler.setStackInSlot(0,stack);
            pedestals.add(new RitualNetwork.Pedestal(new BlockPos(++n*2,1,0),handler,stack,false,false));
        }
        var known=Set.copyOf(Knowledge.data(book).entries());h.assertTrue(known.size()==7,"Book retains all seven knowledge pages");
        var source=new BlockPos(-2,1,0);
        var network=new RitualNetwork.Snapshot(Map.of(id,known),List.of(source),pedestals,List.of(),List.of(new RitualNetwork.KnowledgeSource(source,id,known)),BlockPos.ZERO);
        var player=TestPlayers.create(h);boolean chained=Config.CHAIN_ANIMATIONS.get();
        try {
            List<ItemStack> targets=new ArrayList<>();targets.add(new ItemStack(Items.BOOK));
            for(var block:RacksNStands.FIXTURES.values()) targets.add(new ItemStack(block.get()));
            for(boolean chain:new boolean[]{false,true}) {
                Config.CHAIN_ANIMATIONS.set(chain);
                for(var target:targets) {
                    var plan=RitualMath.automatic(player,target,network);
                    h.assertTrue(plan.evaluation().ready()&&plan.selected().getOrDefault(id,0)==5,"Seven passive materials reach exactly Repairing V: "+target+" chained="+chain);
                    h.assertTrue(plan.experienceCatalysts()==0&&plan.consumed().isEmpty()&&plan.withdrawals().isEmpty()&&plan.evaluation().xpMultiplier()==1,"No experience or material consumption is needed");
                    if(!chain) h.assertTrue(Math.abs(plan.evaluation().lines().getFirst().power()-472)<1e-6,"Baseline passive power is 472");
                }
            }
            Config.CHAIN_ANIMATIONS.set(false);
            for(int missing=0;missing<7;missing++) {
                var fewer=new ArrayList<>(pedestals);fewer.remove(missing);
                var partial=new RitualNetwork.Snapshot(network.knowledge(),network.shelves(),fewer,List.of(),network.sources(),BlockPos.ZERO);
                h.assertTrue(RitualMath.automatic(player,targets.get(1),partial).selected().getOrDefault(id,0)<5,"All seven distinct materials are needed at baseline power");
            }
        } finally { Config.CHAIN_ANIMATIONS.set(chained);player.discard(); }
        h.succeed();
    }
}
