package com.cappleapple.racksnstands.display.render;

import com.cappleapple.racksnstands.RacksNStands;
import com.cappleapple.racksnstands.block.FixtureBlock;
import com.cappleapple.racksnstands.blockentity.FixtureBlockEntity;
import com.cappleapple.racksnstands.material.MaterialPalette;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.*;
import org.jetbrains.annotations.Nullable;
import java.util.*;

/** Re-textures baked furniture in the chunk mesh; there is no per-frame furniture renderer. */
public final class MaterialModel extends BakedModelWrapper<BakedModel> {
    private static final ModelProperty<Mesh> MESH=new ModelProperty<>();
    private record UvOrientation(boolean swap,boolean flipU,boolean flipV) {
        static final UvOrientation IDENTITY=new UvOrientation(false,false,false);
        float u(float u,float v) { float a=swap?v:u;return flipU?1-a:a; }
        float v(float u,float v) { float a=swap?u:v;return flipV?1-a:a; }
    }
    private record Layer(TextureAtlasSprite sprite,int color,RenderType type,UvOrientation uv) {}
    private record Mesh(List<BakedQuad> all,Map<RenderType,List<BakedQuad>> layers) {
        List<BakedQuad> quads(RenderType layer) { return layer==null?all:layers.getOrDefault(layer,List.of()); }
    }
    private final Direction facing;
    private final Mesh fixed;
    private final Map<MaterialPalette,BakedModel> items=Collections.synchronizedMap(new LinkedHashMap<>(16,.75f,true) {
        @Override protected boolean removeEldestEntry(Map.Entry<MaterialPalette,BakedModel> entry) { return size()>128; }
    });
    private final ItemOverrides overrides=new ItemOverrides() {
        @Override public BakedModel resolve(BakedModel model,ItemStack stack,@Nullable ClientLevel level,@Nullable LivingEntity entity,int seed) {
            var palette=stack.getOrDefault(RacksNStands.MATERIALS.get(),MaterialPalette.EMPTY);
            if(palette.materials().isEmpty()) return MaterialModel.this;
            return items.computeIfAbsent(palette,key -> new MaterialModel(originalModel,facing,bake(key,null,null)));
        }
    };
    private MaterialModel(BakedModel model,Direction facing,Mesh fixed) { super(model);this.facing=facing;this.fixed=fixed; }
    public MaterialModel(BakedModel model,Direction facing) { this(model,facing,null); }

    @Override public ModelData getModelData(BlockAndTintGetter level,BlockPos pos,BlockState state,ModelData data) {
        var base=FixtureBlock.base(pos,state);
        var palette=level.getBlockEntity(base) instanceof FixtureBlockEntity fixture?fixture.materials():MaterialPalette.EMPTY;
        return data.derive().with(MESH,bake(palette,level,base)).build();
    }
    @Override public List<BakedQuad> getQuads(@Nullable BlockState state,@Nullable Direction side,RandomSource random,ModelData data,@Nullable RenderType layer) {
        // Generated furniture has no neighbour-cull faces: every exposed face belongs to its own shape.
        if(side!=null) return List.of();
        var mesh=fixed!=null?fixed:data.get(MESH);
        if(mesh!=null) return state==null?mesh.all:mesh.quads(layer);
        return originalModel.getQuads(state,null,random,ModelData.EMPTY,layer);
    }
    @Override public List<BakedQuad> getQuads(@Nullable BlockState state,@Nullable Direction side,RandomSource random) {
        return getQuads(state,side,random,ModelData.EMPTY,null);
    }
    @Override public ChunkRenderTypeSet getRenderTypes(BlockState state,RandomSource random,ModelData data) {
        var mesh=data.get(MESH);
        return mesh==null?ChunkRenderTypeSet.of(RenderType.solid()):ChunkRenderTypeSet.of(mesh.layers.keySet());
    }
    @Override public List<RenderType> getRenderTypes(ItemStack stack,boolean fabulous) {
        // One item pass retains alpha for glass as well as opaque/cutout materials.
        return List.of(Sheets.translucentItemSheet());
    }
    @Override public List<BakedModel> getRenderPasses(ItemStack stack,boolean fabulous) { return List.of(this); }
    @Override public ItemOverrides getOverrides() { return overrides; }
    @Override public BakedModel applyTransform(ItemDisplayContext context,PoseStack pose,boolean left) {
        originalModel.applyTransform(context,pose,left);return this;
    }
    @Override public TextureAtlasSprite getParticleIcon(ModelData data) {
        var mesh=fixed!=null?fixed:data.get(MESH);
        return mesh==null||mesh.all.isEmpty()?originalModel.getParticleIcon(data):mesh.all.getFirst().getSprite();
    }
    private Direction localFace(Direction direction) {
        if(direction.getAxis().isVertical()) return direction;
        int turns=switch(facing) {case EAST -> 1;case SOUTH -> 2;case WEST -> 3;default -> 0;};
        for(int i=0;i<turns;i++) direction=direction.getCounterClockWise();
        return direction;
    }
    private Mesh bake(MaterialPalette palette,@Nullable BlockAndTintGetter world,@Nullable BlockPos pos) {
        var all=new ArrayList<BakedQuad>();var layers=new LinkedHashMap<RenderType,List<BakedQuad>>();
        var samples=new HashMap<String,Map<Direction,List<Layer>>>();
        var random=RandomSource.create(42);
        for(var quad:originalModel.getQuads(null,null,random,ModelData.EMPTY,null)) {
            int group=quad.getTintIndex();
            String key=group>=0&&group<MaterialPalette.GROUPS.size()?MaterialPalette.GROUPS.get(group):"";
            var sample=palette.materials().get(key);
            List<Layer> replacements;
            if(sample==null) replacements=List.of(new Layer(quad.getSprite(),-1,RenderType.solid(),UvOrientation.IDENTITY));
            else replacements=samples.computeIfAbsent(key,k -> new EnumMap<>(Direction.class)).computeIfAbsent(localFace(quad.getDirection()),face -> sample(sample,face,world,pos));
            for(var layer:replacements) {
                var remapped=remap(quad,layer);all.add(remapped);
                layers.computeIfAbsent(layer.type,k -> new ArrayList<>()).add(remapped);
            }
        }
        layers.replaceAll((layer,quads) -> List.copyOf(quads));
        return new Mesh(List.copyOf(all),Map.copyOf(layers));
    }
    private static List<Layer> sample(BlockState state,Direction face,@Nullable BlockAndTintGetter world,@Nullable BlockPos pos) {
        var client=Minecraft.getInstance();var model=client.getBlockRenderer().getBlockModel(state);
        // Sampling a fixture uses its original block textures and cannot recursively wrap its palette.
        if(model instanceof MaterialModel material) model=material.originalModel;
        var result=new ArrayList<Layer>();var seen=new HashSet<Layer>();
        var random=RandomSource.create(42);
        for(var type:model.getRenderTypes(state,random,ModelData.EMPTY)) {
            random.setSeed(42);var quads=new ArrayList<>(model.getQuads(state,face,random,ModelData.EMPTY,type));
            random.setSeed(42);for(var quad:model.getQuads(state,null,random,ModelData.EMPTY,type)) if(quad.getDirection()==face) quads.add(quad);
            for(var quad:quads) {
                int color=quad.isTinted()?client.getBlockColors().getColor(state,world,pos,quad.getTintIndex()):-1;
                var layer=new Layer(quad.getSprite(),color,type,orientation(quad));
                if(seen.add(layer)) result.add(layer);
            }
        }
        // Entity-rendered or unusual models may have no face quads (chests, skulls, etc.).
        // Their baked particle sprite is the universal block-material fallback.
        if(result.isEmpty()) result.add(new Layer(model.getParticleIcon(ModelData.EMPTY),-1,RenderType.cutout(),UvOrientation.IDENTITY));
        return List.copyOf(result);
    }
    /** Recover face rotation/mirroring from source quads without importing their shape or texture scale. */
    private static UvOrientation orientation(BakedQuad quad) {
        int[] vertices=quad.getVertices();int stride=vertices.length/4;
        double[][] p=new double[4][4];
        for(int n=0;n<4;n++) {
            double x=Float.intBitsToFloat(vertices[n*stride]),y=Float.intBitsToFloat(vertices[n*stride+1]),z=Float.intBitsToFloat(vertices[n*stride+2]);
            double[] xy=switch(quad.getDirection()) {
                case UP -> new double[]{x,z};case DOWN -> new double[]{x,1-z};
                case NORTH -> new double[]{1-x,1-y};case SOUTH -> new double[]{x,1-y};
                case WEST -> new double[]{z,1-y};case EAST -> new double[]{1-z,1-y};
            };
            p[n]=new double[]{xy[0],xy[1],quad.getSprite().getUOffset(Float.intBitsToFloat(vertices[n*stride+4])),quad.getSprite().getVOffset(Float.intBitsToFloat(vertices[n*stride+5]))};
        }
        double ax=p[1][0]-p[0][0],ay=p[1][1]-p[0][1],bx=p[2][0]-p[0][0],by=p[2][1]-p[0][1],det=ax*by-bx*ay;
        if(Math.abs(det)<1e-8) return UvOrientation.IDENTITY;
        double au=p[1][2]-p[0][2],bu=p[2][2]-p[0][2],av=p[1][3]-p[0][3],bv=p[2][3]-p[0][3];
        double ux=(au*by-bu*ay)/det,uy=(ax*bu-bx*au)/det,vx=(av*by-bv*ay)/det,vy=(ax*bv-bx*av)/det;
        boolean swap=Math.abs(uy)>Math.abs(ux);
        if(swap== (Math.abs(vy)>Math.abs(vx))) return UvOrientation.IDENTITY;
        return new UvOrientation(swap,(swap?uy:ux)<0,(swap?vx:vy)<0);
    }
    private static BakedQuad remap(BakedQuad quad,Layer layer) {
        int[] data=quad.getVertices().clone();int stride=data.length/4;
        var old=quad.getSprite();
        for(int vertex=0;vertex<4;vertex++) {
            int at=vertex*stride;
            float u=old.getUOffset(Float.intBitsToFloat(data[at+4]));
            float v=old.getVOffset(Float.intBitsToFloat(data[at+5]));
            data[at+4]=Float.floatToRawIntBits(layer.sprite.getU(layer.uv.u(u,v)));
            data[at+5]=Float.floatToRawIntBits(layer.sprite.getV(layer.uv.v(u,v)));
            if(layer.color!=-1) {
                int original=data[at+3],r=(layer.color>>16)&255,g=(layer.color>>8)&255,b=layer.color&255;
                data[at+3]=(original&0xff000000)|((original&255)*r/255)|((((original>>>8)&255)*g/255)<<8)|((((original>>>16)&255)*b/255)<<16);
            }
        }
        // The generated tint index is metadata, not a request to tint the entire fixture.
        return new BakedQuad(data,-1,quad.getDirection(),layer.sprite,quad.isShade(),quad.hasAmbientOcclusion());
    }
    @EventBusSubscriber(modid=RacksNStands.MOD_ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
    public static final class Registration {
        @SubscribeEvent public static void models(ModelEvent.ModifyBakingResult event) {
            for(var block:RacksNStands.FIXTURES.values()) {
                for(var state:block.get().getStateDefinition().getPossibleStates()) {
                    var id=net.minecraft.client.renderer.block.BlockModelShaper.stateToModelLocation(state);
                    event.getModels().computeIfPresent(id,(key,model) -> new MaterialModel(model,state.getValue(FixtureBlock.FACING)));
                }
                var id=new ModelResourceLocation(block.getId(),"inventory");
                event.getModels().computeIfPresent(id,(key,model) -> new MaterialModel(model,Direction.NORTH));
            }
        }
    }
}
