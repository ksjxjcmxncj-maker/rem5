package nro.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

/**
 *
 * @author 💖 Obito - Đâu Phải Tuấn 💖
 * @copyright 💖 GirlkuN 💖
 *
 */
public class DBService {

    public static String DRIVER = "com.mysql.cj.jdbc.Driver";
    public static String URL = "jdbc:#0://#1:#2/#3";
    public static String DB_HOST = System.getenv("DB_HOST") != null ? System.getenv("DB_HOST") : "localhost";
    public static int DB_PORT = 3306;
    public static String DB_NAME = "";
    public static String DB_USER = System.getenv("DB_USER") != null ? System.getenv("DB_USER") : "root";
    public static String DB_PASSWORD = System.getenv("DB_PASSWORD") != null ? System.getenv("DB_PASSWORD") : "";
    public static int MAX_CONN = 2;
    private static final Connection[] connections = new Connection[10];

    private static volatile DBService i; // FIX: volatile
    public static String dbName;

    private ConnPool connPool;

    public static synchronized DBService gI() { // FIX: synchronized singleton
        if (i == null) {
            i = new DBService();
        }
        return i;
    }

    private DBService() {
        this.connPool = ConnPool.gI();
    }

    // FIX: tất cả getConnectionForXxx() không còn đệ quy → không còn StackOverflowError
    // Pattern cố định: kiểm tra, tạo mới 1 lần nếu cần, return ngay
    private synchronized Connection getOrCreate(int idx) throws SQLException {
        if (connections[idx] != null) {
            try {
                if (!connections[idx].isValid(5)) {
                    connections[idx].close();
                    connections[idx] = null;
                }
            } catch (SQLException e) {
                connections[idx] = null;
            }
        }
        if (connections[idx] == null || connections[idx].isClosed()) {
            try {
                connections[idx] = getConnection();
            } catch (Exception ex) {
                ex.printStackTrace();
                throw new SQLException("Không thể tạo DB connection [" + idx + "]", ex);
            }
        }
        return connections[idx];
    }

    public Connection getConnectionForLogin() throws SQLException      { return getOrCreate(0); }
    public Connection getConnectionForLogout() throws SQLException     { return getOrCreate(1); }
    public Connection getConnectionForSaveData() throws SQLException   { return getOrCreate(2); }
    public Connection getConnectionForGame() throws SQLException       { return getOrCreate(3); }
    public Connection getConnectionForClan() throws SQLException       { return getOrCreate(4); }
    public Connection getConnectionForAutoSave() throws SQLException   { return getOrCreate(5); }
    public Connection getConnectionForGetPlayer() throws SQLException  { return getOrCreate(7); }
    public Connection getConnectionCreatPlayer() throws SQLException   { return getOrCreate(8); }

    public Connection getConnection() throws Exception {
        return DBHika.getConnection();
    }

    public void release(Connection con) {
        // no-op (HikariCP manages pooling)
    }

    public int currentActive() { return -1; }
    public int currentIdle()   { return -1; }
}
