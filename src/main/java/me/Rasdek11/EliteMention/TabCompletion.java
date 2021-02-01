package me.Rasdek11.EliteMention;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class TabCompletion implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args){

        if(args.length == 1){
            List<String> arguments = new ArrayList<String>();
            arguments.add("help");
            arguments.add("mute");
            arguments.add("reload");
            arguments.add("toggle");

            return arguments;
        }

        if (args.length == 2 && args[0] == "mute"){
            List<String> playerList = new ArrayList<String>();
            Player[] players = new Player[Bukkit.getServer().getOnlinePlayers().size()];
            Bukkit.getServer().getOnlinePlayers().toArray(players);
            for (int i = 0; i < players.length; i++){
                playerList.add(players[i].getName());
            }

            return playerList;
        }

        return null;
    }
}
