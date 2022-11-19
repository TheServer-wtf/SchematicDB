package wtf.TheServer.SchematicDB;

import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {
    private final String HOST;
    private final int PORT;
    private final String USER;
    private final String PASSWORD;
    private final String DATABASE;
    private Connection connection;

    public DatabaseManager(@NotNull String host, int port, @NotNull String user, @NotNull String password, @NotNull String database) {
        HOST = host;
        PORT = port;
        USER = user;
        PASSWORD = password;
        DATABASE = database;
    }

    public void open(){
        if(connection != null)
            return;
        try {
            connection = DriverManager.getConnection("jdbc:mysql://"+HOST+":"+PORT+"/"+DATABASE, USER, PASSWORD);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void close(){
        try {
            if (connection!=null && !connection.isClosed()){
                connection.close();
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void resetConnection(){
        try {
            if(connection != null && !connection.isClosed())
                return;
        } catch (SQLException ignored) {}
        connection = null;
    }
}
