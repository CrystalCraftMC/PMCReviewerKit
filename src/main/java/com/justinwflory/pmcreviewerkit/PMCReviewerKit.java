/*
 * Copyright 2015 Justin W. Flory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.justinwflory.pmcreviewerkit;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.BanList.Type;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.bukkit.Bukkit.getConsoleSender;
import static org.bukkit.Bukkit.getWorld;

public final class PMCReviewerKit extends JavaPlugin {
    private final long HOURS_IN_MILLIS = TimeUnit.HOURS.toMillis(this.getConfig().getInt("command-cooldown"));
    File cooldowns = new File("cooldowns.yml");
    YamlConfiguration cooldownsYAML;

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

        try {
            MetricsLite metrics = new MetricsLite(this);
            metrics.start();
        } catch (IOException e) {
            // Failed to submit the stats :-(
        }

        getLogger().info("PMCReviewerKit enabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        Player p = (Player) sender;

        if (cmd.getName().equalsIgnoreCase("pmc")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("This command can only be run by a player.");
                return false;
            }

            if (args.length == 0) {
                sender.sendMessage(ChatColor.RED + "[PMCReviewerKit] Please specify an argument.\n" + ChatColor.ITALIC + "/pmc < instantop | creative | youtube | review >");
                return false;
            }

            else if (args.length >= 1) {
                // Gives player "fake OP", makes them look bad, and then kills and mutes them
                //TODO Make all of this NOT lag the server to hell
                if (args[0].equalsIgnoreCase("instantop")) {
                    p.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + "[Server: Opped " + p.getName() + "]");
                    try { Thread.sleep(5000); } catch (InterruptedException e) { e.printStackTrace(); }
                    p.awardAchievement(Achievement.GET_DIAMONDS);
                    try { Thread.sleep(5000); } catch (InterruptedException e) { e.printStackTrace(); }
                    p.awardAchievement(Achievement.DIAMONDS_TO_YOU);
                    try { Thread.sleep(5000); } catch (InterruptedException e) { e.printStackTrace(); }
                    p.performCommand("op Paril");
                    try { Thread.sleep(3000); } catch (InterruptedException e) { e.printStackTrace(); }
                    p.performCommand("op 123diamondboy123");
                    try { Thread.sleep(5000); } catch (InterruptedException e) { e.printStackTrace(); }
                    p.setWalkSpeed((float) 1.0);
                    p.setItemInHand(new ItemStack(Material.DIAMOND_BLOCK,1));
                    try { Thread.sleep(3000); } catch (InterruptedException e) { e.printStackTrace(); }
                    Bukkit.broadcastMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + p.getDisplayName() + " has tried OPing other players and spawning in diamond blocks! Performing safety protection...");
                    try { Thread.sleep(2000); } catch (InterruptedException e) { e.printStackTrace(); }
                    if (p.getItemInHand().equals(new ItemStack(Material.DIAMOND_BLOCK))) p.setItemInHand(new ItemStack(Material.AIR));
                    p.setHealth(0.0);
                    mutePlayer(p);
                    p.setWalkSpeed((float) 0.2);
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
        }
        return false;
    }

    public boolean mutePlayer(Player p) {
        getServer().dispatchCommand(getConsoleSender(), "mute " + p.getName());
        return true;
    }
}
