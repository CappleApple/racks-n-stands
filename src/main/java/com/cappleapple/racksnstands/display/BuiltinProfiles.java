package com.cappleapple.racksnstands.display;
import com.cappleapple.racksnstands.RacksNStands;
import com.cappleapple.racksnstands.api.*;
import com.cappleapple.racksnstands.registry.FixtureCatalog;
import java.util.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.AABB;
public final class BuiltinProfiles {
    public static DisplayProfile create(FixtureCatalog.Kind kind) {
        var slots=new ArrayList<DisplaySlotDefinition>();
        for(int n=0;n<kind.slots();n++) {
            Optional<EquipmentSlot> equipment=kind.tall()?Optional.of(List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET).get(n))
                :kind.equipment().isEmpty()?Optional.empty():Optional.of(EquipmentSlot.byName(kind.equipment()));
            double x=.5,y=.64,z=.5,s=.65,rx=0,ry=0,rz=0;
            AABB bounds=new AABB(0,0,0,1,1,1);
            boolean worn=equipment.isPresent();
            if(worn) ry=180;
            if(kind.wall()) { z=.70-n*.002;y=.52;s=1.0; }
            if(kind.slots()>1 && !kind.tall()) {
                int cols=kind.slots()==6?3:kind.slots(),row=n/cols,col=n%cols;
                x=(col+.5)/cols;y=kind.slots()==6?.80-row*.45:.54;s=kind.slots()==6?.40:(kind.slots()==4?.31:.56);
                if(kind.wall()&&!kind.curio()) s=1;
                if(kind.curio()) { z=kind.style().equals("curio_cabinet")?.7375:.30;s=.27; }
                if(kind.style().equals("curio_cabinet")) { x=(3+5*col)/16.0;y=(row==0?8.5:2)/16.0+.001; }
                bounds=new AABB((double)col/cols,kind.slots()==6?(row==0?.50:0):0,0,(col+1.0)/cols,kind.slots()==6?(row==0?1:.50):1,1);
            }
            if(kind.style().equals("pedestal")) { y=1.05;s=1.10; }
            if(kind.style().equals("curio_generic")||kind.style().equals("curio_charm")) y=.91;
            if(kind.style().equals("curio_bust")||kind.style().equals("curio_belt")) { z=.27;s=.45;y=.64; }
            if(kind.style().equals("table")) { y=.23;rx=90;s=.65; }
            if(kind.style().equals("floor")) { s=1.0;y=.73;z=.50; }
            if(kind.tall()) {
                y=1.72;s=1;z=.5;
                double[] lows={1.7,.9,.4,0},highs={2.25,1.7,.9,.4};
                bounds=new AABB(0,lows[n],0,1,highs[n],1);
            } else if(worn) {
                s=equipment.orElseThrow()==EquipmentSlot.HEAD?.85:equipment.orElseThrow()==EquipmentSlot.CHEST?.90:1.0;
                y=switch(equipment.orElseThrow()) { case HEAD -> .60;case CHEST -> .95;case LEGS -> 1.58;default -> 1.73; };
            }
            // Built-in furniture accepts any item; only armor bindings constrain insertion.
            var filter=new DisplayFilter(List.of(),List.of(),List.of(),List.of(),List.of(),List.of(),false,Optional.empty());
            slots.add(new DisplaySlotDefinition(n,filter,equipment,new DisplayTransform(List.of(x,y,z),List.of(rx,ry,rz),List.of(s,s,s),ItemDisplayContext.FIXED),bounds,worn));
        }
        return new DisplayProfile(slots,RacksNStands.id(kind.tall()?"armor_loadout":"normal"));
    }
}
