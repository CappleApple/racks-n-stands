package com.cappleapple.racksnstands.material;

import com.cappleapple.racksnstands.block.FixtureBlock;
import com.cappleapple.racksnstands.block.FixtureGeometry;
import com.cappleapple.racksnstands.blockentity.FixtureBlockEntity;
import com.cappleapple.racksnstands.registry.FixtureCatalog;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.phys.Vec3;

public final class MaterialCustomization {
    /** The server ray has already hit a solid. Later solids own coincident surfaces, as in datagen. */
    public static String groupAt(FixtureCatalog.Kind kind,Vec3 local) {
        var parts=FixtureGeometry.parts(kind);
        double x=local.x*16,y=local.y*16,z=local.z*16,epsilon=.002;
        for(int n=parts.size()-1;n>=0;n--) {
            var p=parts.get(n);
            if(x>=p.x()-epsilon&&x<=p.xx()+epsilon&&y>=p.y()-epsilon&&y<=p.yy()+epsilon&&z>=p.z()-epsilon&&z<=p.zz()+epsilon)
                return p.texture();
        }
        return null;
    }
    public static boolean apply(FixtureBlockEntity fixture,Player player,InteractionHand hand,Vec3 local) {
        var held=player.getItemInHand(hand);
        if(!(held.getItem() instanceof BlockItem item)) return false;
        // A sample changes appearance only. Neither the held block nor any displayed/equipped item moves.
        if(!player.isSpectator()&&player.mayBuild()) {
            var group=groupAt(((FixtureBlock)fixture.getBlockState().getBlock()).kind(),local);
            var sample=held.getOrDefault(DataComponents.BLOCK_STATE,BlockItemStateProperties.EMPTY).apply(item.getBlock().defaultBlockState());
            if(group!=null&&!sample.isAir()) fixture.setMaterial(group,sample);
        }
        return true;
    }
    private MaterialCustomization() {}
}
