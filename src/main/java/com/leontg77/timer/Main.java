/*
 * Project: Timer
 * Class: com.leontg77.timer.Main
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2016-2018 Leon Vaktskjold <leontg77@gmail.com>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.leontg77.timer;

import com.leontg77.timer.commands.StopwatchCommands;
import com.leontg77.timer.commands.TimerCommand;
import com.leontg77.timer.handling.Actionbar;
import com.leontg77.timer.handling.PacketSender;
import com.leontg77.timer.handling.handlers.ActionbarHandler;
import com.leontg77.timer.handling.handlers.BossBarHandler;
import com.leontg77.timer.handling.handlers.NewActionBarHandler;
import com.leontg77.timer.handling.handlers.OldActionBarHandler;
import com.leontg77.timer.runnable.TimerRunnable;
import com.leontg77.timer.util.Util;
import org.apache.commons.lang.time.StopWatch;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Main class of the plugin.
 *
 * @author LeonTG
 */
public class Main extends JavaPlugin {
    public static final String PREFIX = "§cTimer §8» §7";
    private boolean isRunning, isPaused;
    private StopWatch stopWatch;
    private File configFile;
    private YamlConfiguration config;
    private String timeMessage;
    private int offsetSec;
    private int endSec = Integer.MAX_VALUE;
    private Actionbar actionbar;
    private UUID curPlayer;
    private TimerRunnable runnable = null;

    public static File getConfig(final String name, final Plugin plugin) {
        final File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        final File configFile = new File(plugin.getDataFolder() + File.separator + name + ".yml");
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return configFile;
    }

    @Override
    public void onEnable() {
        reloadConfig();
        getCommand("timer").setExecutor(new TimerCommand(this));
        getCommand("stopwatch").setExecutor(new StopwatchCommands(this));
        if (!setupActionbar()) {
            getLogger().severe("Failed to setup Actionbar!");
            getLogger().severe("Your server version is not compatible with this plugin!");
            Bukkit.getPluginManager().disablePlugin(this);
        }
        this.configFile = getConfig("config", this);
        this.reload();
        if (!this.config.isSet("stopwatchMessage")) {
            this.config.set("stopwatchMessage", "&6{0}");
            this.save();
        }
        timeMessage = this.config.getString("stopwatchMessage");
        stopWatch = new StopWatch();

        isRunning = false;
        curPlayer = null;
        isPaused = false;
        Bukkit.getPluginManager().registerEvents(new ItemListener(this), this);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (isRunning) {

                    int totalSecs = (int) (stopWatch.getTime() / 1000.0) + offsetSec;
                    int hours = (totalSecs / 3600);
                    int minutes = ((totalSecs % 3600) / 60);
                    int seconds = totalSecs % 60;
                    String time;
                    if (hours > 0) {
                        time = String.format("%01d:%02d:%02d", hours, minutes, seconds);
                    } else {
                        time = String.format("%01d:%02d", minutes, seconds);
                    }
                    String timerMessage = Util.replace(timeMessage, time);
                    //System.out.println(timerMessage);
                    if (curPlayer != null) {
                        if (Bukkit.getOfflinePlayer(curPlayer).isOnline()) {
                            if (Bukkit.getPlayer(curPlayer).hasPermission("stopwatch.default")) {

                                actionbar.sendActionbar(Bukkit.getPlayer(curPlayer), timerMessage);
                            }
                        } else {
                            for (Player p : Bukkit.getOnlinePlayers()) {
                                if (p.hasPermission("stopwatch.default")) {
                                    actionbar.sendActionbar(p, timerMessage);
                                }
                            }
                        }
                    } else {
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            if (p.hasPermission("stopwatch.default")) {
                                actionbar.sendActionbar(p, timerMessage);
                            }
                        }
                    }
                    if (totalSecs >= endSec) {
                        stopStopwatch(Bukkit.getConsoleSender());
                    }
                }
            }
        }.runTaskTimer(this, 1L, 1L);
    }

    public void showHelp(CommandSender s) {
        s.sendMessage(ChatColor.GREEN + "/st pause will either pause or unpause the timer\n" +
                "/st start will start the timer and restart if already started\n" +
                "/st stop will stop the timer\n" +
                "/st showall will show the timer message to everyone (default)\n" +
                "/st show [player] will show the timer to the chosen player to show all do st showall\n" +
                "/st item will give you items to use the 3 time control commands\n" +
                "/st message [message] will set the message of the time, make sure to include {0} in it which represents time\n" +
                "/st offset [seconds] will add to the offset seconds by the amount given until next stopwatch reset\n" +
                "/st endsec [seconds] will set the end seconds to the amount given so that the timer will stop at that time, must be set after every reset");
    }

    public void startStopwatch() {
        stopWatch.reset();
        offsetSec = 0;
        endSec = Integer.MAX_VALUE;
        //System.out.println("started");
        stopWatch.start();
        isRunning = true;
        isPaused = false;
    }

    public void showAllStopwatch(CommandSender s) {
        curPlayer = null;
        s.sendMessage(ChatColor.GREEN + "Timer message is now showing to all players!");
    }

    public void showStopwatch(CommandSender s, String[] args) {
        if (args.length == 1) {
            s.sendMessage(ChatColor.RED + "Specify a player, like this /st show [playername]");
            return;
        }
        String playername = args[1];
        Player player = Bukkit.getPlayer(playername);
        if (player == null) {
            s.sendMessage(ChatColor.RED + "Unknown player.");
            return;
        }
        curPlayer = player.getUniqueId();
        s.sendMessage(ChatColor.GREEN + "Timer is now showing to " + player.getName());
    }

    public void stopStopwatch(CommandSender s) {
        if (!isRunning) {
            s.sendMessage(ChatColor.RED + "Stopwatch already stopped!");

        } else {
            // System.out.println("stopped");
            stopWatch.stop();
            isRunning = false;
            isPaused = false;
        }
    }

    public void pauseStopwatch(CommandSender s) {
        if (!isRunning) {
            s.sendMessage(ChatColor.RED + "The timer isn't running at the moment!");
        } else {
            if (isPaused) {
                stopWatch.resume();
                //System.out.println("un paused");
            } else {
                stopWatch.suspend();
                //System.out.println("paused");
            }
            isPaused = !isPaused;
        }
    }

    public void getItems(CommandSender s) {
        if (!(s instanceof Player)) {
            s.sendMessage(ChatColor.RED + "Only players can use items!");
            return;
        }
        Player p = (Player) s;
        //todo: avoid insert inventory
        p.getInventory().setItem(0, ItemListener.getStartItem());
        p.getInventory().setItem(1, ItemListener.getStopItem());
        p.getInventory().setItem(2, ItemListener.getPauseItem());

    }

    //Config methods

    public void setMessage(CommandSender s, String[] args) {
        if (args.length == 1) {
            s.sendMessage(ChatColor.RED + "Specify a message, like this /st message &6Time: {0}");
            return;
        }

        String message = args[1];

        if (args.length > 2) {
            for (int i = 2; i < args.length; i++) {
                message += " " + args[i];
            }
        }

        if (!Util.isPlaceholderInMessage(message)) {
            s.sendMessage(ChatColor.RED + "The message " + message + " must contain {0}!");
            return;
        }
        timeMessage = message;
        this.config.set("stopwatchMessage", message);
        s.sendMessage("Timer message set to " + Util.replace(Util.colour(timeMessage), "20"));
        this.save();
    }

    public void offsetSeconds(CommandSender s, String[] args) {
        if (args.length == 1) {
            s.sendMessage(ChatColor.RED + "Specify offset seconds like 40 or -50, the offset seconds go back to 0 once timer is reset, current offset is " + offsetSec);
            return;
        }
        try {
            int offsetArg = Integer.parseInt(args[1]);
            offsetSec += offsetArg;
            s.sendMessage(ChatColor.GREEN + "Successfully offset by " + offsetArg + " current offset now is " + offsetSec);
        } catch (IllegalArgumentException e) {
            s.sendMessage(ChatColor.RED + "Please input an integer");

        }
    }

    public void setEndSec(CommandSender s, String[] args) {
        if (args.length == 1) {
            s.sendMessage(ChatColor.RED + "Specify end seconds like 40, current end seconds are " + endSec);
            return;
        }
        try {
            int endSec = Integer.parseInt(args[1]);
            s.sendMessage(ChatColor.GREEN + "Successfully set end seconds to " + endSec);
        } catch (IllegalArgumentException e) {
            s.sendMessage(ChatColor.RED + "Please input an integer");
        }

    }

    /**
     * Get the current runnable for the timer.
     *
     * @return The current runnable.
     */
    public TimerRunnable getRunnable() {
        return runnable;
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();

        if (getConfig().getConfigurationSection("bossbar") == null) {
            getConfig().set("bossbar.enabled", true);
            getConfig().set("bossbar.color", "pink");
            getConfig().set("bossbar.style", "solid");
            saveConfig();
        }

        if (runnable != null && runnable.getHandler() instanceof Listener) {
            HandlerList.unregisterAll((Listener) runnable.getHandler());
        }

        try {
            PacketSender packetSender = new PacketSender();
            FileConfiguration config = getConfig();

            if (config.getBoolean("bossbar.enabled")) {
                try {
                    runnable = new TimerRunnable(this, new BossBarHandler(this, config.getString("bossbar.color", "pink"), config.getString("bossbar.style", "solid")));
                    return;
                } catch (Exception ignored) {
                }

                getLogger().warning("BossBars are not supported in pre Minecraft 1.9, defaulting to action bar.");
            }

            try {
                runnable = new TimerRunnable(this, new NewActionBarHandler(packetSender));
            } catch (Exception ex) {
                runnable = new TimerRunnable(this, new OldActionBarHandler(packetSender));
            }
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, "Failed to setup action timer plugin, are you using Minecraft 1.8 or higher?", ex);
            setEnabled(false);
        }
    }

    public void reload() {
        this.config = YamlConfiguration.loadConfiguration(this.configFile);
    }

    public void save() {
        try {
            this.config.save(this.configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean setupActionbar() {
        String version;
        try {
            version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];

        } catch (ArrayIndexOutOfBoundsException whatVersionAreYouUsingException) {
            return false;
        }
        getLogger().info("Your server is running version " + version);
        //we are running 1.10+ where you can use ChatMessageType
        actionbar = new ActionbarHandler();
        return true;
    }
}
