package Utils;

import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.SQLException;
public class Mydatabase {
    Connection con;
    public static Mydatabase instance;
    private final String url;
    private final String user;
    private final String password;
    public Mydatabase() {
        try{
            this.url = System.getProperty("db.url", "jdbc:mysql://localhost:3306/careerlink");
            this.user = System.getProperty("db.user", "root");
            this.password = System.getProperty("db.password", "");
            con = DriverManager.getConnection(this.url, this.user, this.password);
            System.out.println("Connected to the database");
        }catch(SQLException e){
            throw new RuntimeException(e);
        }
    }
    public static Mydatabase getInstance(){
        if(instance == null){
            instance = new Mydatabase();
        }
        return instance;
    }
    public static synchronized void reset() {
        if (instance != null) {
            try {
                if (instance.con != null && !instance.con.isClosed()) {
                    instance.con.close();
                }
            } catch (SQLException ignored) {
            }
            instance = null;
        }
    }
    public Connection getConnection(){
        try {
            if (con == null || con.isClosed()) {
                con = DriverManager.getConnection(url, user, password);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return con;
    }
}
