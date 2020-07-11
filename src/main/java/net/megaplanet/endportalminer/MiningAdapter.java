package net.megaplanet.endportalminer;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.EnumWrappers;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

public class MiningAdapter extends PacketAdapter implements Listener, Runnable {

    private final EndPortalMiner plugin;
    private final Map<Player, MineTask> tasks = new HashMap<>();
    private final BukkitTask bukkitTask;

    public MiningAdapter(EndPortalMiner plugin) {
        super(plugin, ListenerPriority.NORMAL, PacketType.Play.Client.BLOCK_DIG);
        this.plugin = plugin;
        this.bukkitTask = this.plugin.getServer().getScheduler().runTaskTimer(plugin, this, 20L, 20L);
    }

    @Override
    public void onPacketReceiving(PacketEvent event){
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE) {
            return;
        }

        PacketContainer packet = event.getPacket();
        EnumWrappers.PlayerDigType digType = packet.getPlayerDigTypes().read(0);

        if (digType == EnumWrappers.PlayerDigType.START_DESTROY_BLOCK) {
            BlockPosition blockPosition = packet.getBlockPositionModifier().read(0);
            EnumWrappers.Direction direction = packet.getDirections().read(0);
            handleDig(event.getPlayer(), blockPosition, direction);
        } else if (digType == EnumWrappers.PlayerDigType.ABORT_DESTROY_BLOCK) {
            cancelTask(event.getPlayer());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancelTask(event.getPlayer());
    }

    @Override
    public void run() {
        for (Map.Entry<Player, MineTask> playerMineTaskEntry : tasks.entrySet()) {
            if (playerMineTaskEntry.getValue().isCancelled()) {
                tasks.remove(playerMineTaskEntry.getKey());
            }
        }
    }

    public void destroy() {
        for (MineTask value : tasks.values()) {
            value.cancel();
        }
        tasks.clear();
        bukkitTask.cancel();
    }

    private void cancelTask(Player player) {
        MineTask remove = tasks.remove(player);

        if (remove == null) {
            return;
        }

        remove.cancel();
    }

    private void handleDig(Player player, BlockPosition blockPosition, EnumWrappers.Direction direction) {
        ItemStack itemInMainHand = player.getInventory().getItemInMainHand();
        if (itemInMainHand.getType() == Material.AIR) {
            return;
        }

        Block blockAt = player.getWorld().getBlockAt(blockPosition.getX(), blockPosition.getY(), blockPosition.getZ());
        if (blockAt.getType() != Material.END_PORTAL_FRAME) {
            return;
        }

        if (!plugin.getMiningTimes().containsKey(itemInMainHand.getType())) {
            return;
        }
        double miningTime = plugin.getMiningTimes().get(itemInMainHand.getType());
        int enchantmentLevel = itemInMainHand.getEnchantmentLevel(Enchantment.DIG_SPEED);
        if (enchantmentLevel != 0) {
            enchantmentLevel *= 5;
            enchantmentLevel += 25;
            double amount = (100 - enchantmentLevel) / 100.0;
            miningTime *= amount;
        }

        MineTask mineTask = new MineTask(player, blockAt, MineFace.UP, miningTime, plugin.getMiningMessage());
        mineTask.runTaskTimer(plugin, 0, 1);
        tasks.put(player, mineTask);
    }
}
