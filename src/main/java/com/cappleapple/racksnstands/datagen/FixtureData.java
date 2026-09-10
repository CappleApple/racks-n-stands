package com.cappleapple.racksnstands.datagen;

import com.cappleapple.racksnstands.RacksNStands;
import com.cappleapple.racksnstands.api.DisplayProfile;
import com.cappleapple.racksnstands.display.BuiltinProfiles;
import com.cappleapple.racksnstands.registry.FixtureCatalog;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import net.minecraft.data.*;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Deterministic datagen for the complete shipped asset and datapack set. */
public final class FixtureData implements DataProvider {
    private final Path output;
    public FixtureData(PackOutput output) { this.output=output.getOutputFolder(); }
    public static void gather(GatherDataEvent event) { event.getGenerator().addProvider(true,new FixtureData(event.getGenerator().getPackOutput())); }
    @Override public String getName() { return "Racks N' Stands fixtures, displays, enchantment and companion data"; }
    public static JsonObject object(Object... pairs) {
        JsonObject result=new JsonObject();
        for(int i=0;i<pairs.length;i+=2) result.add(pairs[i].toString(),new Gson().toJsonTree(pairs[i+1]));
        return result;
    }
    public static JsonArray array(Object... entries) { return new Gson().toJsonTree(entries).getAsJsonArray(); }
    private static JsonObject tag(Object... entries) { return object("replace",false,"values",array(entries)); }
    private static JsonObject optionalTag(String id) { return object("id","#"+id,"required",false); }
    @Override public CompletableFuture<?> run(CachedOutput cache) {
        var files=resources();
        return CompletableFuture.allOf(files.entrySet().stream().map(e -> DataProvider.saveStable(cache,e.getValue(),output.resolve(e.getKey()))).toArray(CompletableFuture[]::new));
    }
    public static Map<String,JsonElement> resources() {
        var files=new TreeMap<String,JsonElement>();
        JsonObject lang=object("itemGroup.racksnstands","Racks N' Stands","enchantment.racksnstands.repairing","Repairing");
        lang.addProperty("enchantment.racksnstands.repairing.desc","Gradually repairs items stored on this display. Higher levels repair faster.");
        lang.addProperty("tooltip.racksnstands.repair.full","Fully repaired");
        lang.addProperty("tooltip.racksnstands.repair.inactive","Not repairing");
        lang.addProperty("tooltip.racksnstands.repair.remaining","Fully repaired in %s");
        lang.addProperty("tooltip.racksnstands.repair.detail","%s / %s");
        lang.addProperty("tooltip.racksnstands.slot","Slot %s of %s");
        lang.addProperty("tooltip.racksnstands.slot.head","Head");lang.addProperty("tooltip.racksnstands.slot.chest","Chest");
        lang.addProperty("tooltip.racksnstands.slot.legs","Legs");lang.addProperty("tooltip.racksnstands.slot.feet","Feet");
        lang.addProperty("config.jade.plugin_racksnstands.repair_progress","Display repair progress");
        List<String> hosts=new ArrayList<>();int recipeIndex=0;
        String[] centers={"smooth_stone","iron_pickaxe","armor_stand","carved_pumpkin","leather_chestplate","leather_leggings","leather_boots","iron_sword","flint","iron_ingot","string","shield","blaze_rod","trident","item_frame","oak_slab","gold_nugget","gold_ingot","leather","copper_ingot","amethyst_shard","quartz","chest"};
        for(var kind:FixtureCatalog.ALL) {
            String id=kind.id(),full="racksnstands:"+id;hosts.add(full);
            lang.addProperty("block.racksnstands."+id,kind.name());
            files.put("data/racksnstands/racksnstands/displays/"+id+".json",DisplayProfile.CODEC.encodeStart(JsonOps.INSTANCE,BuiltinProfiles.create(kind)).getOrThrow());
            var variants=new JsonObject();String[] directions={"north","east","south","west"};
            boolean surface=id.equals("generic_tabletop_display");
            for(int i=0;i<4;i++) {
                // Model front is local -Z. Blockstate rotations turn it clockwise.
                if(surface) for(String face:List.of("floor","wall","ceiling")) {
                    String suffix=face.equals("floor")?"":"_"+face;
                    variants.add("face="+face+",facing="+directions[i]+",half=lower",object("model","racksnstands:block/"+id+suffix,"y",i*90));
                    variants.add("face="+face+",facing="+directions[i]+",half=upper",object("model","racksnstands:block/empty"));
                } else {
                    variants.add("facing="+directions[i]+",half=lower",object("model","racksnstands:block/"+id,"y",i*90));
                    variants.add("facing="+directions[i]+",half=upper",object("model",kind.tall()?"racksnstands:block/"+id+"_upper":"racksnstands:block/empty","y",i*90));
                }
            }
            files.put("assets/racksnstands/blockstates/"+id+".json",object("variants",variants));
            var furniture=model(kind);
            files.put("assets/racksnstands/models/block/"+id+".json",kind.tall()?modelHalf(kind,false):furniture);
            if(kind.tall()) files.put("assets/racksnstands/models/block/"+id+"_upper.json",modelHalf(kind,true));
            if(surface) for(var face:List.of(net.minecraft.world.level.block.state.properties.AttachFace.WALL,net.minecraft.world.level.block.state.properties.AttachFace.CEILING))
                files.put("assets/racksnstands/models/block/"+id+"_"+face.getSerializedName()+".json",model(kind,com.cappleapple.racksnstands.block.SurfaceDisplayBlock.parts(kind,face)));
            files.put("assets/racksnstands/models/item/"+id+".json",itemModel(furniture));
            var entry=object("type","minecraft:item","name",full,"functions",array(object("function","minecraft:copy_components","source","block_entity","include",array("minecraft:enchantments","racksnstands:materials"))));
            files.put("data/racksnstands/loot_table/blocks/"+id+".json",object("type","minecraft:block","pools",array(object("rolls",1,"entries",array(entry),"conditions",array(
                object("condition","minecraft:survives_explosion"),object("condition","minecraft:block_state_property","block",full,"properties",object("half","lower")))))));
            String center=centers[recipeIndex++];
            if(!kind.available()) continue;
            if(!kind.armor()&&kind.style().equals("rack")) center="iron_ingot";
            var pattern=switch(id) {
                case "tool_rack" -> array("WCW","S S","WWW");
                case "weapon_rack" -> array("W W","WCW"," S ");
                case "polearm_rack" -> array("SCS","S S","WWW");
                case "sword_floor_stand" -> array(" C ","S S","WWW");
                default -> array("W W"," C ","WSW");
            };
            var recipe=object("type","minecraft:crafting_shaped","category","decorations","pattern",pattern,
                "key",object("W",object("item","minecraft:oak_planks"),"S",object("item","minecraft:stick"),"C",object("item","minecraft:"+center)),"result",object("id",full,"count",1));
            if(kind.style().equals("pedestal")) recipe.add("key",object("W",object("item","minecraft:stone_bricks"),"S",object("item","minecraft:smooth_stone"),"C",object("item","minecraft:iron_ingot")));
            files.put("data/racksnstands/recipe/"+id+".json",recipe);
            files.put("data/racksnstands/advancement/recipes/"+id+".json",object("parent","minecraft:recipes/root","criteria",object("has_planks",object("trigger","minecraft:inventory_changed","conditions",object("items",array(object("items","minecraft:oak_planks"))))),"requirements",array(array("has_planks")),"rewards",object("recipes",array(full))));
        }
        files.put("assets/racksnstands/models/block/empty.json",object("textures",object("particle","minecraft:block/oak_planks"),"elements",array()));
        files.put("assets/racksnstands/lang/en_us.json",lang);
        files.put("data/racksnstands/tags/item/repairing_hosts.json",tag(hosts.toArray()));
        files.put("data/minecraft/tags/block/mineable/axe.json",tag(hosts.stream().filter(id -> !id.equals("racksnstands:generic_pedestal")).toArray()));
        files.put("data/minecraft/tags/block/mineable/pickaxe.json",tag("racksnstands:generic_pedestal"));
        files.put("data/racksnstands/tags/item/display_denied.json",tag());
        Map<String,Object[]> tags=new LinkedHashMap<>();
        tags.put("tools",new Object[]{optionalTag("minecraft:pickaxes"),optionalTag("minecraft:axes"),optionalTag("minecraft:shovels"),optionalTag("minecraft:hoes"),optionalTag("c:tools"),"minecraft:shears","minecraft:brush","minecraft:fishing_rod","minecraft:flint_and_steel"});
        tags.put("upright_tools",new Object[]{optionalTag("minecraft:pickaxes"),optionalTag("minecraft:axes"),optionalTag("minecraft:shovels"),optionalTag("minecraft:hoes"),optionalTag("c:tools/pickaxe"),optionalTag("c:tools/axe"),optionalTag("c:tools/shovel"),optionalTag("c:tools/hoe")});
        tags.put("swords",new Object[]{optionalTag("minecraft:swords"),optionalTag("c:tools/sword"),optionalTag("simplyswords:swords"),optionalTag("simplymore:weapon_types/all")});
        tags.put("weapons",new Object[]{"#racksnstands:swords","#racksnstands:polearms","#racksnstands:staffs",optionalTag("c:melee_weapon_tools"),optionalTag("c:tools/melee_weapon"),optionalTag("minecraft:axes"),"minecraft:mace"});
        tags.put("bows",new Object[]{"minecraft:bow","minecraft:crossbow",optionalTag("c:tools/bow"),optionalTag("c:tools/crossbow")});
        tags.put("shields",new Object[]{"minecraft:shield",optionalTag("c:tools/shield")});
        tags.put("staffs",new Object[]{"minecraft:blaze_rod",optionalTag("c:tools/wand"),optionalTag("c:tools/staff"),optionalTag("c:staffs"),optionalTag("c:wands")});
        tags.put("polearms",new Object[]{"minecraft:trident",optionalTag("c:tools/spear"),optionalTag("c:tools/polearm")});
        tags.put("helmets",new Object[]{optionalTag("minecraft:head_armor")});tags.put("chestplates",new Object[]{optionalTag("minecraft:chest_armor"),"minecraft:elytra"});
        tags.put("leggings",new Object[]{optionalTag("minecraft:leg_armor")});tags.put("boots",new Object[]{optionalTag("minecraft:foot_armor")});
        tags.forEach((name,values) -> files.put("data/racksnstands/tags/item/"+name+".json",tag(values)));
        files.put("data/racksnstands/enchantment/repairing.json",object("description",object("translate","enchantment.racksnstands.repairing"),"supported_items","#racksnstands:repairing_hosts",
            "primary_items","#racksnstands:repairing_hosts","weight",1,"max_level",10,"min_cost",object("base",10,"per_level_above_first",5),"max_cost",object("base",60,"per_level_above_first",5),"anvil_cost",2,"slots",array("any"),"effects",object()));
        for(String name:List.of("treasure","on_random_loot","tradeable")) files.put("data/minecraft/tags/enchantment/"+name+".json",tag("racksnstands:repairing"));
        files.put("data/racksnstands/racksnstands/item_transforms/upright_tools.json",object("relative_rotation",true,"priority",15,"item_tags",array("racksnstands:upright_tools"),"rotation",array(0,0,-45)));
        files.put("data/racksnstands/racksnstands/item_transforms/downward_swords.json",object("relative_rotation",true,"priority",10,"item_tags",array("racksnstands:swords"),"rotation",array(0,0,135)));
        files.put("data/racksnstands/racksnstands/item_transforms/downward_weapons.json",object("relative_rotation",true,"priority",10,"item_tags",array("racksnstands:weapons"),"rotation",array(0,0,135)));
        files.put("data/racksnstands/racksnstands/item_transforms/upright_tridents.json",object("relative_rotation",true,"priority",30,"items",array("minecraft:trident"),"rotation",array(0,0,0)));
        files.put("data/racksnstands/racksnstands/item_transforms/bows.json",object("relative_rotation",true,"priority",30,"item_tags",array("racksnstands:bows"),"rotation",array(0,0,0)));
        files.put("data/racksnstands/tags/item/upright_axes_hammers.json",tag(optionalTag("minecraft:axes"),optionalTag("c:tools/axe"),
            optionalTag("c:tools/hammer"),optionalTag("simplyswords:implicit/greataxe"),optionalTag("simplyswords:implicit/greathammer"),optionalTag("simplyswords:implicit/hammer"),
            optionalTag("simplymore:weapon_types/greataxes"),optionalTag("simplymore:weapon_types/greathammers"),optionalTag("simplymore:weapon_types/pernachs"),"minecraft:mace"));
        files.put("data/racksnstands/racksnstands/item_transforms/upright_axes_hammers.json",object("priority",25,"relative_rotation",true,"item_tags",array("racksnstands:upright_axes_hammers"),"rotation",array(0,0,-45)));
        files.put("data/racksnstands/racksnstands/item_transforms/outward_shield.json",object("priority",50,"relative_rotation",true,
            "items",array("minecraft:shield"),"rotation",array(0,0,0),"display_context","fixed"));
        files.put("data/racksnstands/racksnstands/item_transforms/sideways_display_stand.json",object("priority",60,
            "fixtures",array("racksnstands:sword_floor_stand"),"item_tags",array("racksnstands:tools","racksnstands:weapons","racksnstands:staffs","racksnstands:polearms","racksnstands:bows"),"rotation",array(0,0,45)));
        files.put("data/racksnstands/racksnstands/item_transforms/sideways_stand_trident.json",object("priority",65,
            "fixtures",array("racksnstands:sword_floor_stand"),"items",array("minecraft:trident"),"rotation",array(0,0,90)));
        CompanionOrientations.add(files);
        ExtraWeaponOrientations.add(files);
        var levels=new JsonObject();for(int n=1;n<=10;n++) levels.addProperty(Integer.toString(n),128*n);
        files.put("data/racksnstands/ritual_enchanting/enchantments/repairing.json",object("enchantment","racksnstands:repairing","optional",true,
            "materials",array(object("id","iron_ingot","item","minecraft:iron_ingot","power",16,"resource_value",2),
                object("id","amethyst_shard","item","minecraft:amethyst_shard","power",32,"resource_value",4),
                object("id","experience_bottle","item","minecraft:experience_bottle","power",64,"resource_value",8),
                object("id","echo_shard","item","minecraft:echo_shard","power",128,"resource_value",16)),
            "levels",levels,"particle","minecraft:enchant","particle_color","#8DCBC2"));
        return files;
    }
    private static JsonObject cube(double x,double y,double z,double xx,double yy,double zz,String texture) {
        var faces=new JsonObject();
        for(String face:List.of("north","south","east","west","up","down")) {
            double bottom=y-Math.floor(y/16)*16,top=bottom+yy-y;
            JsonArray uv=switch(face) {
                case "down" -> array(x,16-zz,xx,16-z);
                case "up" -> array(x,z,xx,zz);
                case "north" -> array(16-xx,16-top,16-x,16-bottom);
                case "south" -> array(x,16-top,xx,16-bottom);
                case "west" -> array(z,16-top,zz,16-bottom);
                default -> array(16-zz,16-top,16-z,16-bottom);
            };
            faces.add(face,object("texture","#"+texture,"uv",uv,"tintindex",com.cappleapple.racksnstands.material.MaterialPalette.GROUPS.indexOf(texture)));
        }
        return object("from",array(x,y,z),"to",array(xx,yy,zz),"faces",faces);
    }
    private static JsonObject model(FixtureCatalog.Kind kind) { return model(kind,com.cappleapple.racksnstands.block.FixtureGeometry.parts(kind)); }
    private static JsonObject modelHalf(FixtureCatalog.Kind kind,boolean upper) {
        var parts=new ArrayList<com.cappleapple.racksnstands.block.FixtureGeometry.Part>();double low=upper?16:0,high=upper?Double.POSITIVE_INFINITY:16;
        for(var p:com.cappleapple.racksnstands.block.FixtureGeometry.parts(kind)) {
            double y=Math.max(low,p.y()),yy=Math.min(high,p.yy());if(yy<=y) continue;
            parts.add(new com.cappleapple.racksnstands.block.FixtureGeometry.Part(p.x(),y-low,p.z(),p.xx(),yy-low,p.zz(),p.texture()));
        }
        return model(kind,parts);
    }
    private static JsonObject model(FixtureCatalog.Kind kind,List<com.cappleapple.racksnstands.block.FixtureGeometry.Part> parts) {
        String style=kind.style();var cubes=new JsonArray();
        for(var part:parts) {
            // Tile taller pieces at block boundaries instead of stretching one texture over the post.
            double y=part.y();
            while(y<part.yy()) {
                double top=Math.min(part.yy(),(Math.floor(y/16)+1)*16);
                cubes.add(cube(part.x(),y,part.z(),part.xx(),top,part.zz(),part.texture()));y=top;
            }
        }
        return object("parent","minecraft:block/block","textures",object("particle",style.equals("pedestal")?"minecraft:block/stone_bricks":"minecraft:block/oak_planks","wood","minecraft:block/stripped_oak_log","dark","minecraft:block/dark_oak_planks","metal","minecraft:block/iron_block","cloth","minecraft:block/brown_wool","stone","minecraft:block/stone_bricks","trim","minecraft:block/smooth_stone","base","minecraft:block/polished_andesite"),"elements",ExposedFaces.build(cubes));
    }
    private static JsonObject itemModel(JsonObject furniture) {
        var item=furniture.deepCopy();var elements=item.getAsJsonArray("elements");
        double[] low={Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY};
        double[] high={Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY};
        for(var e:elements) for(int axis=0;axis<3;axis++) {
            low[axis]=Math.min(low[axis],e.getAsJsonObject().getAsJsonArray("from").get(axis).getAsDouble());
            high[axis]=Math.max(high[axis],e.getAsJsonObject().getAsJsonArray("to").get(axis).getAsDouble());
        }
        double longest=Math.max(high[0]-low[0],Math.max(high[1]-low[1],high[2]-low[2]));
        if(longest>16) for(var e:elements) for(String end:List.of("from","to")) for(int axis=0;axis<3;axis++) {
            var vector=e.getAsJsonObject().getAsJsonArray(end);
            vector.set(axis,new JsonPrimitive((vector.get(axis).getAsDouble()-(low[axis]+high[axis])*.5)*16/longest+8));
        }
        return item;
    }

}
