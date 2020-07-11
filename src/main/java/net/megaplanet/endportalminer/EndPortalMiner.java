package net.megaplanet.endportalminer;

import com.comphenix.protocol.ProtocolLibrary;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class EndPortalMiner extends JavaPlugin {

    private String miningMessage;
    private MiningAdapter miningAdapter;
    private final Map<Material, Integer> miningTimes = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        ConfigurationSection miningTimes = getConfig().getConfigurationSection("mining-times");

        if (miningTimes == null) {
            getLogger().severe("Mining-times not found in config!");
        } else {
            Set<String> keys = miningTimes.getKeys(false);
            for (String key : keys) {
                try {
                    Material material = Material.valueOf(key.toUpperCase());
                    this.miningTimes.put(material, miningTimes.getInt(key));
                } catch (Exception e) {
                    getLogger().warning("Material " + key + " not found.");
                }
            }

            miningMessage = getConfig().getString("messages.mining");
        }

        this.miningAdapter = new MiningAdapter(this);
        ProtocolLibrary.getProtocolManager().addPacketListener(miningAdapter);
    }

    @Override
    public void onDisable() {
        ProtocolLibrary.getProtocolManager().removePacketListener(miningAdapter);
        miningAdapter.destroy();
        miningTimes.clear();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("endportalminer.reload")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return false;
        }

        onDisable();
        onEnable();
        sender.sendMessage(ChatColor.GREEN + "Successfully reloaded plugin!");
        return true;
    }

    public Map<Material, Integer> getMiningTimes() {
        return miningTimes;
    }

    public String getMiningMessage() {
        return miningMessage;
    }
}
