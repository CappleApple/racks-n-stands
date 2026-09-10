package com.cappleapple.racksnstands.display;
import com.cappleapple.racksnstands.RacksNStands;
import com.cappleapple.racksnstands.api.*;
import com.cappleapple.racksnstands.registry.FixtureCatalog;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import java.util.*;
public final class Profiles extends SimpleJsonResourceReloadListener {
    public record Snapshot(Map<ResourceLocation,DisplayProfile> displays,Map<ResourceLocation,DisplayFilter> categories,Map<ResourceLocation,ItemDisplayRule> itemRules,List<ItemDisplayRule> orderedRules) {
        public Snapshot(Map<ResourceLocation,DisplayProfile> displays,Map<ResourceLocation,DisplayFilter> categories) { this(displays,categories,Map.of()); }
        public Snapshot(Map<ResourceLocation,DisplayProfile> displays,Map<ResourceLocation,DisplayFilter> categories,Map<ResourceLocation,ItemDisplayRule> rules) {
            this(displays,categories,Map.copyOf(rules),rules.entrySet().stream().sorted(Comparator.<Map.Entry<ResourceLocation,ItemDisplayRule>>comparingInt(e -> e.getValue().priority()).reversed().thenComparing(e -> e.getKey().toString())).map(Map.Entry::getValue).toList());
        }
    }
    private static final ResourceLocation FALLBACK_ID=RacksNStands.id("generic_pedestal");
    private static final Map<ResourceLocation,DisplayProfile> DEFAULTS=new HashMap<>();
    static { FixtureCatalog.ALL.forEach(k -> DEFAULTS.put(RacksNStands.id(k.id()),BuiltinProfiles.create(k))); }
    public static volatile Snapshot SERVER=new Snapshot(Map.copyOf(DEFAULTS),Map.of());
    public static volatile Snapshot CLIENT=new Snapshot(Map.copyOf(DEFAULTS),Map.of());
    public static void registerDefault(ResourceLocation id,DisplayProfile profile) { if(DEFAULTS.putIfAbsent(id,profile)!=null) throw new IllegalArgumentException("Duplicate display "+id); }
    private final net.minecraft.core.HolderLookup.Provider registries;
    public Profiles(net.minecraft.core.HolderLookup.Provider registries) { super(new Gson(),"racksnstands");this.registries=registries; }
    public static DisplayProfile get(ResourceLocation id,boolean client) { return (client?CLIENT:SERVER).displays().getOrDefault(id,DEFAULTS.getOrDefault(id,DEFAULTS.get(FALLBACK_ID))); }
    public static Map<ResourceLocation,DisplayFilter> categories(boolean client) { return (client?CLIENT:SERVER).categories(); }
    @Override protected void apply(Map<ResourceLocation,JsonElement> input,ResourceManager manager,ProfilerFiller profiler) {
        SERVER=decode(input,registries.createSerializationContext(JsonOps.INSTANCE));
        RacksNStands.LOGGER.info("Loaded {} display profiles and {} categories",SERVER.displays().size(),SERVER.categories().size());
        RacksNStands.LOGGER.info("Loaded {} item display rules",SERVER.itemRules().size());
    }
    public static Snapshot decode(Map<ResourceLocation,JsonElement> input) { return decode(input,JsonOps.INSTANCE); }
    public static Snapshot decode(Map<ResourceLocation,JsonElement> input,com.mojang.serialization.DynamicOps<JsonElement> ops) {
        var displays=new HashMap<>(DEFAULTS);var categories=new HashMap<ResourceLocation,DisplayFilter>();var rules=new HashMap<ResourceLocation,ItemDisplayRule>();
        input.forEach((resource,json) -> {
            try {
                String path=resource.getPath();
                if(path.startsWith("item_transforms/")) {
                    var id=ResourceLocation.fromNamespaceAndPath(resource.getNamespace(),path.substring(16));
                    rules.put(id,ItemDisplayRule.CODEC.parse(ops,json).getOrThrow());
                } else if(path.startsWith("displays/")) {
                    var id=ResourceLocation.fromNamespaceAndPath(resource.getNamespace(),path.substring(9));
                    var profile=DisplayProfile.CODEC.parse(ops,json).getOrThrow();
                    if(DEFAULTS.containsKey(id)&&profile.slots().size()!=DEFAULTS.get(id).slots().size()) throw new IllegalArgumentException("Cannot change registered fixture capacity; existing gear must remain accessible");
                    displays.put(id,profile);
                } else if(path.startsWith("categories/")) {
                    var id=ResourceLocation.fromNamespaceAndPath(resource.getNamespace(),path.substring(11));
                    var filter=DisplayFilter.CODEC.parse(ops,json).getOrThrow();
                    if(!filter.categories().isEmpty()) throw new IllegalArgumentException("Category definitions cannot recursively reference categories");
                    categories.put(id,filter);
                }
            } catch(RuntimeException e) { RacksNStands.LOGGER.warn("Ignoring display data {}: {}",resource,e.getMessage()); }
        });
        return new Snapshot(Map.copyOf(displays),Map.copyOf(categories),rules);
    }
}
