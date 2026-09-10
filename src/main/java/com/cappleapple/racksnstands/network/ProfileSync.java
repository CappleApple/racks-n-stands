package com.cappleapple.racksnstands.network;
import com.cappleapple.racksnstands.RacksNStands;
import com.cappleapple.racksnstands.api.*;
import com.cappleapple.racksnstands.display.Profiles;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import java.util.*;

public final class ProfileSync {
    /** One definition per packet keeps payloads bounded; clear precedes the new immutable snapshot. */
    public record Payload(boolean clear,int kind,ResourceLocation id,String json) implements CustomPacketPayload {
        public static final Type<Payload> TYPE=new Type<>(RacksNStands.id("display_profile"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Payload> CODEC=StreamCodec.of((b,p) -> {
            b.writeBoolean(p.clear);b.writeByte(p.kind);b.writeResourceLocation(p.id);b.writeUtf(p.json,262144);
        },b -> new Payload(b.readBoolean(),b.readByte(),b.readResourceLocation(),b.readUtf(262144)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("2").playToClient(Payload.TYPE,Payload.CODEC,(payload,context) -> context.enqueueWork(() -> {
            if(payload.clear) { Profiles.CLIENT=new Profiles.Snapshot(Map.of(),Map.of());return; }
            try {
                var displays=new HashMap<>(Profiles.CLIENT.displays());var categories=new HashMap<>(Profiles.CLIENT.categories());var rules=new HashMap<>(Profiles.CLIENT.itemRules());
                if(payload.kind==2) rules.put(payload.id,ItemDisplayRule.CODEC.parse(context.player().registryAccess().createSerializationContext(JsonOps.INSTANCE),JsonParser.parseString(payload.json)).getOrThrow());
                else if(payload.kind==1) categories.put(payload.id,DisplayFilter.CODEC.parse(context.player().registryAccess().createSerializationContext(JsonOps.INSTANCE),JsonParser.parseString(payload.json)).getOrThrow());
                else displays.put(payload.id,DisplayProfile.CODEC.parse(context.player().registryAccess().createSerializationContext(JsonOps.INSTANCE),JsonParser.parseString(payload.json)).getOrThrow());
                Profiles.CLIENT=new Profiles.Snapshot(Map.copyOf(displays),Map.copyOf(categories),rules);
            } catch(RuntimeException e) { RacksNStands.LOGGER.warn("Invalid synced display {}: {}",payload.id,e.getMessage()); }
        }));
    }
    public static void sync(OnDatapackSyncEvent event) {
        if(event.getPlayer()==null) com.cappleapple.racksnstands.config.LiveConfig.refresh(event.getPlayerList().getServer());
        var ops=event.getPlayerList().getServer().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        List<Payload> packets=new ArrayList<>();packets.add(new Payload(true,0,RacksNStands.id("reset"),""));
        Profiles.SERVER.displays().forEach((id,p) -> packets.add(new Payload(false,0,id,DisplayProfile.CODEC.encodeStart(ops,p).getOrThrow().toString())));
        Profiles.SERVER.categories().forEach((id,f) -> packets.add(new Payload(false,1,id,DisplayFilter.CODEC.encodeStart(ops,f).getOrThrow().toString())));
        Profiles.SERVER.itemRules().forEach((id,r) -> packets.add(new Payload(false,2,id,ItemDisplayRule.CODEC.encodeStart(ops,r).getOrThrow().toString())));
        var players=event.getPlayer()!=null?List.of(event.getPlayer()):event.getPlayerList().getPlayers();
        for(var player:players) for(var packet:packets) PacketDistributor.sendToPlayer(player,packet);
    }
}
