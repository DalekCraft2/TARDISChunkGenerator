package me.eccentric_nz.tardischunkgenerator.custombiome;

import com.mojang.serialization.Lifecycle;
import me.eccentric_nz.tardischunkgenerator.TARDISHelper;
import net.minecraft.core.IRegistry;
import net.minecraft.core.IRegistryWritable;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.BiomeFog;
import net.minecraft.world.level.biome.BiomeSettingsGeneration;
import net.minecraft.world.level.biome.BiomeSettingsMobs;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_17_R1.CraftServer;

import java.lang.reflect.Field;
import java.util.logging.Level;

public class CustomBiome {

    public static void addCustomBiome(CustomBiomeData data) {

        DedicatedServer dedicatedServer = ((CraftServer) Bukkit.getServer()).getServer();
        ResourceKey<BiomeBase> minecraftKey = ResourceKey.a(IRegistry.aO, new MinecraftKey("minecraft", data.getMinecraftName()));
        ResourceKey<BiomeBase> customKey = ResourceKey.a(IRegistry.aO, new MinecraftKey("tardis", data.getCustomName()));
        IRegistryWritable<BiomeBase> registryWritable = dedicatedServer.getCustomRegistry().b(IRegistry.aO);
        BiomeBase minecraftBiome = registryWritable.a(minecraftKey);
        BiomeBase.a newBiome = new BiomeBase.a();
        try {
            Field biomeSettingMobsField = BiomeBase.class.getDeclaredField("m");
            biomeSettingMobsField.setAccessible(true);
            newBiome.a(minecraftBiome.t());
            newBiome.a(minecraftBiome.c());
            BiomeSettingsMobs biomeSettingMobs = (BiomeSettingsMobs) biomeSettingMobsField.get(minecraftBiome);
            Field biomeSettingGenField = BiomeBase.class.getDeclaredField("l");
            biomeSettingGenField.setAccessible(true);
            newBiome.a(biomeSettingMobs);
            BiomeSettingsGeneration biomeSettingGen = (BiomeSettingsGeneration) biomeSettingGenField.get(minecraftBiome);
            newBiome.a(biomeSettingGen);
            newBiome.a(data.getDepth()); // depth of biome
            newBiome.b(data.getScale()); // scale of biome
            newBiome.c(data.getTemperature()); // temperature of biome
            newBiome.d(data.getDownfall()); // downfall of biome
            newBiome.a(data.isFrozen() ? BiomeBase.TemperatureModifier.b : BiomeBase.TemperatureModifier.a); // BiomeBase.TemperatureModifier.a will make your biome normal, BiomeBase.TemperatureModifier.b will make your biome frozen
            BiomeFog.a newFog = new BiomeFog.a();
            newFog.a(BiomeFog.GrassColor.a); // this doesn't affect the actual final grass color, just leave this line as it is, or you will get errors
            // necessary values; removing them will break your biome
            newFog.a(data.getFogColour()); // fog colour
            newFog.b(data.getWaterColour()); // water colour
            newFog.c(data.getWaterFogColour()); // water fog colour
            newFog.d(data.getSkyColour()); // sky colour
            newFog.e(data.getFoliageColour()); // foliage colour (leaves, vines and more)
            newFog.f(data.getGrassColour()); // grass blocks colour
            newBiome.a(newFog.a());
            dedicatedServer.getCustomRegistry().b(IRegistry.aO).a(customKey, newBiome.a(), Lifecycle.stable());
        } catch (IllegalAccessException | IllegalArgumentException | NoSuchFieldException | SecurityException e) {
            TARDISHelper.plugin.getLogger().log(Level.WARNING, "Exception adding custom biome to registry: %s", e.getMessage());
        }
    }
}
