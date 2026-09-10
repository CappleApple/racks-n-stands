package com.cappleapple.racksnstands;
import com.cappleapple.racksnstands.block.*;
import com.cappleapple.racksnstands.blockentity.FixtureBlockEntity;
import com.cappleapple.racksnstands.config.FixtureConfig;
import com.cappleapple.racksnstands.registry.FixtureCatalog;
import com.cappleapple.racksnstands.display.Profiles;
import com.cappleapple.racksnstands.network.ProfileSync;
import com.cappleapple.racksnstands.datagen.FixtureData;
import com.cappleapple.racksnstands.debug.DebugCommands;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.*;
import net.neoforged.neoforge.capabilities.*;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import java.util.*;
@Mod(RacksNStands.MOD_ID)
public final class RacksNStands {
    public static final String MOD_ID="racksnstands";
    public static final Logger LOGGER=LogUtils.getLogger();
    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(MOD_ID,path); }
    public static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(MOD_ID);
    public static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,MOD_ID);
    public static final DeferredRegister<net.minecraft.core.component.DataComponentType<?>> COMPONENTS=DeferredRegister.create(Registries.DATA_COMPONENT_TYPE,MOD_ID);
    public static final java.util.function.Supplier<net.minecraft.core.component.DataComponentType<com.cappleapple.racksnstands.material.MaterialPalette>> MATERIALS=COMPONENTS.register("materials",() -> net.minecraft.core.component.DataComponentType.<com.cappleapple.racksnstands.material.MaterialPalette>builder().persistent(com.cappleapple.racksnstands.material.MaterialPalette.CODEC).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.fromCodec(com.cappleapple.racksnstands.material.MaterialPalette.CODEC)).build());
    public static final DeferredRegister<net.minecraft.world.item.crafting.RecipeSerializer<?>> RECIPES=DeferredRegister.create(Registries.RECIPE_SERIALIZER,MOD_ID);
    public static final java.util.function.Supplier<net.minecraft.world.item.crafting.RecipeSerializer<com.cappleapple.racksnstands.crafting.AppearanceRecipe>> RESET_APPEARANCE=RECIPES.register("reset_appearance",() -> new net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<>(category -> new com.cappleapple.racksnstands.crafting.AppearanceRecipe(category,false)));
    public static final java.util.function.Supplier<net.minecraft.world.item.crafting.RecipeSerializer<com.cappleapple.racksnstands.crafting.AppearanceRecipe>> COPY_APPEARANCE=RECIPES.register("copy_appearance",() -> new net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<>(category -> new com.cappleapple.racksnstands.crafting.AppearanceRecipe(category,true)));
    public static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(Registries.CREATIVE_MODE_TAB,MOD_ID);
    public static final Map<String,DeferredBlock<FixtureBlock>> FIXTURES=new LinkedHashMap<>();
    static {
        for(var kind:FixtureCatalog.ALL) {
            var block=BLOCKS.<FixtureBlock>register(kind.id(),() -> {
                var properties=BlockBehaviour.Properties.of().strength(2.5f).sound(kind.style().equals("pedestal")?SoundType.STONE:SoundType.WOOD).noOcclusion();
                return kind.id().equals("generic_tabletop_display")?new SurfaceDisplayBlock(kind,properties):new FixtureBlock(kind,properties);
            });
            FIXTURES.put(kind.id(),block);ITEMS.register(kind.id(),() -> new FixtureItem(block.get(),new Item.Properties().stacksTo(64)));
        }
    }
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<FixtureBlockEntity>> FIXTURE_ENTITY=ENTITIES.register("fixture",() -> BlockEntityType.Builder.of(FixtureBlockEntity::new,FIXTURES.values().stream().map(DeferredBlock::get).toArray(Block[]::new)).build(null));
    static {
        TABS.register("fixtures",() -> CreativeModeTab.builder().title(Component.translatable("itemGroup.racksnstands"))
            .icon(() -> new ItemStack(FIXTURES.get("generic_pedestal").get()))
            .displayItems((parameters,output) -> FIXTURES.forEach((name,block) -> {
                if(FixtureCatalog.get(name).available()) output.accept(block.get());
            })).build());
    }
    public RacksNStands(IEventBus bus,ModContainer container) {
        COMPONENTS.register(bus);RECIPES.register(bus);bus.addListener(com.cappleapple.racksnstands.config.LiveConfig::reloaded);BLOCKS.register(bus);ITEMS.register(bus);ENTITIES.register(bus);TABS.register(bus);
        com.cappleapple.racksnstands.config.RepairConfigMigration.migrate(net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().resolve("racksnstands-common.toml"));
        container.registerConfig(ModConfig.Type.COMMON,FixtureConfig.COMMON);container.registerConfig(ModConfig.Type.CLIENT,FixtureConfig.CLIENT);
        bus.addListener(this::capabilities);bus.addListener(ProfileSync::register);bus.addListener(FixtureData::gather);
        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent e) -> e.addListener(new Profiles(e.getRegistryAccess())));
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) -> {
            var state=event.getLevel().getBlockState(event.getPos());
            if(event.getEntity().isShiftKeyDown() && state.getBlock() instanceof FixtureBlock block ) event.setUseBlock(net.neoforged.neoforge.common.util.TriState.TRUE);
        });
        NeoForge.EVENT_BUS.addListener(ProfileSync::sync);NeoForge.EVENT_BUS.addListener(DebugCommands::register);
    }
    private void capabilities(RegisterCapabilitiesEvent e) { e.registerBlockEntity(Capabilities.ItemHandler.BLOCK,FIXTURE_ENTITY.get(),(fixture,side) -> fixture.automation()); }
}
