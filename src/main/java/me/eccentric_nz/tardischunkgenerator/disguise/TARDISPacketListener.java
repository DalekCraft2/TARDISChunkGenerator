/*
 * Copyright (C) 2020 eccentric_nz
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (location your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package me.eccentric_nz.tardischunkgenerator.disguise;

import io.netty.channel.*;
import me.eccentric_nz.tardischunkgenerator.TARDISHelper;
import net.minecraft.network.protocol.game.PacketPlayOutNamedEntitySpawn;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.UUID;
import java.util.logging.Level;

public class TARDISPacketListener {

    public static void removePlayer(Player player) {
        Channel channel = ((CraftPlayer) player).getHandle().b.a.k; // b = playerConnection, a = networkManager, k = channel
        channel.eventLoop().submit(() -> {
            channel.pipeline().remove(player.getName());
            return null;
        });
    }

    public static void injectPlayer(Player player) {
        ChannelDuplexHandler channelDuplexHandler = new ChannelDuplexHandler() {

            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                super.channelRead(ctx, msg);
            }

            @Override
            public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                if (msg instanceof PacketPlayOutNamedEntitySpawn namedEntitySpawn) {
                    try {
                        Field field = namedEntitySpawn.getClass().getDeclaredField("b"); // NoSuchFieldException, b = UUID
                        field.setAccessible(true);
                        UUID uuid = (UUID) field.get(namedEntitySpawn);
                        if (TARDISDisguiseTracker.DISGUISED_AS_MOB.containsKey(uuid)) {
                            Entity entity = Bukkit.getEntity(uuid);
                            if (entity.getType().equals(EntityType.PLAYER)) {
                                Player player = (Player) entity;
                                Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(TARDISHelper.plugin, () -> TARDISDisguiser.redisguise(player, entity.getWorld()), 5L);
                            }
                            field.setAccessible(false);
                        }
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        TARDISHelper.plugin.getLogger().log(Level.SEVERE, "Could not get UUID from PacketPlayOutNamedEntitySpawn: " + e.getMessage());
                    }
                }
                super.write(ctx, msg, promise);
            }
        };

        ChannelPipeline pipeline = ((CraftPlayer) player).getHandle().b.a.k.pipeline(); // b = playerConnection, a = networkManager, k = channel
        pipeline.addBefore("packet_handler", player.getName(), channelDuplexHandler);
    }
}
