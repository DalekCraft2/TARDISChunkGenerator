package me.eccentric_nz.tardischunkgenerator.disguise;

import net.minecraft.server.v1_14_R1.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftPlayer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

public class TARDISDisguiser {

    private static final boolean nameVisible = false;
    private final Player player;
    private Object[] options;
    private EntityType entityType;
    private Entity entity;

    public TARDISDisguiser(Player player) {
        this.player = player;
    }

    public TARDISDisguiser(EntityType entityType, Player player) {
        this.entityType = entityType;
        this.player = player;
        options = null;
        createDisguise();
    }

    public TARDISDisguiser(EntityType entityType, Player player, Object[] options) {
        this.entityType = entityType;
        this.player = player;
        this.options = options;
        createDisguise();
    }

    public static void disguiseToPlayer(Player to, org.bukkit.World world) {
        for (Player p : Bukkit.getServer().getOnlinePlayers()) {
            if (p.getWorld() == world) {
                if (TARDISDisguiseTracker.DISGUISED_AS_PLAYER.contains(p.getUniqueId())) {
                    TARDISPlayerDisguiser.disguiseToPlayer(p, to);
                }
                if (TARDISDisguiseTracker.DISGUISED_AS_MOB.containsKey(p.getUniqueId())) {
                    TARDISDisguise disguise = TARDISDisguiseTracker.DISGUISED_AS_MOB.get(p.getUniqueId());
                    Entity mob = TARDISDisguise.createMobDisguise(disguise, world);
                    if (mob != null) {
                        // set location
                        setEntityLocationIdAndName(mob, p.getLocation(), p, nameVisible);
                        PacketPlayOutEntityDestroy packetPlayOutEntityDestroy = new PacketPlayOutEntityDestroy(p.getEntityId());
                        PacketPlayOutSpawnEntityLiving packetPlayOutSpawnEntityLiving = new PacketPlayOutSpawnEntityLiving((EntityLiving) mob);
                        ((CraftPlayer) to).getHandle().playerConnection.sendPacket(packetPlayOutEntityDestroy);
                        ((CraftPlayer) to).getHandle().playerConnection.sendPacket(packetPlayOutSpawnEntityLiving);
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
            setEntityLocationIdAndName(mob, player.getLocation(), player, nameVisible);
            TARDISDisguiseTracker.DISGUISED_AS_MOB.put(player.getUniqueId(), new TARDISDisguise(disguise.getEntityType(), disguise.getOptions()));
            PacketPlayOutEntityDestroy packetPlayOutEntityDestroy = new PacketPlayOutEntityDestroy(player.getEntityId());
            PacketPlayOutSpawnEntityLiving packetPlayOutSpawnEntityLiving = new PacketPlayOutSpawnEntityLiving((EntityLiving) mob);
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p != player && player.getWorld() == p.getWorld()) {
                    ((CraftPlayer) p).getHandle().playerConnection.sendPacket(packetPlayOutEntityDestroy);
                    ((CraftPlayer) p).getHandle().playerConnection.sendPacket(packetPlayOutSpawnEntityLiving);
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
        entity.yaw = fixYaw(location.getYaw());
        entity.pitch = location.getPitch();
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
                setEntityLocationIdAndName(entity, location, player, nameVisible);
            }
        }
    }

    public void removeDisguise() {
        if (TARDISDisguiseTracker.DISGUISED_AS_PLAYER.contains(player.getUniqueId())) {
            new TARDISPlayerDisguiser(player, player.getUniqueId()).disguiseToAll();
            TARDISDisguiseTracker.DISGUISED_AS_PLAYER.remove(player.getUniqueId());
        } else {
            TARDISDisguiseTracker.DISGUISED_AS_MOB.remove(player.getUniqueId());
            PacketPlayOutEntityDestroy packetPlayOutEntityDestroy = new PacketPlayOutEntityDestroy(player.getEntityId());
            PacketPlayOutNamedEntitySpawn packetPlayOutNamedEntitySpawn = new PacketPlayOutNamedEntitySpawn(((CraftPlayer) player).getHandle());
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p != player && player.getWorld() == p.getWorld()) {
                    ((CraftPlayer) p).getHandle().playerConnection.sendPacket(packetPlayOutEntityDestroy);
                    ((CraftPlayer) p).getHandle().playerConnection.sendPacket(packetPlayOutNamedEntitySpawn);
                }
            }
        }
    }

    public void disguiseToAll() {
        TARDISDisguiseTracker.DISGUISED_AS_MOB.put(player.getUniqueId(), new TARDISDisguise(entityType, options));
        PacketPlayOutEntityDestroy packetPlayOutEntityDestroy = new PacketPlayOutEntityDestroy(player.getEntityId());
        PacketPlayOutSpawnEntityLiving packetPlayOutSpawnEntityLiving = new PacketPlayOutSpawnEntityLiving((EntityLiving) entity);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p != player && player.getWorld() == p.getWorld()) {
                ((CraftPlayer) p).getHandle().playerConnection.sendPacket(packetPlayOutEntityDestroy);
                ((CraftPlayer) p).getHandle().playerConnection.sendPacket(packetPlayOutSpawnEntityLiving);
            }
        }
    }
}
