/*
 * Copyright (C) 2021 eccentric_nz
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
package me.eccentric_nz.tardischunkgenerator;

import me.eccentric_nz.tardischunkgenerator.custombiome.BiomeHelper;
import me.eccentric_nz.tardischunkgenerator.custombiome.BiomeUtilities;
import me.eccentric_nz.tardischunkgenerator.disguise.*;
import me.eccentric_nz.tardischunkgenerator.helpers.TARDISFactions;
import me.eccentric_nz.tardischunkgenerator.helpers.TARDISMapUpdater;
import me.eccentric_nz.tardischunkgenerator.helpers.TARDISPlanetData;
import me.eccentric_nz.tardischunkgenerator.keyboard.SignInputHandler;
import me.eccentric_nz.tardischunkgenerator.light.ChunkInfo;
import me.eccentric_nz.tardischunkgenerator.light.Light;
import me.eccentric_nz.tardischunkgenerator.light.LightType;
import me.eccentric_nz.tardischunkgenerator.light.RequestSteamMachine;
import me.eccentric_nz.tardischunkgenerator.logging.TARDISLogFilter;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTCompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.ChatMessageType;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.game.PacketPlayOutChat;
import net.minecraft.network.protocol.game.PacketPlayOutOpenSignEditor;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.server.network.PlayerConnection;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.BlockAttachable;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityFurnace;
import net.minecraft.world.level.block.entity.TileEntitySign;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_17_R1.CraftChunk;
import org.bukkit.craftbukkit.v1_17_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_17_R1.block.CraftBlock;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.map.MapView;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;

public class TARDISHelper extends JavaPlugin implements TARDISHelperAPI {

    public static final RequestSteamMachine MACHINE = new RequestSteamMachine();
    public static TARDISHelper plugin;
    private String messagePrefix;
    private BiomeProvider biomeProvider;

    @Override
    public void onDisable() {
        if (MACHINE.isStarted()) {
            MACHINE.shutdown();
        }
    }

    @Override
    public void onEnable() {
        messagePrefix = ChatColor.GOLD + "[" + getDescription().getName() + "]" + ChatColor.RESET + " ";
        plugin = this;
        // register disguise listener
        getServer().getPluginManager().registerEvents(new TARDISDisguiseListener(this), this);
        // start RequestStreamMachine
        MACHINE.start(2, 400);
        String basePath = getServer().getWorldContainer() + File.separator + "plugins" + File.separator + "TARDIS" + File.separator;
        // Add custom biomes
        BiomeUtilities.addBiomes(basePath);
        // get the TARDIS config
        FileConfiguration configuration = YamlConfiguration.loadConfiguration(new File(basePath + "config.yml"));
        // should we filter the log?
        if (configuration.getBoolean("debug")) {
            // yes we should!
            filterLog(basePath + "filtered.log");
            getLogger().log(Level.INFO, "Starting filtered logging for TARDIS plugins...");
            getLogger().log(Level.INFO, "Log file located at 'plugins/TARDIS/filtered.log'");
        }
        biomeProvider = new TARDISVoidBiomeProvider();
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(@NotNull String worldName, String id) {
        return new TARDISChunkGenerator();
    }

    public String getMessagePrefix() {
        return messagePrefix;
    }

    @Override
    public BiomeProvider getDefaultBiomeProvider(@NotNull String worldName, String id) {
        return biomeProvider;
    }

    @Override
    public BiomeProvider getBiomeProvider() {
        return biomeProvider;
    }

    @Override
    public void nameFurnaceGUI(Block block, String name) {
        if (block != null) {
            WorldServer worldServer = ((CraftWorld) block.getWorld()).getHandle();
            BlockPosition blockPosition = new BlockPosition(block.getX(), block.getY(), block.getZ());
            TileEntity tileEntity = worldServer.getTileEntity(blockPosition);
            if (tileEntity instanceof TileEntityFurnace tileEntityFurnace) {
                tileEntityFurnace.setCustomName(new ChatMessage(name));
            }
        }
    }

    @Override
    public boolean isArtronFurnace(Block block) {
        if (block != null) {
            WorldServer worldServer = ((CraftWorld) block.getWorld()).getHandle();
            BlockPosition blockPosition = new BlockPosition(block.getX(), block.getY(), block.getZ());
            TileEntity tileEntity = worldServer.getTileEntity(blockPosition);
            if (tileEntity instanceof TileEntityFurnace tileEntityFurnace && tileEntityFurnace.getCustomName() != null) {
                return tileEntityFurnace.getCustomName().getString().equals("TARDIS Artron Furnace");
            }
        }
        return false;
    }

    @Override
    public void setFallFlyingTag(org.bukkit.entity.Entity entity) {
        Entity nmsEntity = ((CraftEntity) entity).getHandle();
        NBTTagCompound tag = new NBTTagCompound();
        // writes the entity's NBT data to the `tag` object
        nmsEntity.save(tag);
        tag.setBoolean("FallFlying", true);
        // sets the entity's tag to the altered `tag`
        nmsEntity.load(tag);
    }

    @Override
    public void openSignGUI(Player player, Sign sign) {
        Location location = sign.getLocation();
        TileEntitySign tileEntitySign = (TileEntitySign) ((CraftWorld) location.getWorld()).getHandle().getTileEntity(new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ()));
        EntityPlayer entityPlayer = ((CraftPlayer) player.getPlayer()).getHandle();
        entityPlayer.b.sendPacket(tileEntitySign.getUpdatePacket()); // b = playerConnection
        tileEntitySign.f = true; // f = isEditable
        tileEntitySign.a(entityPlayer);
        PacketPlayOutOpenSignEditor packet = new PacketPlayOutOpenSignEditor(tileEntitySign.getPosition());
        entityPlayer.b.sendPacket(packet);
        SignInputHandler.injectNetty(player, this);
    }

    @Override
    public void finishSignEditing(Player player) {
        SignInputHandler.ejectNetty(player);
    }

    @Override
    public void setRandomSeed(String world) {
        File file = new File(Bukkit.getWorldContainer().getAbsolutePath() + File.separator + world + File.separator + "level.dat");
        if (file.exists()) {
            try {
                FileInputStream fileInputStream = new FileInputStream(file);
                NBTTagCompound tagCompound = NBTCompressedStreamTools.a(fileInputStream);
                NBTTagCompound data = tagCompound.getCompound("Data");
                fileInputStream.close();
                long random = new Random().nextLong();
                // set RandomSeed tag
                data.setLong("RandomSeed", random);
                tagCompound.set("Data", data);
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                NBTCompressedStreamTools.a(tagCompound, fileOutputStream);
                fileOutputStream.close();
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, e.getMessage());
            }
        }
    }

    @Override
    public void setLevelName(String oldName, String newName) {
        File file = new File(Bukkit.getWorldContainer().getAbsolutePath() + File.separator + oldName + File.separator + "level.dat");
        if (file.exists()) {
            try {
                FileInputStream fileInputStream = new FileInputStream(file);
                NBTTagCompound tagCompound = NBTCompressedStreamTools.a(fileInputStream);
                NBTTagCompound data = tagCompound.getCompound("Data");
                fileInputStream.close();
                // set LevelName tag
                data.setString("LevelName", newName);
                tagCompound.set("Data", data);
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                NBTCompressedStreamTools.a(tagCompound, fileOutputStream);
                fileOutputStream.close();
                getLogger().log(Level.INFO, "Renamed level to " + newName);
                // rename the directory
                File directory = new File(Bukkit.getWorldContainer().getAbsolutePath() + File.separator + oldName);
                File folder = new File(Bukkit.getWorldContainer().getAbsolutePath() + File.separator + newName);
                directory.renameTo(folder);
                getLogger().log(Level.INFO, "Renamed directory to " + newName);
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, e.getMessage());
            }
        }
    }

    @Override
    public void setWorldGameMode(String world, GameMode gameMode) {
        File file = new File(Bukkit.getWorldContainer().getAbsolutePath() + File.separator + world + File.separator + "level.dat");
        if (file.exists()) {
            try {
                FileInputStream fileInputStream = new FileInputStream(file);
                NBTTagCompound tagCompound = NBTCompressedStreamTools.a(fileInputStream);
                NBTTagCompound data = tagCompound.getCompound("Data");
                fileInputStream.close();
                int gameModeInt = switch (gameMode) {
                    case CREATIVE -> 1;
                    case ADVENTURE -> 2;
                    case SPECTATOR -> 3;
                    default -> 0; // SURVIVAL
                };
                // set GameType tag
                data.setInt("GameType", gameModeInt);
                tagCompound.set("Data", data);
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                NBTCompressedStreamTools.a(tagCompound, fileOutputStream);
                fileOutputStream.close();
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, e.getMessage());
            }
        }
    }

    @Override
    public TARDISPlanetData getLevelData(String world) {
        File file = new File(Bukkit.getWorldContainer().getAbsolutePath() + File.separator + world + File.separator + "level.dat");
        if (file.exists()) {
            try {
                FileInputStream fileInputStream = new FileInputStream(file);
                NBTTagCompound tagCompound = NBTCompressedStreamTools.a(fileInputStream);
                fileInputStream.close();
                NBTTagCompound data = tagCompound.getCompound("Data");
                // get GameType tag
                GameMode gameMode;
                int gameModeInt = data.getInt("GameType");
                gameMode = switch (gameModeInt) {
                    case 1 -> GameMode.CREATIVE;
                    case 2 -> GameMode.ADVENTURE;
                    case 3 -> GameMode.SPECTATOR;
                    default -> GameMode.SURVIVAL;
                };
                // get generatorName tag
                WorldType worldType;
                String generatorName = data.getString("generatorName");
                worldType = switch (generatorName.toLowerCase(Locale.ROOT)) {
                    case "flat" -> WorldType.FLAT;
                    case "largeBiomes" -> WorldType.LARGE_BIOMES;
                    case "amplified" -> WorldType.AMPLIFIED;
                    default -> WorldType.NORMAL; // default or unknown
                };
                World.Environment environment = World.Environment.NORMAL;
                File dimDashOne = new File(Bukkit.getWorldContainer().getAbsolutePath() + File.separator + world + File.separator + "DIM-1");
                File dimOne = new File(Bukkit.getWorldContainer().getAbsolutePath() + File.separator + world + File.separator + "DIM1");
                if (dimDashOne.exists() && !dimOne.exists()) {
                    environment = World.Environment.NETHER;
                }
                if (dimOne.exists() && !dimDashOne.exists()) {
                    environment = World.Environment.THE_END;
                }
                return new TARDISPlanetData(gameMode, environment, worldType);
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, e.getMessage());
                return new TARDISPlanetData(GameMode.SURVIVAL, World.Environment.NORMAL, WorldType.NORMAL);
            }
        }
        getLogger().log(Level.INFO, "Defaulted to GameMode.SURVIVAL, World.Environment.NORMAL, WorldType.NORMAL");
        return new TARDISPlanetData(GameMode.SURVIVAL, World.Environment.NORMAL, WorldType.NORMAL);
    }

    @Override
    public void disguise(EntityType entityType, Player player) {
        new TARDISDisguiser(this, entityType, player).disguiseToAll();
    }

    @Override
    public void disguise(EntityType entityType, Player player, Object[] options) {
        new TARDISDisguiser(this, entityType, player, options).disguiseToAll();
    }

    @Override
    public void disguise(Player player, String name) {
        new TARDISChameleonArchDisguiser(this, player).changeSkin(name);
    }

    @Override
    public void disguise(Player player, UUID uuid) {
        new TARDISPlayerDisguiser(this, player, uuid).disguiseToAll();
    }

    @Override
    public void undisguise(Player player) {
        new TARDISDisguiser(this, player).removeDisguise();
    }

    @Override
    public void reset(Player player) {
        new TARDISChameleonArchDisguiser(this, player).resetSkin();
    }

    @Override
    public int spawnEmergencyProgrammeOne(Player player, Location location) {
        return new TARDISEPSDisguiser(player, location).showToAll();
    }

    @Override
    public void removeNPC(int id, World world) {
        TARDISEPSDisguiser.removeNPC(id, world);
    }

    @Override
    public void disguiseArmourStand(ArmorStand armorStand, EntityType entityType, Object[] options) {
        new TARDISArmourStandDisguiser(this, armorStand, entityType, options).disguiseToAll();
    }

    @Override
    public void undisguiseArmourStand(ArmorStand armorStand) {
        TARDISArmourStandDisguiser.removeDisguise(armorStand);
    }

    @Override
    public void createLight(Location location) {
        Light.createLight(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), LightType.BLOCK, 15, true);
        Collection<Player> players = location.getWorld().getPlayers();
        for (ChunkInfo info : Light.collectChunks(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), LightType.BLOCK, 15)) {
            Light.updateChunk(info, LightType.BLOCK, players);
        }
    }

    @Override
    public void deleteLight(Location location) {
        Light.deleteLight(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), LightType.BLOCK, true);
        Collection<Player> players = location.getWorld().getPlayers();
        for (ChunkInfo info : Light.collectChunks(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), LightType.BLOCK, 15)) {
            Light.updateChunk(info, LightType.BLOCK, players);
        }
    }

    @Override
    public boolean isInFaction(Player player, Location location) {
        return new TARDISFactions().isInFaction(player, location);
    }

    @Override
    public void updateMap(World world, MapView mapView) {
        new TARDISMapUpdater(world, mapView.getCenterX(), mapView.getCenterZ()).update(mapView);
    }

    @Override
    public void sendActionBarMessage(Player player, String message) {
        PlayerConnection connection = ((CraftPlayer) player).getHandle().b; // b = playerConnection
        if (connection == null) {
            return;
        }
        IChatBaseComponent component = new ChatComponentText(message);
        PacketPlayOutChat packet = new PacketPlayOutChat(component, ChatMessageType.c, player.getUniqueId()); // c = GAME_INFO
        connection.sendPacket(packet);
    }

    @Override
    public Location searchBiome(World world, Biome biome, Player player, Location policeBox) {
        return BiomeUtilities.searchBiome(world, biome, player, policeBox);
    }

    @Override
    public void setCustomBiome(String biome, Chunk chunk) {
        new BiomeHelper().setCustomBiome(biome, chunk);
    }

    @Override
    public String getBiomeKey(Location location) {
        return BiomeUtilities.getBiomeKey(location);
    }

    @Override
    public String getBiomeKey(Chunk chunk) {
        return BiomeUtilities.getBiomeKey(chunk);
    }

    @Override
    public void removeTileEntity(BlockState tile) {
        net.minecraft.world.level.chunk.Chunk chunk = ((CraftChunk) tile.getChunk()).getHandle();
        BlockPosition position = new BlockPosition(tile.getLocation().getX(), tile.getLocation().getY(), tile.getLocation().getZ());
        chunk.removeTileEntity(position);
        tile.getBlock().setType(Material.AIR);
    }

    @Override
    public void setPowerableBlockInteract(Block block) {
        IBlockData data = ((CraftBlock) block).getNMS();
        net.minecraft.world.level.World world = ((CraftWorld) block.getWorld()).getHandle();
        BlockPosition position = ((CraftBlock) block).getPosition();
        data.interact(world, null, null, MovingObjectPositionBlock.a(data.n(world, position), data.get(BlockAttachable.aE), position)); // aE = BlockStateDirection
    }

    /**
     * Start filtering logs for TARDIS related information
     *
     * @param path the file path for the filtered log file
     */
    public void filterLog(String path) {
        ((Logger) LogManager.getRootLogger()).addFilter(new TARDISLogFilter(path));
    }
}
