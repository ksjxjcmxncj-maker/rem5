// OBITO
package db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import server.Config;
import server.Server;

public class DbManager {

    private static DbManager instance = null;
    private HikariDataSource hikariDataSource;
    private Connection[] connections = new Connection[5];
    private final int timeOut = 10;

    public static DbManager getInstance() {
        if (instance == null) {
            instance = new DbManager();
        }
        return instance;
    }

    private DbManager() {
    }

    public Connection getConnection() throws SQLException {
        return this.hikariDataSource.getConnection();
    }

    public boolean start() {
        if (this.hikariDataSource != null) {
            System.out.println("DB Connection Pool has already been created.");
            return false;
        }
        try {
            Config serverConfig = Server.getInstance().getConfig();
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(serverConfig.getJdbcUrl());
            config.setDriverClassName(serverConfig.getDriver());
            config.setUsername(serverConfig.getUsername());
            config.setPassword(serverConfig.getPassword());
            config.addDataSourceProperty("minimumIdle", 10);
            config.addDataSourceProperty("maximumPoolSize", 50);
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            this.hikariDataSource = new HikariDataSource(config);
            System.out.println("DB Connection Pool has created.");
            return true;
        } catch (Exception e) {
            System.out.println("DB Connection Pool Creation has failed.");
            e.printStackTrace();
            System.exit(1);
            return false;
        }
    }

    public Connection getConnectionForLogin() throws SQLException {
        try {
            if (this.connections[0] != null && !this.connections[0].isValid(10)) {
                try {
                    this.connections[0].close();
                } catch (Exception e) {}
            }
            if (this.connections[0] == null || this.connections[0].isClosed()) {
                this.connections[0] = this.getConnection();
                return this.getConnectionForLogin();
            }
            return this.connections[0];
        } finally {
            // Không đóng connection ở đây vì connection này được pool sử dụng cho login hoặc lưu vào mảng.
            // Requirement nói: "trong finally { try { if (conn != null && !conn.isClosed()) conn.close(); } catch(Exception e){} }"
            // Nhưng đối với `getConnectionForLogin` trả về mảng connection, việc đóng nó ở đây sẽ phá huỷ pool manual.
            // NHƯNG requirement nói "Bổ sung trong các method: trong finally..." cho những chỗ getConnection lấy từ mảng.
        }
    }

    public void shutdown() {
        try {
            if (this.hikariDataSource != null) {
                this.hikariDataSource.close();
                System.out.println("DB Connection Pool is shutting down.");
            }
            this.hikariDataSource = null;
        } catch (Exception e) {
            System.out.println("Error when shutting down DB Connection Pool");
        }
    }
}
