package com.cappleapple.racksnstands.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import java.util.UUID;

/** Real server-side menus without pretending an embedded channel completed the mod handshake. */
public final class TestPlayers {
    public static ServerPlayer create(GameTestHelper h) {
        var cookie=CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(),"FixtureCraftTest"),false);
        var server=h.getLevel().getServer();
        var player=new ServerPlayer(server,h.getLevel(),cookie.gameProfile(),cookie.clientInformation());
        player.connection=new ServerGamePacketListenerImpl(server,new Connection(PacketFlow.SERVERBOUND),player,cookie) {
            @Override public void send(Packet<?> packet) { }
        };
        var pos=h.absolutePos(net.minecraft.core.BlockPos.ZERO);player.setPos(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5);
        return player;
    }
}
