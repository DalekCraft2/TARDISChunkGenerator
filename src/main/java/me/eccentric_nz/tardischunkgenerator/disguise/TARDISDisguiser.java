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

import me.eccentric_nz.tardischunkgenerator.TARDISHelper;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.protocol.game.PacketPlayOutEntityDestroy;
import net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata;
import net.minecraft.network.protocol.game.PacketPlayOutNamedEntitySpawn;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntityLiving;
import net.minecraft.server.network.PlayerConnection;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

public class TARDISDisguiser {

    private static final boolean NAME_VISIBLE = false;
    private final TARDISHelper plugin;
    private final Player player;
    private Object[] options;
    private EntityType entityType;
    private Entity entity;

    public TARDISDisguiser(TARDISHelper plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public TARDISDisguiser(TARDISHelper plugin, EntityType entityType, Player player) {
        this.plugin = plugin;
        this.entityType = entityType;
        this.player = player;
        options = null;
        createDisguise();
    }

    public TARDISDisguiser(TARDISHelper plugin, EntityType entityType, Player player, Object[] options) {
        this.plugin = plugin;
        this.entityType = entityType;
        this.player = player;
        this.options = options;
        createDisguise();
    }

    public static void disguiseToPlayer(Player to, org.bukkit.World world) {
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            if (player.getWorld() == world) {
                if (TARDISDisguiseTracker.DISGUISED_AS_PLAYER.contains(player.getUniqueId())) {
                    TARDISPlayerDisguiser.disguiseToPlayer(player, to);
                }
                if (TARDISDisguiseTracker.DISGUISED_AS_MOB.containsKey(player.getUniqueId())) {
                    TARDISDisguise disguise = TARDISDisguiseTracker.DISGUISED_AS_MOB.get(player.getUniqueId());
                    Entity mob = TARDISDisguise.createMobDisguise(disguise, world);
                    if (mob != null) {
                        // set location
                        setEntityLocationIdAndName(mob, player.getLocation(), player, NAME_VISIBLE);
                        PacketPlayOutEntityDestroy packetPlayOutEntityDestroy = new PacketPlayOutEntityDestroy(player.getEntityId());
                        PacketPlayOutSpawnEntityLiving packetPlayOutSpawnEntityLiving = new PacketPlayOutSpawnEntityLiving((EntityLiving) mob);
                        PacketPlayOutEntityMetadata packetPlayOutEntityMetadata = new PacketPlayOutEntityMetadata(mob.getId(), mob.getDataWatcher(), false);
                        PlayerConnection connection = ((CraftPlayer) to).getHandle().b; // b = playerConnection
                        connection.sendPacket(packetPlayOutEntityDestroy);
                        connection.sendPacket(packetPlayOutSpawnEntityLiving);
                        connection.sendPacket(packetPlayOutEntityMetadata);
                    }
                }
            }
        }
    }

    public static void redisguise(Player player, org.bukkit.World world) {
        TARDISDisguise disguise = TARDISDisguiseTracker.DISGUISED_AS_MOB.get(player.getUniqueId());
        Entity mob = TARDISDisguise.createMobDisguise(disguise, world);
        if (mob != null) {
            // set location
            setEntityLocationIdAndName(mob, player.getLocation(), player, NAME_VISIBLE);
            TARDISDisguiseTracker.DISGUISED_AS_MOB.put(player.getUniqueId(), new TARDISDisguise(disguise.getEntityType(), disguise.getOptions()));
            PacketPlayOutEntityDestroy packetPlayOutEntityDestroy = new PacketPlayOutEntityDestroy(player.getEntityId());
            PacketPlayOutSpawnEntityLiving packetPlayOutSpawnEntityLiving = new PacketPlayOutSpawnEntityLiving((EntityLiving) mob);
            PacketPlayOutEntityMetadata packetPlayOutEntityMetadata = new PacketPlayOutEntityMetadata(mob.getId(), mob.getDataWatcher(), false);
            for (Player player1 : Bukkit.getOnlinePlayers()) {
                if (player1 != player && player.getWorld() == player1.getWorld()) {
                    PlayerConnection connection = ((CraftPlayer) player1).getHandle().b; // b = playerConnection
                    connection.sendPacket(packetPlayOutEntityDestroy);
                    connection.sendPacket(packetPlayOutSpawnEntityLiving);
                    connection.sendPacket(packetPlayOutEntityMetadata);
                }
            }
        }
    }

    private static void setEntityLocationIdAndName(Entity entity, Location location, Player player, boolean nameVisible) {
        entity.setPosition(location.getX(), location.getY(), location.getZ());
        entity.e(player.getEntityId());
        if (nameVisible) {
            entity.setCustomName(new ChatMessage(player.getDisplayName()));
            entity.setCustomNameVisible(true);
        }
        entity.setYRot(fixYaw(location.getYaw()));
        entity.setXRot(location.getPitch());
        EntityInsentient insentient = (EntityInsentient) entity;
        insentient.setNoAI(true);
    }

    private static float fixYaw(float yaw) {
        return yaw * 256.0F / 360.0F;
    }

    private void createDisguise() {
        if (entityType != null) {
            Location location = player.getLocation();
            TARDISDisguise disguise = new TARDISDisguise(entityType, options);
            entity = TARDISDisguise.createMobDisguise(disguise, location.getWorld());
            if (entity != null) {
                setEntityLocationIdAndName(entity, location, player, NAME_VISIBLE);
            }
        }
    }

    public void removeDisguise() {
        if (TARDISDisguiseTracker.DISGUISED_AS_PLAYER.contains(player.getUniqueId())) {
            new TARDISPlayerDisguiser(plugin, player, player.getUniqueId()).disguiseToAll();
            TARDISDisguiseTracker.DISGUISED_AS_PLAYER.remove(player.getUniqueId());
        } else {
            TARDISDisguiseTracker.DISGUISED_AS_MOB.remove(player.getUniqueId());
            PacketPlayOutEntityDestroy packetPlayOutEntityDestroy = new PacketPlayOutEntityDestroy(player.getEntityId());
            PacketPlayOutNamedEntitySpawn packetPlayOutNamedEntitySpawn = new PacketPlayOutNamedEntitySpawn(((CraftPlayer) player).getHandle());
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player != this.player && this.player.getWorld() == player.getWorld()) {
                    PlayerConnection connection = ((CraftPlayer) player).getHandle().b; // b = playerConnection
                    connection.sendPacket(packetPlayOutEntityDestroy);
                    connection.sendPacket(packetPlayOutNamedEntitySpawn);
                }
            }
        }
    }

    public void disguiseToAll() {
        TARDISDisguiseTracker.DISGUISED_AS_MOB.put(player.getUniqueId(), new TARDISDisguise(entityType, options));
        PacketPlayOutEntityDestroy packetPlayOutEntityDestroy = new PacketPlayOutEntityDestroy(player.getEntityId());
        PacketPlayOutSpawnEntityLiving packetPlayOutSpawnEntityLiving = new PacketPlayOutSpawnEntityLiving((EntityLiving) entity);
        PacketPlayOutEntityMetadata packetPlayOutEntityMetadata = new PacketPlayOutEntityMetadata(entity.getId(), entity.getDataWatcher(), false);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player != this.player && this.player.getWorld() == player.getWorld()) {
                PlayerConnection connection = ((CraftPlayer) player).getHandle().b; // b = playerConnection
                connection.sendPacket(packetPlayOutEntityDestroy);
                connection.sendPacket(packetPlayOutSpawnEntityLiving);
                connection.sendPacket(packetPlayOutEntityMetadata);
            }
        }
    }
}
