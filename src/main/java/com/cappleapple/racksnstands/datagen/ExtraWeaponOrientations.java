package com.cappleapple.racksnstands.datagen;

import com.google.gson.*;
import java.util.*;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import static com.cappleapple.racksnstands.datagen.FixtureData.*;

/** Authored-axis corrections and measured local centers; third-party assets remain in their own mods. */
public final class ExtraWeaponOrientations {
    public static void add(Map<String,JsonElement> files) {
        iron(files);
        custom(files,"coral_spear",0,-60,0,0.877419,-0.004487,-0.03125,-0.068371);
        custom(files,"coral_bardiche",0,90,0,0.566667,0.0,-0.1875,0.054687);
        custom(files,"black_steel_targe",0,90,0,1,-0.094375,0.125,-0.25);
        custom(files,"azure_sea_shield",0,90,0,1,-0.0475,0.125,-0.25);
        custom(files,"bulwark_of_the_flame",0,90,0,0.971429,-0.070313,0.09375,-0.125);
        custom(files,"gauntlet_of_guard",90,0,0,1,-0.09993,0.10625,-0.514653);
        custom(files,"gauntlet_of_bulwark",90,0,0,0.859353,-0.221169,0.050362,-0.435043);
        custom(files,"gauntlet_of_maelstrom",90,0,0,0.864709,-0.06961,0.10625,-0.369886);
        custom(files,"the_incinerator",0,90,180,0.469682,-0.0,-0.858462,-0.140245);
        custom(files,"cursed_bow",0,90,0,0.655867,0.062525,0.223138,-0.099183);
        custom(files,"wrath_of_the_desert",0,90,0,0.655867,0.062525,0.223138,0.013562);
        custom(files,"soul_render",0,90,0,0.395349,-0.0,-0.575,-0.028125);
        custom(files,"the_annihilator",0,90,0,0.829268,-0.0,-0.3875,-0.140625);
        custom(files,"astrape",0,90,0,0.34,-0.0,-0.25,-0.140625);
        custom(files,"ceraunus",0,90,0,0.625295,-0.0,-0.103766,-0.15625);
        custom(files,"brontes",0,90,0,0.688608,-0.0,-0.046875,-0.18125);
        custom(files,"the_immolator",0,90,0,0.737186,-0.0,-0.259467,-0.140625);
        custom(files,"meat_shredder",0,90,0,0.590278,0.0425,-0.485,-0.140625);
        custom(files,"laser_gatling",90,0,0,1,-0.025187,-0.0375,0.031044);
        custom(files,"wither_assault_shoulder_weapon",90,0,0,0.802277,-0.0,-0.185853,-0.359266);
        custom(files,"void_assault_shoulder_weapon",90,0,0,0.802277,-0.0,-0.185853,-0.359266);
        custom(files,"void_forge",0,90,0,0.656896,-0.0,-0.356464,-0.296875);
        custom(files,"tidal_claws",90,0,0,1,-0.102599,0.128531,-0.075876);
        custom(files,"infernal_forge",0,90,0,0.766197,-0.0,-0.171875,-0.140625);
        custom(files,"ancient_spear",0,90,180,0.425,-0.0,-0.425,-0.140625);
        group(files,"too_many_bows_horizontal_authored",array("too_many_bows:arcane_bow","too_many_bows:dark_bow","too_many_bows:frostbite","too_many_bows:arc_heavens","too_many_bows:dragons_breath","too_many_bows:solar_bow","too_many_bows:shulker_blast","too_many_bows:emerald_sage_bow","too_many_bows:demons_grasp","too_many_bows:aethers_call","too_many_bows:ironclad_bow","too_many_bows:hunter_bow","too_many_bows:sentinels_wrath","too_many_bows:burnt_relic","too_many_bows:cyroheart_bow","too_many_bows:vitality_weaver","too_many_bows:astral_bound","too_many_bows:spectral_whisper","too_many_bows:auroras_grace","too_many_bows:twin_shadows","too_many_bows:dusk_reaper","too_many_bows:verdant_vigor","too_many_bows:ethereal_hunter","too_many_bows:crimson_nexus","too_many_bows:radiance","too_many_bows:webstring","too_many_bows:torchbearer","too_many_bows:beacon_beam_bow"),0,0,90);
        group(files,"too_many_bows_side_authored",array("too_many_bows:ancient_sage_bow","too_many_bows:verdant_viper","too_many_bows:flame_bow","too_many_bows:tidal_bow","too_many_bows:necro_flame_bow","too_many_bows:scatter_bow","too_many_bows:wind_bow"),0,90,0);
        group(files,"too_many_bows_front_authored",array("too_many_bows:gravewire_bow","too_many_bows:vaultpiercer","too_many_bows:soulhoard"),0,0,0);
    }
    private static void iron(Map<String,JsonElement> files) {
        var staffs=rule(array("irons_spellbooks:graybeard_staff","irons_spellbooks:artificer_cane","irons_spellbooks:ice_staff","irons_spellbooks:blood_staff","irons_spellbooks:staff_of_the_nines"),0,0,180);
        staffs.addProperty("scale",.75);put(files,"irons_spellbooks_staffs",staffs,0,0,180);table(files,"irons_spellbooks_staffs",staffs,.12);
        var pyrium=rule(array("irons_spellbooks:pyrium_staff"),0,90,180);
        pyrium.addProperty("scale",.65);pyrium.add("model_translation",array(0,.0625,-.109375));put(files,"irons_spellbooks_pyrium_staff",pyrium,0,90,180);table(files,"irons_spellbooks_pyrium_staff",pyrium,.10);
        var books=rule(array("irons_spellbooks:netherite_spell_book","irons_spellbooks:diamond_spell_book","irons_spellbooks:gold_spell_book","irons_spellbooks:iron_spell_book","irons_spellbooks:copper_spell_book","irons_spellbooks:rotten_spell_book","irons_spellbooks:blaze_spell_book","irons_spellbooks:dragonskin_spell_book","irons_spellbooks:druidic_spell_book","irons_spellbooks:villager_spell_book","irons_spellbooks:ice_spell_book","irons_spellbooks:evoker_spell_book","irons_spellbooks:necronomicon_spell_book","irons_spellbooks:cursed_doll_spell_book"),0,90,0);
        books.addProperty("scale",1.5);files.put("data/racksnstands/racksnstands/item_transforms/irons_spellbooks_books.json",books);table(files,"irons_spellbooks_books",books,.102);
        var ring=rule(array("irons_spellbooks:affinity_ring"),0,0,180);
        // The dynamic affinity ring renderer delegates to its own selected inventory sprite.
        ring.add("rotation",array(0,0,0));ring.add("model_translation",array(0,-.1875,-.0625));ring.addProperty("scale",1.5);
        files.put("data/racksnstands/racksnstands/item_transforms/irons_spellbooks_affinity_ring.json",ring);
    }
    private static void table(Map<String,JsonElement> files,String name,JsonObject rule,double lift) {
        var tabletop=rule.deepCopy();tabletop.addProperty("priority",75);tabletop.add("fixtures",array("racksnstands:generic_tabletop_display"));tabletop.add("offset",array(0,lift,0));
        files.put("data/racksnstands/racksnstands/item_transforms/tabletop_"+name+".json",tabletop);
    }
    private static void custom(Map<String,JsonElement> files,String item,double x,double y,double z,double scale,double cx,double cy,double cz) {
        var rule=rule(array("cataclysm:"+item),x,y,z);rule.addProperty("scale",scale);rule.add("model_translation",array(cx,cy,cz));
        put(files,"cataclysm_"+item,rule,x,y,z);
    }
    private static void group(Map<String,JsonElement> files,String name,JsonArray items,double x,double y,double z) {
        var rule=rule(items,x,y,z);rule.addProperty("scale",.65);put(files,name,rule,x,y,z);
    }
    private static JsonObject rule(JsonArray items,double x,double y,double z) {
        return object("priority",55,"relative_rotation",true,"items",items,"rotation",array(x,y,z),"display_context","thirdperson_righthand");
    }
    private static void put(Map<String,JsonElement> files,String name,JsonObject rule,double x,double y,double z) {
        files.put("data/racksnstands/racksnstands/item_transforms/"+name+".json",rule);
        // Rotate in the fixture's front plane after correcting the authored model axes.
        var rotation=new Quaternionf().rotateZ((float)(Math.PI/2)).mul(new Quaternionf().rotationXYZ((float)Math.toRadians(x),(float)Math.toRadians(y),(float)Math.toRadians(z)));
        var angles=rotation.getEulerAnglesXYZ(new Vector3f());
        var horizontal=rule.deepCopy();horizontal.addProperty("priority",70);horizontal.addProperty("relative_rotation",false);
        horizontal.add("rotation",array(Math.toDegrees(angles.x),Math.toDegrees(angles.y),Math.toDegrees(angles.z)));
        horizontal.add("fixtures",array("racksnstands:sword_floor_stand"));
        files.put("data/racksnstands/racksnstands/item_transforms/sideways_"+name+".json",horizontal);
    }
}
