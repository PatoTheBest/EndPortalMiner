package net.megaplanet.endportalminer;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.EnumSet;

public class MineTask extends BukkitRunnable {

    private final static EnumSet<BlockFace> RELATIVE_PORTAL_FRAMES = EnumSet.of(BlockFace.EAST, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.WEST);
    private final static ItemStack PORTAL_FRAME = new ItemStack(Material.END_PORTAL_FRAME);

    private final Player player;
    private final Block block;
    private final World world;
    private final Location particleLocation;
    private final double ticksToMine;
    private final String miningMessage;
    private int ticks = 0;

    public MineTask(Player player, Block block, MineFace face, double ticksToMine, String miningMessage) {
        this.player = player;
        this.world = player.getWorld();
        this.block = block;
        this.ticksToMine = ticksToMine;
        this.miningMessage = miningMessage;

        particleLocation = block.getLocation().add(face.getOffset());
    }

    @Override
    public void run() {
        if (ticks % 2 == 0) {
            double progress = ticks/ticksToMine;
            displayProgress(progress, player);
        }

        if (ticks % 3 == 0) {
            world.spawnParticle(Particle.PORTAL, particleLocation, 1, 0,  0, 0, 0);
        }

        if (ticks >= ticksToMine) {
            cancel();
            BlockBreakEvent blockBreakEvent = new BlockBreakEvent(block, player);
            Bukkit.getServer().getPluginManager().callEvent(blockBreakEvent);

            if (blockBreakEvent.isCancelled()) {
                return;
            }

            world.spawnParticle(Particle.BLOCK_CRACK, block.getLocation(), 12, 0,  0, 0, block.getBlockData());
            world.playSound(block.getLocation(), Sound.BLOCK_GLASS_BREAK, 10, 1f);
            block.breakNaturally();
            breakPortalBlocks(block);
            world.dropItemNaturally(particleLocation, PORTAL_FRAME);
            damageTool(player);
            return;
        }

        ticks++;
    }

    private void breakPortalBlocks(Block block) {
        for (BlockFace relativePortalFrame : RELATIVE_PORTAL_FRAMES) {
            Block adjacentBlock = block.getRelative(relativePortalFrame);
            if (adjacentBlock.getType() == Material.END_PORTAL) {
                world.spawnParticle(Particle.BLOCK_CRACK, adjacentBlock.getLocation(), 12, 0,  0, 0, adjacentBlock.getBlockData());
                world.playSound(adjacentBlock.getLocation(), Sound.BLOCK_GLASS_BREAK, 10, 1f);
                adjacentBlock.breakNaturally();
                breakPortalBlocks(adjacentBlock);
            }
        }
    }

    public void damageTool(Player player) {
        ItemStack tool = player.getInventory().getItemInMainHand();
        ItemMeta itemMeta = tool.getItemMeta();

        if(itemMeta instanceof Damageable) {
            Damageable damageable = (Damageable) itemMeta;

            // (100 ÷ (level+1))%
            int enchantmentLevel = tool.getEnchantmentLevel(Enchantment.DURABILITY);
            double percentChance = (1.0 / (enchantmentLevel+1));

            if (Math.random() > percentChance) {
                return;
            }

            damageable.setDamage(damageable.getDamage() + 1);
            tool.setItemMeta((ItemMeta) damageable);

            if (damageable.getDamage() >= tool.getType().getMaxDurability()) {
                player.getWorld().spawnParticle(Particle.ITEM_CRACK, player.getLocation(), 12, tool);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 10, 1f);
                player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
            }
        }
    }

    private void displayProgress(double progress, Player player) {
        int bars = 24;
        StringBuilder progressBar = new StringBuilder(ChatColor.YELLOW.toString()).append(miningMessage);
        progressBar.append(ChatColor.GREEN);
        boolean hasChangedColor = false;
        for (int bar = 0; bar < bars; bar++) {
            if (!hasChangedColor && (float) bar / (float) bars >= progress) {
                progressBar.append(ChatColor.WHITE);
                hasChangedColor = true;
            }

            progressBar.append("#");
        }
        progressBar.append(" ").append(ChatColor.YELLOW).append(Math.ceil(progress * 100.0)).append("%");

        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(progressBar.toString()));
    }

    @Override
    public synchronized void cancel() throws IllegalStateException {
        if (isCancelled()) {
            return;
        }

        super.cancel();
    }
}
