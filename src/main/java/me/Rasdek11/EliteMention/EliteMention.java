package me.Rasdek11.EliteMention;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;


import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;


public class EliteMention extends JavaPlugin {

    private  final Logger log = Logger.getLogger("Minecraft");

    private  boolean tagEnabled;
    private  String tag;

    private  boolean soundEnabled;
    private  Sound sound;
    private  Float volume;
    private  Float pitch;

    private  boolean colorEnabled;
    private  ChatColor mentionColor;
    private  ChatColor afterMentionColor;

    private long cooldown;

    private  boolean groupsEnabled;

    private Set<UUID> toggle = new HashSet<UUID>();
    private File playersFile = new File(this.getDataFolder(), "players.yml");
    private FileConfiguration players = YamlConfiguration.loadConfiguration(playersFile);
    private File messagesFile = new File(this.getDataFolder(), "messages.yml");
    private FileConfiguration messages = YamlConfiguration.loadConfiguration(messagesFile);
    private HashMap<String, Long> cooldowns = new HashMap<String, Long>();
    private HashMap<String, ArrayList> muted = new HashMap<String, ArrayList>();



    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(new ChatListener(), this);
        saveDefaultConfig();
        saveDefaultMessagesConfig();
        if (players != null) {
            restoreMuted();
        }
        savePlayersConfig();
        groupsEnabled = getConfig().getBoolean("groupsEnabled");
        if (!groupsEnabled) {
            configLoad();
        }

        getCommand("elitemention").setTabCompleter(new TabCompletion());
    }

    @Override
    public void onDisable() {
        if(!muted.isEmpty())
            saveMuted();

        log.info(String.format("[%s] Disabled Version %s", getDescription().getName(), getDescription().getVersion()));
    }

    public class ChatListener implements Listener {
        @EventHandler
        private void onPlayerChat(AsyncPlayerChatEvent event) {
            for (Player target : Bukkit.getServer().getOnlinePlayers()) {
                if ((tagEnabled && event.getMessage().contains(tag + target.getName()))
                        || (!tagEnabled && event.getMessage().contains(target.getName()))) {
                    if (event.getPlayer().hasPermission("elitemention.mention"))
                        event.setMessage(mention(event.getPlayer(), target, event.getMessage(), target.getName()));
                }
            }
            if (event.getMessage().contains(getConfig().getString("everyone.tag"))
                    && getConfig().getBoolean("everyone.enabled")
                    && event.getPlayer().hasPermission("elitemention.everyone")) {
                for (Player target : Bukkit.getServer().getOnlinePlayers()) {
                    event.setMessage(mention(event.getPlayer(), target, event.getMessage(),
                            getConfig().getString("everyone.tag")));
                }
            }
            for (String group : getConfig().getStringList("customGroups.groups")) {
                if (event.getMessage().contains(getConfig().getString("customGroups.tag") + group)
                        && getConfig().getBoolean("customGroups.enabled")
                        && event.getPlayer().hasPermission("elitemention.mentioncustomgroup")) {
                    for (Player target : Bukkit.getServer().getOnlinePlayers()) {
                        if (target.hasPermission("elitemention.customgroup." + group)){
                            for (String name : getConfig().getStringList("customGroups.groups")) {
                                permissionInitiate("elitemention.customgroup." + name, "FALSE");
                            }
                        event.setMessage(mention(event.getPlayer(), target, event.getMessage(),
                                getConfig().getString("customGroups.tag") + group));
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("elitemention") || label.equalsIgnoreCase("em")) {
            if (args.length == 0 ) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "         &f---&b&l=====[&r&eElite&fMention&b&l]=====&r&f---"));
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&b - &lGet attention of players by mentioning them"));
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&b - Usage: &e/elitemention help"));
                return true;
            }

            if(args[0].equalsIgnoreCase("help")){
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "[&r&eElite&fMention&b&l]&r &bHelp Menu"));
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&b - /elitemention help&f: " + msg("helpHelp")));
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&b - /elitemention mute [Player]&f: " + msg("helpMute")));
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&b - /elitemention toggle&f: " + msg("helpToggle")));
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&b - /elitemention reload&f: " + msg("helpReload")));
            }

            if (args[0].equalsIgnoreCase("reload")){
                if (sender.hasPermission("elitemention.reload") || sender.isOp()){
                    reloadConfig();
                    reloadMessagesConfig();
                    groupsEnabled = getConfig().getBoolean("groupsEnabled");
                    if (!groupsEnabled) {
                    configLoad();
                    }
                    sender.sendMessage(msg("reloadMessage"));
                } else sender.sendMessage(msg("noPermission"));
                return true;
            }

            if (args[0].equalsIgnoreCase("toggle")) {
                if(!(sender instanceof Player)){
                    //the sender is not a Player
                    sender.sendMessage(msg("playerCmd"));
                } else {
                    if (!sender.hasPermission("elitemention.toggle")) {
                        sender.sendMessage(msg("noPermission"));
                    } else {
                        if (getConfig().getBoolean("toggle.enabled")) {
                            UUID id;
                            if (Bukkit.getServer().getOnlinePlayers().contains(args[1])
                                    && sender.hasPermission("elitemention.toggleothers")){
                                id = Bukkit.getServer().getPlayer(args[1]).getUniqueId();
                            }
                            else id = ((Player) sender).getPlayer().getUniqueId();
                            toggleSwitch(id);
                            if (toggle.contains(id)) {
                                sender.sendMessage(msg("toggleOff"));
                            } else sender.sendMessage(msg("toggleOn"));

                        } else sender.sendMessage(msg("notEnabled"));
                    }
                }
            }
            if (args.length == 1 && args[0].equalsIgnoreCase("mute")) {
            sender.sendMessage(msg("muteUsage"));
            }

            if (args.length == 2 && args[0].equalsIgnoreCase("mute")) {
                if(!(sender instanceof Player)){
                    //the sender is not a Player
                    sender.sendMessage(msg("playerCmd"));
                } else {
                    if (!sender.hasPermission("elitemention.mute")) {
                        sender.sendMessage(msg("noPermission"));
                    } else {
                        if (getConfig().getBoolean("mute")) {
                            playerMute(((Player) sender).getPlayer(), args[1]);
                        } else sender.sendMessage(msg("notEnabled"));
                    }
                }
            }
        }
        return false;
    }

    private String mention(Player player, Player target, String message, String string) {
        UUID targetID = target.getUniqueId();
        if(cooldowns.get(player.getUniqueId().toString()) == null)
            cooldowns.put(player.getUniqueId().toString(), System.currentTimeMillis());
        if (cooldowns.get(player.getUniqueId().toString()) <= System.currentTimeMillis()
                || player.hasPermission("elitemention.cooldownbypass")){
            ArrayList list = new ArrayList();
            if (muted.get(targetID.toString()) == null)
                list = muted.get(targetID.toString());

            list = muted.get(targetID.toString());
            if(!list.contains(player.getName())){
                if (groupsEnabled) {
                    getGroup(player);
                }
                if (soundEnabled) {
                    if (toggle.contains(targetID) && getConfig().getBoolean("toggle.sound")) {
                    } else
                        target.playSound(target.getLocation(), sound, volume, pitch);
                }
                if (colorEnabled) {
                    if (toggle.contains(targetID) && getConfig().getBoolean("toggle.mentionColor")) {
                    } else {

                        message = message.replace(string,
                                mentionColor + string + afterMentionColor);
                    }
                }
                cooldowns.replace(player.getUniqueId().toString(), System.currentTimeMillis() + (cooldown * 1000));
                return message;
            }
        } else {
            long timeLeft = (cooldowns.get(player.getUniqueId().toString()) - System.currentTimeMillis()) / 1000;
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    String.format(msg("onCooldown"), timeLeft)));
        }
        return message;
    }

    private void configLoad() {
        tagEnabled = getConfig().getBoolean("tag.tagEnabled");
        tag = getConfig().getString("tag.tag");

        soundEnabled = getConfig().getBoolean("sound.enabled");
        sound = Sound.valueOf(getConfig().getString("sound.effect"));
        volume = Float.parseFloat(getConfig().getString("sound.volume"));
        pitch = Float.parseFloat(getConfig().getString("sound.pitch"));

        colorEnabled = getConfig().getBoolean("mentionColor.enabled");
        mentionColor = ChatColor.valueOf(getConfig().getString("mentionColor.mention"));
        afterMentionColor = ChatColor.valueOf(getConfig().getString("mentionColor.after"));

        cooldown = Long.parseLong(getConfig().getString("cooldown"));
    }

    private void getGroup(Player player){
        for(String key : getConfig().getConfigurationSection("groups").getKeys(false)) {
            permissionInitiate("elitemention.group." + key, "FALSE");

            if (player.hasPermission("elitemention.group." + key)) {
                soundEnabled = getConfig().getBoolean("groups." + key + ".sound.enabled");
                sound = Sound.valueOf(getConfig().getString("groups." + key + ".sound.effect"));
                volume = Float.parseFloat(getConfig().getString("groups." + key + ".sound.volume"));
                pitch = Float.parseFloat(getConfig().getString("groups." + key + ".sound.pitch"));

                colorEnabled = getConfig().getBoolean("groups." + key + ".mentionColor.enabled");
                mentionColor = ChatColor.valueOf(getConfig().getString("groups." + key + ".mentionColor.mention"));
                afterMentionColor = ChatColor.valueOf(getConfig().getString("groups." + key + ".mentionColor.after"));

                cooldown = Long.parseLong(getConfig().getString("groups." + key + ".cooldown"));
            }
        }
    }

    private void permissionInitiate(String name, String defaultValue){
        try {
            // Create and register permission
            Permission permission = new Permission(name, PermissionDefault.valueOf(defaultValue));
            getServer().getPluginManager().addPermission(permission);
        } catch (IllegalArgumentException e) {
            // Permission already exists, ensure it has the correct PermissionDefault instead
            getServer().getPluginManager().getPermission(name)
                    .setDefault(PermissionDefault.valueOf(defaultValue));
        }
    }

    private void toggleSwitch(UUID id) {
        if (toggle.contains(id)) {
            //The users uuid is in the set, so remove them to toggle 'off'.
            toggle.remove(id);
        } else {
            //The users uuid is not the set, so add them to toggle 'on'.
            toggle.add(id);
        }
    }

    private void playerMute(Player player, String target){
        ArrayList list = new ArrayList();
        if(!Bukkit.getServer().getPlayer(target).hasPermission("elitemention.unmutable")){
            if (muted.get(player.getUniqueId().toString()) == null) {
                list.add(target);
                muted.put(player.getUniqueId().toString(), list);
            }
            else if (muted.get(player.getUniqueId().toString()) != null) {
                list = muted.get(player.getUniqueId().toString());
                if (list.contains(target)) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            String.format(msg("unmuted"), target)));
                    list.remove(target);
                    muted.replace(player.getUniqueId().toString(), list);
                } else if (!list.contains(target)) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            String.format(msg("muted"), target)));
                    list.add(target);
                    muted.replace(player.getUniqueId().toString(), list);
                }
            }
        } else
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    String.format(msg("cantMute"), target)));
    }

    private String msg(String message){
        return ChatColor.translateAlternateColorCodes('&',
                getMessagesConfig().getString("prefix") + " " + getMessagesConfig().getString(message));
    }

    private void saveMuted(){
        for (Map.Entry<String, ArrayList> entry : muted.entrySet()){
            players.set(entry.getKey(), entry.getValue());
        }
        savePlayersConfig();
    }

    private void restoreMuted(){
        players.getRoot().getKeys(false).forEach(key -> {
            ArrayList mutedData = ((ArrayList) getPlayersConfig().get(key));
            muted.put(key, mutedData);
        });
    }

    private void savePlayersConfig(){
        try{
            players.save(playersFile);
        }catch(IOException ex){
            ex.printStackTrace();
        }
    }

    private void reloadMessagesConfig(){
        if(messagesFile == null){
            messagesFile = new File(getDataFolder(), "players.yml");
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        messages.set("Enabled", true);
    }

    private FileConfiguration getPlayersConfig() {
        if (players == null) {
            savePlayersConfig();
        }
        return players;
    }

    private FileConfiguration getMessagesConfig() {
        if (messages == null) {
            savePlayersConfig();
        }
        return messages;
    }

    private void saveDefaultMessagesConfig() {
        if (!messagesFile.exists()) {
            saveResource("messages.yml", true);
            messagesFile.getParentFile().mkdirs();
            try {
                messagesFile.createNewFile();
                Bukkit.getConsoleSender().sendMessage("Creating messages.yml file...");
            } catch (IOException e) {
                System.err.println("Cannot create messages.yml configuration file.");
            }
        }
    }
}
