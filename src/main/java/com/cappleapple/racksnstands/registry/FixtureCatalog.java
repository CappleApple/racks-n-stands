package com.cappleapple.racksnstands.registry;
import java.util.*;
/** Appearance and capacity only; construction materials never control repair. */
public final class FixtureCatalog {
    public record Kind(String id, String name, int slots, String style, String category, String equipment, List<String> curios) {
        public boolean available() { return !LEGACY_IDS.contains(id); }
        public boolean armor() { return tall() || !equipment.isEmpty(); }
        public boolean curio() { return style.startsWith("curio"); }
        public boolean tall() { return id.equals("armor_mannequin"); }
        public boolean wall() { return style.equals("wall") || style.equals("rack") || style.equals("curio_cabinet"); }
    }
    private static Kind k(String id,String name,int n,String style,String category) { return new Kind(id,name,n,style,category,"",List.of()); }
    private static Kind armor(String id,String name,String equipment) { return new Kind(id,name,1,"armor","",equipment,List.of()); }
    private static Kind curio(String id,String name,int n,String style,String... slots) { return new Kind(id,name,n,"curio_"+style,"","",List.of(slots)); }
    // Preserve registry identities and capacities for already placed fixtures.
    private static final Set<String> LEGACY_IDS=Set.of("sword_wall_mount","bow_mount","shield_mount","staff_mount","ring_stand","necklace_bust","belt_display","bracelet_display","charm_pedestal","generic_curio_stand");
    public static final List<Kind> ALL=List.of(
        k("generic_pedestal","Recessed Pedestal",1,"pedestal",""), k("tool_rack","Large Item Rack",6,"rack","tools"),
        k("armor_mannequin","Full Armor Stand",4,"mannequin",""),
        armor("helmet_stand","Helmet Stand","head"),armor("chestplate_stand","Chestplate Stand","chest"),
        armor("leggings_stand","Leggings Stand","legs"),armor("boots_stand","Boots Stand","feet"),
        k("weapon_rack","Item Rack",4,"rack","weapons"),k("sword_wall_mount","Sword Wall Mount",1,"wall","swords"),
        k("sword_floor_stand","Display Stand",1,"floor","weapons"),k("bow_mount","Bow Mount",1,"wall","bows"),
        k("shield_mount","Shield Mount",1,"wall","shields"),k("staff_mount","Staff / Wand Mount",1,"floor","staffs"),
        k("polearm_rack","Small Item Rack",3,"rack","polearms"),k("generic_wall_display","Wall Display",1,"wall",""),
        k("generic_tabletop_display","Display",1,"table",""),
        curio("ring_stand","Ring Stand",2,"ring","ring"),curio("necklace_bust","Necklace / Amulet Bust",1,"bust","necklace","amulet","pendant"),
        curio("belt_display","Belt Display",1,"belt","belt"),curio("bracelet_display","Bracelet / Wrist Display",2,"bracelet","bracelet","wrist","hands"),
        curio("charm_pedestal","Charm / Relic Pedestal",1,"charm","charm","relic","artifact","trinket","curio"),
        curio("generic_curio_stand","Generic Curio Stand",1,"generic"),curio("curio_cabinet","Display Shelf",6,"cabinet")
    );
    public static Kind get(String id) { return ALL.stream().filter(k -> k.id().equals(id)).findFirst().orElseThrow(); }
    private FixtureCatalog() {}
}
