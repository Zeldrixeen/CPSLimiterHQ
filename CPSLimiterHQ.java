package com.cpslimiterhq;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class CPSLimiterHQ extends JavaPlugin implements Listener {

    private final Map<UUID, List<Long>> clickTimes = new HashMap<>();
    private final Set<UUID> penalizedPlayers = new HashSet<>();

    private int cpsLimit;
    private int penaltySeconds;
    private String warningMessage;
    private String restoreMessage;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfiguration();

        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("CPSLimiterHQ activado.");
    }

    private void loadConfiguration() {
        FileConfiguration config = getConfig();
        cpsLimit = config.getInt("cps-limit", 15);
        penaltySeconds = config.getInt("penalty-seconds", 5);
        warningMessage = ChatColor.translateAlternateColorCodes('&', config.getString("messages.warning", "&c⚠️ Estás haciendo demasiados clicks. PvP deshabilitado por %time% segundos."));
        restoreMessage = ChatColor.translateAlternateColorCodes('&', config.getString("messages.restore", "&a✅ Puedes volver a hacer PvP."));
    }

    @Override
    public void onDisable() {
        getLogger().info("CPSLimiterHQ desactivado.");
    }

    @EventHandler
    public void onClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        clickTimes.putIfAbsent(uuid, new ArrayList<>());
        List<Long> times = clickTimes.get(uuid);

        times.removeIf(time -> (now - time) > 1000);
        times.add(now);

        if (times.size() > cpsLimit && !penalizedPlayers.contains(uuid)) {
            penalizePlayer(player);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;

        Player damager = (Player) event.getDamager();
        if (penalizedPlayers.contains(damager.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    private void penalizePlayer(Player player) {
        UUID uuid = player.getUniqueId();
        penalizedPlayers.add(uuid);

        String message = warningMessage.replace("%time%", String.valueOf(penaltySeconds));
        player.sendMessage(message);

        new BukkitRunnable() {
            @Override
            public void run() {
                penalizedPlayers.remove(uuid);
                player.sendMessage(restoreMessage);
            }
        }.runTaskLater(this, penaltySeconds * 20L); // 20 ticks = 1 segundo
    }

    // ✅ Manejo del comando /cpshq reload
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("cpshq")) {
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                this.reloadConfig();
                this.loadConfiguration();
                sender.sendMessage(ChatColor.GREEN + "✔ Configuración recargada correctamente.");
                return true;
            } else {
                sender.sendMessage(ChatColor.RED + "Uso: /cpshq reload");
                return true;
            }
        }
        return false;
    }
}