package wtf.TheServer.SchematicDB;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SchematicDBCommand implements TabExecutor {
    private final SchematicDBPlugin plugin;

    public SchematicDBCommand(SchematicDBPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(!sender.hasPermission("schematicdb.use"))
            return true;
        UUID user = (sender instanceof Player) ? ((Player) sender).getUniqueId() : SchematicDBPlugin.CONSOLE;
        if(args.length == 0) {
            sendHelp(sender,label);
        } else {
            switch (args[0]) {
                default -> sendHelp(sender, label);
                case "info" -> {
                    if(args.length < 2){
                        sender.sendMessage("§eUsage: /"+label+" "+args[0]+" <fileName>");
                        break;
                    }
                    sender.sendMessage("§eRetrieving file information...");
                    plugin.getServer().getScheduler().runTaskAsynchronously(plugin,()-> {
                        try {
                            SchematicFile file = plugin.getFile(args[1]);
                            if (file == null) {
                                sender.sendMessage(plugin.getCentral().getSystemPrefix() + " §cFile not found.");
                                return;
                            }
                            String info = plugin.getConfig().getString("text.info");
                            String editor = file.getUser().equals(SchematicDBPlugin.CONSOLE) ? "CONSOLE" : plugin.getServer().getOfflinePlayer(file.getUser()).getName();
                            File schem = new File(plugin.getWEFolder(),"/schematics/"+args[1]+".schem");
                            long edit = 0;
                            String status = plugin.getConfig().getString("text.status.notondisk");
                            if(schem.exists()) {
                                edit = schem.lastModified() / 1000L;
                                if(file.lastEdited() > edit){
                                    status = plugin.getConfig().getString("text.status.updated");
                                } else {
                                    status = plugin.getConfig().getString("text.status.uptodate");
                                }
                            }
                            info = MessageFormat.format(info != null ? info : "", file.getName(), editor, plugin.formatTime(file.createTime()), plugin.formatTime(file.lastEdited()), status);
                            info = ChatColor.translateAlternateColorCodes('&', plugin.getCentral().hexColor("&#", "", info));
                            if (!info.isEmpty())
                                sender.sendMessage(info);
                        } catch (Exception e) {
                            sender.sendMessage(plugin.getCentral().getSystemPrefix() + " §c" + e.getMessage());
                        }
                    });
                }
                case "upload" -> {
                    if(args.length < 2){
                        sender.sendMessage("§eUsage: /"+label+" "+args[0]+" <fileName>");
                        break;
                    }
                    sender.sendMessage("§eAttempting file upload...");
                    plugin.getServer().getScheduler().runTaskAsynchronously(plugin,()->{
                        try {
                            plugin.uploadFile(user,args[1]);
                            sender.sendMessage("§aFile upload finished.");
                        } catch (FileNotFoundException e) {
                            sender.sendMessage("§cFile not found.");
                        } catch (NullPointerException | IllegalArgumentException e){
                            sender.sendMessage("§c"+e.getMessage());
                        }
                    });
                }
                case "download" -> {
                    if(args.length < 2){
                        sender.sendMessage("§eUsage: /"+label+" "+args[0]+" <fileName>");
                        break;
                    }
                    sender.sendMessage("§eAttempting file download...");
                    plugin.getServer().getScheduler().runTaskAsynchronously(plugin,()->{
                        try {
                            plugin.downloadFile(args[1]);
                            sender.sendMessage("§aFile download finished.");
                        } catch (FileNotFoundException e) {
                            sender.sendMessage("§cFile not found.");
                        } catch (NullPointerException | IllegalArgumentException e){
                            sender.sendMessage("§c"+e.getMessage());
                        }
                    });
                }
                case "reload" -> {
                    plugin.reloadConfig();
                    sender.sendMessage("Plugin configuration reloaded.");
                }
            }
        }
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        ArrayList<String> help = new ArrayList<>();
        if(sender.hasPermission("schematicdb.use")) {
            if(args.length < 2){
                help.add("upload");
                help.add("download");
                help.add("info");
                help.add("reload");
            } else {
                if(!args[0].equalsIgnoreCase("reload")){
                    final File folder = new File(plugin.getWEFolder(), "/schematics/");
                    File[] list = folder.listFiles((dir, name) -> name.endsWith(".schem"));
                    for(File f : list != null ? list : new File[0]){
                        String name = f.getName();
                        help.add(name.substring(0,name.length()-6));
                    }
                }
            }
        }
        return help;//me
    }

    public void sendHelp(@NotNull CommandSender sender, @NotNull String label){
        sender.sendMessage(
                plugin.getCentral().getSystemPrefix()+"§6Schematic§dDB §eplugin v" + plugin.getDescription().getVersion(),
                "§7- §f/" + label + " §7- Display this list",
                "§7- §f/" + label + " upload <fileName> §7- Upload the given file to the database",
                "§7- §f/" + label + " download <fileName> §7- Download the requested file from the database",
                "§7- §f/" + label + " info <fileName> §7- Get information on the requested file from the database",
                "§7- §f/" + label + " reload §7- Reload plugin configuration",
                "§cPlease note: reloading plugin configuration doesn't update database connection settings," +
                        " you have to restart the server for the db settings to update.");
    }
}
