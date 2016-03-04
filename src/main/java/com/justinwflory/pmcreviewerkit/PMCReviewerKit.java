/*
 * Copyright (c) 2016 Justin W. Flory
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.justinwflory.pmcreviewerkit;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.BanList.Type;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.bukkit.Bukkit.getConsoleSender;
import static org.bukkit.Bukkit.getWorld;

/**
 * PMCReviewerKit.java
 *
 * This class allows a server owner of a Spigot Minecraft server to "troll the trolls" by letting players who claim to
 * be reviewing your server for Planet Minecraft to get a little surprise when they are suddenly granted the
 * permissions that they so desperately desire!
 *
 * @author Justin W. Flory <jflory7>
 * @version 2016.03.04.v1
 */
public final class PMCReviewerKit extends JavaPlugin {

    // Constants
    private final long HOURS_IN_MILLIS = TimeUnit.HOURS.toMillis(this.getConfig().getInt("command-cooldown"));

    // Variables
    File cooldowns = new File("cooldowns.yml");
    YamlConfiguration cooldownsYAML;

    /**
     * Defines plugin behavior on server start-up.
     */
    @Override
    public void onEnable() {
        getLogger().info("Enabling PMCReviewerKit...");

        cooldownsYAML = YamlConfiguration.loadConfiguration(cooldowns);

        this.saveDefaultConfig();
        if (!cooldowns.exists()) {
            try {
                cooldownsYAML.save(cooldowns);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Connect to the Metrics server and submit plugin metrics
        try {
            MetricsLite metrics = new MetricsLite(this);
            metrics.start();
        } catch (IOException e) {
            // Failed to submit the stats :-(
        }

        getLogger().info("PMCReviewerKit enabled.");
    }

    /**
     * Command listener that defines behavior for when specific commands are called or executed.
     *
     * @param sender the sender of the command
     * @param cmd the command used by the sender
     * @param label the label
     * @param args the additional arguments passed with the command
     * @return true if command executed successfully
     */
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return false;
        }

        Player p = (Player) sender;

        if (cmd.getName().equalsIgnoreCase("pmc")) {

            if (args.length == 0) {
                sender.sendMessage(ChatColor.RED + "[PMCReviewerKit] Please specify an argument.\n" + ChatColor.ITALIC
                        + "/pmc < instantop | creative | youtube | review >");
                return false;
            }

            else if (args.length >= 1) {
                // Gives player "fake OP", makes them look bad, and then kills and mutes them
                if (args[0].equalsIgnoreCase("instantop")) {
                    BukkitRunnable opThread = new InstantOp(p);
                    opThread.runTaskAsynchronously(this);
                }
                return true;
            }

            // Sets a player's gamemode to adventure
            else if (args[0].equalsIgnoreCase("creative")) {
                p.setGameMode(GameMode.ADVENTURE);
                p.sendMessage("Your gamemode has been updated");
                return true;
            }

            else if (args[0].equalsIgnoreCase("youtube")) {
                if (args.length == 1) {
                    p.sendMessage(ChatColor.RED + "[PMCReviewerKit] Invalid usage.\n" + ChatColor.ITALIC + "/pmc youtube < verify | challenge >");
                    return false;
                }
            }

            // Mutes player and instructs them to "verify" their YouTuber status by emailing a null email
            else if (args[1].equalsIgnoreCase("verify")) {
                mutePlayer(p);
                for (int i=0; i<21; i++) p.sendMessage("");
                p.sendMessage(ChatColor.GREEN + "Congratulations, you have been put on the verification waitlist! In order to receive your " +
                        "perks and privileges as soon as possible, please email proof of your YouTuber status to " + ChatColor.GRAY + "" +
                        ChatColor.BOLD + "" + ChatColor.ITALIC + this.getConfig().getString("youtube-verify-email") + ChatColor.RESET + "" +
                        ChatColor.GREEN + ", and once you have done so, your application will be reviewed and your chat privileges restored!");
                return true;
            }

            // Teleports player to the End with low health and spawns 15 creepers on them; cooldown is implemented
            else if (args[1].equalsIgnoreCase("challenge")) {
                long last = cooldownsYAML.getLong("youtube.challenge" + sender.getName(), 0L);
                long now = System.currentTimeMillis();
                if ((now - last) > HOURS_IN_MILLIS) {
                    p.teleport(new Location(getWorld("world_the_end"), 0, 75, 0));
                    p.setNoDamageTicks(500);
                    p.setHealth(0.5);
                    for (int i = 0; i < 16; i++) p.getWorld().spawnEntity(p.getLocation(), EntityType.CREEPER);
                    this.getConfig().set("youtube.challenge" + sender.getName(), now);
                } else {
                    p.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + "You have used this command too recently. Please wait " +
                            (now - last) + " hours before running this command again.");
                }
                return true;
            }

            else if (args[0].equalsIgnoreCase("review")) {
                // Ask player to confirm if they want to follow through with reviewing; if answers yes and "verifies", bans user for a month
                if (args.length == 1) {
                    p.sendMessage(ChatColor.BLUE + "Welcome to our server. Only PMC Staff Members are permitted to perform privileged reviews on our server. " +
                            "If you are a PMC Staff Member, we will need you to prove your identity for us! If you do this, you will get OP, creative, fly, and " +
                            "WorldEdit privileges.\n" + ChatColor.RED + "" + ChatColor.BOLD + "Are you a PMC Staff Member? Type " + ChatColor.ITALIC + "/pmc review <yes|no>");
                    return true;
                }

                if (args[1].equalsIgnoreCase("yes")) {
                    p.sendMessage(ChatColor.GREEN + "Welcome, PMC Staff Member! Please identify yourself using your PMC Staff Identification Number provided to you by the PMC Admins.\n" +
                            ChatColor.RED + "" + ChatColor.BOLD + "Identify yourself using " + ChatColor.ITALIC + "/pmc review verify <ID #>");
                    return true;
                }

                if (args[1].equalsIgnoreCase("no")) {
                    p.sendMessage(ChatColor.GRAY + "Sorry, you are not eligible to review our server.");
                    return true;
                }

                if (args[1].equalsIgnoreCase("verify")) {
                    if (args.length >= 3) {
                        p.sendMessage("DEBUG: As far as I know, this isn't working yet. :(");
                        //TODO Actually ban the player?
                        Bukkit.getBanList(Type.NAME).addBan(p.getName(), ChatColor.DARK_RED + "Invalid PMC Staff Identification Number!\nImpersonator confirmed.", new Date(Calendar.YEAR, Calendar.MONTH+1, Calendar.DAY_OF_MONTH), "Paril");
                        return true;
                    }

                    else if (args.length == 2){
                        p.sendMessage(ChatColor.RED + "Please specify a PMC Staff Identification Number.");
                        return true;
                    }
                }
            }
            return true;
        }
        return false;
    }

    public boolean mutePlayer(Player p) {
        getServer().dispatchCommand(getConsoleSender(), "mute " + p.getName());
        return true;
    }
}
