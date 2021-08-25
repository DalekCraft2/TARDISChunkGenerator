package me.eccentric_nz.tardischunkgenerator.custombiome;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.core.IRegistryWritable;
import net.minecraft.network.protocol.game.PacketPlayOutMapChunk;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.world.level.World;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.chunk.BiomeStorage;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_17_R1.CraftChunk;
import org.bukkit.craftbukkit.v1_17_R1.CraftServer;
import org.bukkit.craftbukkit.v1_17_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class BiomeHelper {

    DedicatedServer dedicatedServer = ((CraftServer) Bukkit.getServer()).getServer();

    /**
     * Set a chunk to a custom biome
     *
     * @param newBiomeName the name of the custom biome to set (such as tardis:skaro_lakes)
     * @param chunk        the chunk to set the biome for
     * @return true if the biome was set
     */
    public boolean setCustomBiome(String newBiomeName, Chunk chunk) {
        IRegistryWritable<BiomeBase> registryWritable = dedicatedServer.getCustomRegistry().b(IRegistry.aO);
        ResourceKey<BiomeBase> key = ResourceKey.a(IRegistry.aO, new MinecraftKey(newBiomeName.toLowerCase()));
        BiomeBase base = registryWritable.a(key);
        if (base == null) {
            if (newBiomeName.contains(":")) {
                ResourceKey<BiomeBase> newKey = ResourceKey.a(IRegistry.aO, new MinecraftKey(newBiomeName.split(":")[0].toLowerCase(), newBiomeName.split(":")[1].toLowerCase()));
                base = registryWritable.a(newKey);
                if (base == null) {
                    return false;
                }
            } else {
                return false;
            }
        }
        World world = ((CraftWorld) chunk.getWorld()).getHandle();
        for (int x = 0; x <= 15; x++) {
            for (int z = 0; z <= 15; z++) {
                for (int y = 0; y <= chunk.getWorld().getMaxHeight(); y++) {
                    setCustomBiome(chunk.getX() * 16 + x, y, chunk.getZ() * 16 + z, world, base);
                }
            }
        }
        refreshChunksForAll(chunk);
        return true;
    }

    /**
     * Set a location to a custom biome
     *
     * @param newBiomeName the name of the custom biome to set (such as tardis:skaro_lakes)
     * @param location     the location to set the biome for
     * @return true if the biome was set
     */
    public boolean setCustomBiome(String newBiomeName, Location location) {
        BiomeBase base;
        IRegistryWritable<BiomeBase> registryWritable = dedicatedServer.getCustomRegistry().b(IRegistry.aO);
        ResourceKey<BiomeBase> key = ResourceKey.a(IRegistry.aO, new MinecraftKey(newBiomeName.toLowerCase()));
        base = registryWritable.a(key);
        if (base == null) {
            if (newBiomeName.contains(":")) {
                ResourceKey<BiomeBase> newKey = ResourceKey.a(IRegistry.aO, new MinecraftKey(newBiomeName.split(":")[0].toLowerCase(), newBiomeName.split(":")[1].toLowerCase()));
                base = registryWritable.a(newKey);
                if (base == null) {
                    return false;
                }
            } else {
                return false;
            }
        }
        setCustomBiome(location.getBlockX(), location.getBlockY(), location.getBlockZ(), ((CraftWorld) location.getWorld()).getHandle(), base);
        refreshChunksForAll(location.getChunk());
        return true;
    }

    private void setCustomBiome(int x, int y, int z, World world, BiomeBase biomeBase) {
        BlockPosition pos = new BlockPosition(x, 0, z);
        if (world.isLoaded(pos)) {
            net.minecraft.world.level.chunk.Chunk chunk = world.getChunkAtWorldCoords(pos);
            if (chunk != null) {
                BiomeStorage biomeStorage = chunk.getBiomeIndex();
                if (biomeStorage != null) {
                    biomeStorage.setBiome(x >> 2, y >> 2, z >> 2, biomeBase);
                    chunk.markDirty();
                }
            }
        }
    }

    /**
     * Refreshes biome changes for players by resending the MapChunk packet
     *
     * @param chunk the chunk to refresh
     */
    private void refreshChunksForAll(Chunk chunk) {
        net.minecraft.world.level.chunk.Chunk nmsChunk = ((CraftChunk) chunk).getHandle();
        int viewDistance = Bukkit.getServer().getViewDistance() * 16;
        int viewDistanceSquared = viewDistance * viewDistance;
        for (Player player : chunk.getWorld().getPlayers()) {
            if (player.isOnline() && player.getLocation().distanceSquared(chunk.getBlock(0, 0, 0).getLocation()) < viewDistanceSquared) {
                ((CraftPlayer) player).getHandle().b.sendPacket(new PacketPlayOutMapChunk(nmsChunk));
            }
        }
    }
}
