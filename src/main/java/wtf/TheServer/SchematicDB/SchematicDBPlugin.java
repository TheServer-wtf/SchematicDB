package wtf.TheServer.SchematicDB;

import com.google.common.primitives.Bytes;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import wtf.TheServer.TSCPlugin;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Level;

public class SchematicDBPlugin extends JavaPlugin {
    private DatabaseManager DB;
    private File WEFolder;
    private TSCPlugin central;
    private boolean active = false;
    public static final UUID CONSOLE = new UUID( 0 , 0 );
    private static SchematicDBPlugin instance;

    @Override
    public void onEnable() {
        Plugin WEPlugin = getServer().getPluginManager().getPlugin("WorldEdit");
        if(WEPlugin == null){
            getLogger().severe("WorldEdit not found, please install it to be able to use this plugin!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        Plugin TSCPlugin = getServer().getPluginManager().getPlugin("TSCentralPlugin");
        if(TSCPlugin == null){
            getLogger().severe("TSCentralPlugin not found, please install it to be able to use this plugin!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        instance = this;
        central = (TSCPlugin) TSCPlugin;
        WEFolder = WEPlugin.getDataFolder();
        saveDefaultConfig();
        DB = new DatabaseManager(
                getConfig().getString("mysql.host","localhost"),
                getConfig().getInt("mysql.port",3306),
                getConfig().getString("mysql.user","user"),
                getConfig().getString("mysql.password","password"),
                getConfig().getString("mysql.database","database")
        );
        DB.open();
        if(DB.getConnection() != null){
            active = true;
            getLogger().info("The plugin is now ready to use. Welcome, and have fun!");
            getCommand("schematicdb").setExecutor(new SchematicDBCommand(this));
        }
    }

    @Override
    public void onDisable() {
        if(DB != null)
            DB.close();
        if(active) {
            getLogger().info("The plugin is now disabled. Thanks for playing!");
        }
    }

    public TSCPlugin getCentral() {
        return central;
    }

    public static SchematicDBPlugin getInstance() {
        return instance;
    }

    public File getWEFolder() {
        return WEFolder;
    }

    public void uploadFile(@NotNull UUID user, @NotNull String schematic)
            throws NullPointerException, FileNotFoundException, IllegalArgumentException
    {
        if(DB == null || DB.getConnection() == null){
            throw new NullPointerException("Database Connection is not yet initialized");
        }
        if(schematic.length() > 80){
            throw new IllegalArgumentException("The schematic name length must be 80 characters or shorter");
        }
        String uploadSQL = "INSERT INTO `schematics` "
                + "(`name`,`user`,`created`,`edited`,`file`) "
                + "VALUES (?,?,?,?,?) "
                + "ON DUPLICATE KEY UPDATE `file`=VALUES(`file`),`edited`=VALUES(`edited`),`user`=VALUES(`user`)";
        File file = new File(WEFolder,"/schematics/"+schematic+".schem");
        FileInputStream input = new FileInputStream(file);
        try {
            if(DB.getConnection().isClosed()){
                DB.resetConnection();
                DB.open();
            }

            PreparedStatement pstmt = DB.getConnection().prepareStatement(uploadSQL);

            pstmt.setString(1, schematic);
            pstmt.setString(2, user.toString());
            pstmt.setLong(3, System.currentTimeMillis()/1000L);
            pstmt.setLong(4, file.lastModified()/1000L);
            pstmt.setBinaryStream(5, input);

            pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE,"Error during file upload:",e);
        }
    }

    public void downloadFile(@NotNull String schematic)
            throws NullPointerException, IllegalArgumentException, FileNotFoundException
    {
        if(schematic.length() > 80){
            throw new IllegalArgumentException("The schematic name length must be 80 characters or shorter");
        }

        SchematicFile schemFile = getFile(schematic);
        if(schemFile == null){
            throw new FileNotFoundException("The requested schematic is not found in the database");
        }
        File file = new File(WEFolder,"/schematics/"+schemFile.getName()+".schem");
        try {
            FileOutputStream output = new FileOutputStream(file);
            InputStream input = new ByteArrayInputStream(Bytes.toArray(schemFile.getFile()));
            byte[] buffer = new byte[1024];
            while (input.read(buffer) > 0) {
                output.write(buffer);
            }
            file.setLastModified(schemFile.lastEdited()*1000L);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE,"Error during file download:",e);
        }
    }

    @Nullable
    public SchematicFile getFile(@NotNull String schematic)
            throws NullPointerException, IllegalArgumentException
    {
        if(DB == null || DB.getConnection() == null){
            throw new NullPointerException("Database Connection is not yet initialized");
        }
        if(schematic.length() > 80){
            throw new IllegalArgumentException("The schematic name length must be 80 characters or shorter");
        }

        String selectSQL = "SELECT * FROM `schematics` WHERE `name`=?";
        ResultSet rs = null;
        SchematicFile file = null;

        try {
            if(DB.getConnection().isClosed()){
                DB.resetConnection();
                DB.open();
            }

            PreparedStatement pstmt = DB.getConnection().prepareStatement(selectSQL);

            pstmt.setString(1, schematic);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                ArrayList<Byte> bytes = new ArrayList<>();
                InputStream input = rs.getBinaryStream("file");
                byte[] buffer = new byte[1024];
                while (input.read(buffer) > 0) {
                    bytes.addAll(Bytes.asList(buffer));
                }
                file = new SchematicFile(
                        rs.getString("name"),
                        UUID.fromString(rs.getString("user")),
                        rs.getLong("created"),
                        rs.getLong("edited"),
                        bytes);
            }
        } catch (SQLException | IOException e) {
            getLogger().log(Level.SEVERE,"Error during file get:",e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException e) {
                getLogger().log(Level.SEVERE,"Error during file get:",e);
            }
        }
        return file;
    }

    public String formatTime(long unixSeconds){
        Date date = new java.util.Date(unixSeconds*1000L);
        SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MMM-dd HH:mm:ss z");
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        return sdf.format(date);
    }

}
