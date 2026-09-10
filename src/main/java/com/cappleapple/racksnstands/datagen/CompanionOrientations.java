package com.cappleapple.racksnstands.datagen;
import com.google.gson.JsonElement;
import java.util.Map;
import static com.cappleapple.racksnstands.datagen.FixtureData.*;

/** Optional item IDs and published family tags; no companion runtime checks or copied assets. */
public final class CompanionOrientations {
    public static void add(Map<String,JsonElement> files) {
        files.put("data/racksnstands/racksnstands/item_transforms/simplyswords_axes.json",object(
            "priority",35,"relative_rotation",true,"rotation",array(0,0,-45),"items",array(
            "simplyswords:diamond_greataxe",
            "simplyswords:gobber_compat/gobber/gobber_greataxe",
            "simplyswords:gobber_compat/gobber_end/gobber_end_greataxe",
            "simplyswords:gobber_compat/gobber_nether/gobber_nether_greataxe",
            "simplyswords:gold_greataxe",
            "simplyswords:iron_greataxe",
            "simplyswords:livyatan",
            "simplyswords:mythicmetals_compat/adamantite/adamantite_greataxe",
            "simplyswords:mythicmetals_compat/aquarium/aquarium_greataxe",
            "simplyswords:mythicmetals_compat/banglum/banglum_greataxe",
            "simplyswords:mythicmetals_compat/bronze/bronze_greataxe",
            "simplyswords:mythicmetals_compat/carmot/carmot_greataxe",
            "simplyswords:mythicmetals_compat/celestium/celestium_greataxe",
            "simplyswords:mythicmetals_compat/kyber/kyber_greataxe",
            "simplyswords:mythicmetals_compat/metallurgium/metallurgium_greataxe",
            "simplyswords:mythicmetals_compat/mythril/mythril_greataxe",
            "simplyswords:mythicmetals_compat/orichalcum/orichalcum_greataxe",
            "simplyswords:mythicmetals_compat/osmium/osmium_greataxe",
            "simplyswords:mythicmetals_compat/palladium/palladium_greataxe",
            "simplyswords:mythicmetals_compat/prometheum/prometheum_greataxe",
            "simplyswords:mythicmetals_compat/quadrillum/quadrillum_greataxe",
            "simplyswords:mythicmetals_compat/runite/runite_greataxe",
            "simplyswords:mythicmetals_compat/star_platinum/star_platinum_greataxe",
            "simplyswords:mythicmetals_compat/steel/steel_greataxe",
            "simplyswords:mythicmetals_compat/stormyx/stormyx_greataxe",
            "simplyswords:netherite_greataxe",
            "simplyswords:runic_greataxe",
            "simplyswords:soulpyre")));
        files.put("data/racksnstands/racksnstands/item_transforms/simplyswords_3d_hammers.json",object(
            "priority",40,"relative_rotation",true,"rotation",array(0,0,45),"items",array(
            "simplyswords:diamond_greathammer",
            "simplyswords:gobber_compat/gobber/gobber_greathammer",
            "simplyswords:gobber_compat/gobber_end/gobber_end_greathammer",
            "simplyswords:gobber_compat/gobber_nether/gobber_nether_greathammer",
            "simplyswords:gold_greathammer",
            "simplyswords:hearthflame",
            "simplyswords:hiveheart",
            "simplyswords:iron_greathammer",
            "simplyswords:mjolnir",
            "simplyswords:mythicmetals_compat/adamantite/adamantite_greathammer",
            "simplyswords:mythicmetals_compat/aquarium/aquarium_greathammer",
            "simplyswords:mythicmetals_compat/banglum/banglum_greathammer",
            "simplyswords:mythicmetals_compat/bronze/bronze_greathammer",
            "simplyswords:mythicmetals_compat/carmot/carmot_greathammer",
            "simplyswords:mythicmetals_compat/celestium/celestium_greathammer",
            "simplyswords:mythicmetals_compat/durasteel/durasteel_greathammer",
            "simplyswords:mythicmetals_compat/kyber/kyber_greathammer",
            "simplyswords:mythicmetals_compat/metallurgium/metallurgium_greathammer",
            "simplyswords:mythicmetals_compat/mythril/mythril_greathammer",
            "simplyswords:mythicmetals_compat/orichalcum/orichalcum_greathammer",
            "simplyswords:mythicmetals_compat/osmium/osmium_greathammer",
            "simplyswords:mythicmetals_compat/palladium/palladium_greathammer",
            "simplyswords:mythicmetals_compat/prometheum/prometheum_greathammer",
            "simplyswords:mythicmetals_compat/quadrillum/quadrillum_greathammer",
            "simplyswords:mythicmetals_compat/runite/runite_greathammer",
            "simplyswords:mythicmetals_compat/star_platinum/star_platinum_greathammer",
            "simplyswords:mythicmetals_compat/steel/steel_greathammer",
            "simplyswords:mythicmetals_compat/stormyx/stormyx_greathammer",
            "simplyswords:netherite_greathammer",
            "simplyswords:runic_greathammer",
            "simplyswords:soulkeeper")));
        var horizontalHammer=files.get("data/racksnstands/racksnstands/item_transforms/simplyswords_3d_hammers.json").getAsJsonObject().deepCopy();
        horizontalHammer.addProperty("priority",65);horizontalHammer.addProperty("relative_rotation",false);
        horizontalHammer.add("rotation",array(0,0,135));horizontalHammer.add("fixtures",array("racksnstands:sword_floor_stand"));
        files.put("data/racksnstands/racksnstands/item_transforms/sideways_stand_3d_hammers.json",horizontalHammer);
        files.put("data/racksnstands/racksnstands/item_transforms/simplymore_weapons.json",object(
            "priority",12,"relative_rotation",true,"rotation",array(0,0,135),"item_tags",array("simplymore:weapon_types/all")));
        files.put("data/racksnstands/racksnstands/item_transforms/simplymore_axes_hammers.json",object(
            "priority",35,"relative_rotation",true,"rotation",array(0,0,-45),"item_tags",array("simplymore:weapon_types/greataxes","simplymore:weapon_types/pernachs")));
    }
}
