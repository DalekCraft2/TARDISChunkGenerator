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
import net.minecraft.network.protocol.game.PacketPlayOutEntity;
import net.minecraft.network.protocol.game.PacketPlayOutEntityDestroy;
import net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntityLiving;
import net.minecraft.server.network.PlayerConnection;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public class TARDISArmourStandDisguiser {

    private final TARDISHelper plugin;
    private final ArmorStand armorStand;
    private final Object[] options;
    private final EntityType entityType;
    private Entity entity;

    public TARDISArmourStandDisguiser(TARDISHelper plugin, ArmorStand armorStand, EntityType entityType, Object[] options) {
        this.plugin = plugin;
        this.armorStand = armorStand;
        this.entityType = entityType;
        this.options = options;
        createDisguise();
    }

    public static void disguiseToPlayer(Player to, org.bukkit.World world) {
        for (Map.Entry<UUID, TARDISDisguise> map : TARDISDisguiseTracker.DISGUISED_ARMOR_STANDS.entrySet()) {
            ArmorStand armorStand = (ArmorStand) Bukkit.getEntity(map.getKey());
            if (armorStand != null && armorStand.getWorld() == world) {
                Entity mob = TARDISDisguise.createMobDisguise(map.getValue(), world);
                if (mob != null) {
                    // set location
                    setEntityLocationIdAndName(mob, armorStand.getLocation(), armorStand);
                    PacketPlayOutEntityDestroy packetPlayOutEntityDestroy = new PacketPlayOutEntityDestroy(armorStand.getEntityId());
                    PacketPlayOutSpawnEntityLiving packetPlayOutSpawnEntityLiving = new PacketPlayOutSpawnEntityLiving((EntityLiving) mob);
                    PacketPlayOutEntityMetadata packetPlayOutEntityMetadata = new PacketPlayOutEntityMetadata(mob.getId(), mob.getDataWatcher(), false);
                    PacketPlayOutEntity.PacketPlayOutEntityLook packetPlayOutEntityLook = new PacketPlayOutEntity.PacketPlayOutEntityLook(mob.getId(), (byte) mob.getYRot(), (byte) mob.getXRot(), true);
                    PlayerConnection connection = ((CraftPlayer) to).getHandle().b; // b = playerConnection
                    connection.sendPacket(packetPlayOutEntityDestroy);
                    connection.sendPacket(packetPlayOutSpawnEntityLiving);
                    connection.sendPacket(packetPlayOutEntityMetadata);
                    connection.sendPacket(packetPlayOutEntityLook);
                }
            }
        }
    }

    public static void redisguise(ArmorStand armorStand, org.bukkit.World world) {
        TARDISDisguise disguise = TARDISDisguiseTracker.DISGUISED_AS_MOB.get(armorStand.getUniqueId());
        Entity mob = TARDISDisguise.createMobDisguise(disguise, world);
        if (mob != null) {
            // set location
            setEntityLocationIdAndName(mob, armorStand.getLocation(), armorStand);
            TARDISDisguiseTracker.DISGUISED_AS_MOB.put(armorStand.getUniqueId(), new TARDISDisguise(disguise.getEntityType(), disguise.getOptions()));
            PacketPlayOutEntityDestroy packetPlayOutEntityDestroy = new PacketPlayOutEntityDestroy(armorStand.getEntityId());
            PacketPlayOutSpawnEntityLiving packetPlayOutSpawnEntityLiving = new PacketPlayOutSpawnEntityLiving((EntityLiving) mob);
            PacketPlayOutEntityMetadata packetPlayOutEntityMetadata = new PacketPlayOutEntityMetadata(mob.getId(), mob.getDataWatcher(), false);
            PacketPlayOutEntity.PacketPlayOutEntityLook packetPlayOutEntityLook = new PacketPlayOutEntity.PacketPlayOutEntityLook(mob.getId(), (byte) mob.getYRot(), (byte) mob.getXRot(), true);
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (armorStand.getWorld() == player.getWorld()) {
                    PlayerConnection connection = ((CraftPlayer) player).getHandle().b; // b = playerConnection
                    connection.sendPacket(packetPlayOutEntityDestroy);
                    connection.sendPacket(packetPlayOutSpawnEntityLiving);
                    connection.sendPacket(packetPlayOutEntityMetadata);
                    connection.sendPacket(packetPlayOutEntityLook);
                }
            }
        }
    }

    private static void setEntityLocationIdAndName(Entity entity, Location location, ArmorStand armorStand) {
        entity.setPosition(location.getX(), location.getY(), location.getZ());
        entity.e(armorStand.getEntityId());
        float fixed = fixYaw(location.getYaw());
        entity.setHeadRotation(fixed);
        entity.h(fixed);
        entity.setYRot(fixed);
        entity.setXRot(location.getPitch());
        EntityInsentient insentient = (EntityInsentient) entity;
        insentient.setNoAI(true);
    }

    private static float fixYaw(float yaw) {
        return yaw * 256.0F / 360.0F;
    }

    public static void removeDisguise(ArmorStand stand) {
        TARDISDisguiseTracker.DISGUISED_ARMOR_STANDS.remove(stand.getUniqueId());
        PacketPlayOutEntityDestroy packetPlayOutEntityDestroy = new PacketPlayOutEntityDestroy(stand.getEntityId());
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (stand.getWorld() == player.getWorld()) {
                ((CraftPlayer) player).getHandle().b.sendPacket(packetPlayOutEntityDestroy); // b = playerConnection
            }
        }
    }

    private void createDisguise() {
        if (entityType != null) {
            Location location = armorStand.getLocation();
            TARDISDisguise disguise = new TARDISDisguise(entityType, options);
            entity = TARDISDisguise.createMobDisguise(disguise, location.getWorld());
            if (entity != null) {
                setEntityLocationIdAndName(entity, location, armorStand);
            }
        }
    }

    public void disguiseToAll() {
        TARDISDisguiseTracker.DISGUISED_AS_MOB.put(armorStand.getUniqueId(), new TARDISDisguise(entityType, options));
        PacketPlayOutEntityDestroy packetPlayOutEntityDestroy = new PacketPlayOutEntityDestroy(armorStand.getEntityId());
        PacketPlayOutSpawnEntityLiving packetPlayOutSpawnEntityLiving = new PacketPlayOutSpawnEntityLiving((EntityLiving) entity);
        PacketPlayOutEntityMetadata packetPlayOutEntityMetadata = new PacketPlayOutEntityMetadata(entity.getId(), entity.getDataWatcher(), false);
        PacketPlayOutEntity.PacketPlayOutEntityLook packetPlayOutEntityLook = new PacketPlayOutEntity.PacketPlayOutEntityLook(entity.getId(), (byte) entity.getYRot(), (byte) entity.getXRot(), true);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (armorStand.getWorld() == player.getWorld()) {
                PlayerConnection connection = ((CraftPlayer) player).getHandle().b; // b = playerConnection
                connection.sendPacket(packetPlayOutEntityDestroy);
                connection.sendPacket(packetPlayOutSpawnEntityLiving);
                connection.sendPacket(packetPlayOutEntityMetadata);
                connection.sendPacket(packetPlayOutEntityLook);
            }
        }
    }
}
