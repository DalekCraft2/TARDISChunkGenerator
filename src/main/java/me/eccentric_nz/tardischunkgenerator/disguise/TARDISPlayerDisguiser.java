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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.util.UUIDTypeAdapter;
import me.eccentric_nz.tardischunkgenerator.TARDISHelper;
import net.minecraft.server.level.EntityPlayer;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.UUID;
import java.util.logging.Level;

public class TARDISPlayerDisguiser {

    private final TARDISHelper plugin;
    private final Player player;
    private final UUID uuid;

    public TARDISPlayerDisguiser(TARDISHelper plugin, Player player, UUID uuid) {
        this.plugin = plugin;
        this.player = player;
        this.uuid = uuid;
        disguisePlayer();
    }

    public static void disguiseToPlayer(Player disguised, Player to) {
        to.hidePlayer(TARDISHelper.plugin, disguised);
        to.showPlayer(TARDISHelper.plugin, disguised);
    }

    public void disguisePlayer() {
        EntityPlayer entityPlayer = ((CraftPlayer) player).getHandle();
        // set skin
        if (setSkin(entityPlayer.getProfile(), uuid) && !TARDISDisguiseTracker.DISGUISED_AS_PLAYER.contains(player.getUniqueId())) {
            TARDISDisguiseTracker.DISGUISED_AS_PLAYER.add(player.getUniqueId());
        }
    }

    private boolean setSkin(GameProfile profile, UUID uuid) {
        try {
            URL url = new URL(String.format("https://sessionserver.mojang.com/session/minecraft/profile/%s?unsigned=false", UUIDTypeAdapter.fromUUID(uuid)));
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.connect();
            if (connection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
                JsonParser jsonParser = new JsonParser();
                JsonElement root = jsonParser.parse(new InputStreamReader((InputStream) connection.getContent())); //Convert the input stream to a json element
                JsonObject rootObject = root.getAsJsonObject();
                JsonArray jsonArray = rootObject.getAsJsonArray("properties");
                JsonObject properties = jsonArray.get(0).getAsJsonObject();
                String skin = properties.get("value").getAsString();
                String signature = properties.get("signature").getAsString();
                profile.getProperties().removeAll("textures");
                return profile.getProperties().put("textures", new Property("textures", skin, signature));
            } else {
                plugin.getLogger().log(Level.INFO, "Connection could not be opened (Response code " + connection.getResponseCode() + ", " + connection.getResponseMessage() + ")");
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void disguiseToAll() {
        TARDISDisguiseTracker.DISGUISED_AS_PLAYER.add(player.getUniqueId());
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player != this.player && this.player.getWorld() == player.getWorld()) {
                player.hidePlayer(plugin, this.player);
                player.showPlayer(plugin, this.player);
            }
        }
    }
}
