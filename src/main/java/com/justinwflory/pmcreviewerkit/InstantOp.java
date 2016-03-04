/*
 * Copyright (c) 2016 Justin W. Flory
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.justinwflory.pmcreviewerkit;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Achievement;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Created by jflory on 2/12/16.
 */
public class InstantOp extends BukkitRunnable implements Listener {

    // Attributes
    Player p;

    public InstantOp(Player p) {
        this.p = p;
    }

    public void run() {
        p.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + "[Server: Opped " + p.getName() + "]");
        p.awardAchievement(Achievement.GET_DIAMONDS);
        p.awardAchievement(Achievement.DIAMONDS_TO_YOU);
        p.performCommand("op Paril");
        p.performCommand("op 123diamondboy123");
        p.setWalkSpeed((float) 1.0);
        p.setItemInHand(new ItemStack(Material.DIAMOND_BLOCK, 1));
        Bukkit.broadcastMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + p.getDisplayName() + " has tried OPing other players and spawning in diamond blocks! Performing safety protection...");
        if (p.getItemInHand().equals(new ItemStack(Material.DIAMOND_BLOCK))) {
            p.setItemInHand(new ItemStack(Material.AIR));
            p.setHealth(0.0);
            //PMCReviewerKit.mutePlayer(p);
            p.setWalkSpeed((float) 0.2);
        }
        Bukkit.broadcastMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + p.getDisplayName() + " has tried OPing other players and spawning in diamond blocks! Performing safety protection...");
    }
}
