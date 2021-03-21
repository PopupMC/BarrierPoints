package com.popupmc.barrierpoints;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

public class BarrierPoints extends JavaPlugin implements CommandExecutor {
    @Override
    public void onEnable() {
        Objects.requireNonNull(this.getCommand("bp")).setExecutor(this);

        // Add points every 10 minutes
        new BukkitRunnable() {
            @Override
            public void run() {
                addPoints();
            }
        }.runTaskTimer(this, 1, 20 * 60 * 10);

        // Log enabled status
        getLogger().info("JoinTravel is enabled.");
    }

    // Log disabled status
    @Override
    public void onDisable() {
        getLogger().info("JoinTravel is disabled");
    }

    public void addPoints() {
        final Collection<? extends Player> players = Bukkit.getOnlinePlayers();

        for (Player player : players) {
            if(player.isOp())
                continue;

            int bal = pointsAvail.getOrDefault(player.getUniqueId(), 0);
            bal += 2;
            pointsAvail.put(player.getUniqueId(), bal);

            player.sendMessage(ChatColor.GREEN + "You've been awarded 2 barrier points to give to others, /bp");
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if(args.length == 1 && args[0].equalsIgnoreCase("bal")) {
            if(!(sender instanceof  Player)) {
                sender.sendMessage(ChatColor.GOLD + "Points Balance is: " + "Unlimited, your console.");
                return true;
            }

            Player fromPlayer = (Player)sender;

            if(fromPlayer.isOp()) {
                sender.sendMessage(ChatColor.GOLD + "Points Balance is: " + "Unlimited, your admin.");
            }
            else {
                sender.sendMessage(ChatColor.GOLD + "Points Balance is: " +
                        pointsAvail.getOrDefault(fromPlayer.getUniqueId(), 0));
            }
            return true;
        }

        // Ensure name and amount
        if(args.length < 2) {
            sender.sendMessage(ChatColor.RED + "You must specify either 'bal' or player's name and amount");
            sender.sendMessage(ChatColor.RED + "/bp [bal|<name> <amt>]");
            return true;
        }

        // Get name and amount
        String toName = args[0].toLowerCase();
        int toAmount;

        try {
            toAmount = Integer.parseInt(args[1]);
        }
        catch (Exception ex) {
            sender.sendMessage(ChatColor.RED + "Unable to read the amount given, are you using whole numbers?");
            return true;
        }

        // Convert name to player
        Player toPlayer = Bukkit.getPlayer(toName);

        // Make sure online
        if(toPlayer == null) {
            sender.sendMessage(ChatColor.RED + "Unable to find that player, are they online?");
            return true;
        }

        // Get from player if not console
        Player fromPlayer = null;
        if(sender instanceof Player)
            fromPlayer = (Player)sender;

        // Ensure not giving to self
        if(fromPlayer != null && toPlayer.getUniqueId().equals(fromPlayer.getUniqueId())) {
            sender.sendMessage(ChatColor.RED + "You can't give barrier points to yourself");
            return true;
        }

        // Get current points
        int curPointsToGive = -1;

        // Get points to give if not console or op
        if(fromPlayer != null && !fromPlayer.isOp())
            curPointsToGive = pointsAvail.getOrDefault(fromPlayer.getUniqueId(), 0);

        // Ensure has enough to give and not console
        if(curPointsToGive < toAmount && curPointsToGive > -1) {
            sender.sendMessage(ChatColor.RED + "You don't have enough points to give that many.");
            sender.sendMessage(ChatColor.RED + "Asked for " + toAmount + " but only have " + curPointsToGive);
            return true;
        }

        // Give barrier points
        toPlayer.getLocation().getWorld().dropItemNaturally(
                toPlayer.getLocation(), new ItemStack(Material.BARRIER, toAmount));

        // Get player name
        String fromName = "Console";
        if(fromPlayer != null)
            fromName = fromPlayer.getDisplayName();

        // Announce points given
        toPlayer.sendMessage(ChatColor.GREEN + "" + fromName + " has given you " + toAmount + " barrier points.");

        // Deduct points spent if not console
        if(fromPlayer != null && !fromPlayer.isOp()) {
            curPointsToGive -= toAmount;
            pointsAvail.put(fromPlayer.getUniqueId(), curPointsToGive);
        }

        // Announce to sender success message
        if(fromPlayer != null) {
            // Announce points given
            fromPlayer.sendMessage(ChatColor.GREEN + "You've given  " + toPlayer.getName() + " " +
                    toAmount + " barrier points.");
            fromPlayer.sendMessage(ChatColor.GREEN + "New balance is: " + pointsAvail.getOrDefault(fromPlayer.getUniqueId(), 0));
        }

        return true;
    }

    public HashMap<UUID, Integer> pointsAvail = new HashMap<>();
}
