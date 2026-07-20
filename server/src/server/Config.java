// OBITO
package server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class Config {

    private int listen;   // FIX: short → int (short overflow trên port ≥ 32768)
    private String driver;
    private String username;
    private String password;
    private String dbName;
    private String host;
    private int port;     // FIX: short → int
    private short testmode;
    private int secondWaitLogin;

    public Config(String path) {
        // FIX: bắt cả NumberFormatException, không chỉ IOException
        try (FileInputStream input = new FileInputStream(new File(path))) {
            System.out.println("load config");
            Properties props = new Properties();
            props.load(new InputStreamReader((InputStream) input, StandardCharsets.UTF_8));
            // FIX: lọc password khỏi log
            props.forEach((t, u) -> {
                String key = t.toString();
                String val = key.contains("password") || key.contains("secret") ? "***" : u.toString();
                System.out.println(String.format("Config - %s: %s", key, val));
            });
            this.listen = Integer.parseInt(props.getProperty("server.port"));
            this.driver = props.getProperty("db.driver");
            this.username = props.getProperty("db.user");
            this.password = props.getProperty("db.password");
            this.dbName = props.getProperty("db.name");
            this.host = props.getProperty("db.host");
            this.port = Integer.parseInt(props.getProperty("db.port"));
            this.testmode = Short.parseShort(props.getProperty("admin.mode"));
            this.secondWaitLogin = props.containsKey("wait.login")
                    ? Integer.parseInt(props.getProperty("wait.login")) : 5;
        } catch (IOException ex) {
            System.err.println("[Config] Lỗi đọc file config: " + ex.getMessage());
            ex.printStackTrace();
        } catch (NumberFormatException ex) {
            // FIX: bắt lỗi parse số (trước đây bị bỏ qua hoàn toàn)
            System.err.println("[Config] Giá trị không hợp lệ trong server.ini: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public String getJdbcUrl() {
        return "jdbc:mysql://" + this.host + ":" + this.port + "/" + this.dbName;
    }

    public int getListen() { return this.listen; }
    public String getDriver() { return this.driver; }
    public String getUsername() { return this.username; }
    public String getPassword() { return this.password; }
    public String getDbName() { return this.dbName; }
    public String getHost() { return this.host; }
    public int getPort() { return this.port; }
    public short getTestmode() { return this.testmode; }
    public int getSecondWaitLogin() { return this.secondWaitLogin; }

    public void setListen(int listen) { this.listen = listen; }
    public void setDriver(String driver) { this.driver = driver; }
    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
    public void setDbName(String dbName) { this.dbName = dbName; }
    public void setHost(String host) { this.host = host; }
    public void setPort(int port) { this.port = port; }
    public void setTestmode(short testmode) { this.testmode = testmode; }
    public void setSecondWaitLogin(int secondWaitLogin) { this.secondWaitLogin = secondWaitLogin; }
}
