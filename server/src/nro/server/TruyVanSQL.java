package nro.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TruyVanSQL {

    // Thông tin kết nối đến cơ sở dữ liệu
    static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    static final String DB_URL = "jdbc:mysql://localhost/ngocrong";

    // Thông tin đăng nhập vào cơ sở dữ liệu
    static final String USER = "root";
    static final String PASS = "";

    public static String getPlayerNameById(int playerId) {
        String playerName = null;

        try {
            // Bước 1: Đăng ký JDBC Driver
            Class.forName(JDBC_DRIVER);

            // Bước 2: Mở kết nối
            String sql = "SELECT name FROM player WHERE id = ?";
            try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setInt(1, playerId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        playerName = rs.getString("name");
                    }
                }
            }
        } catch (SQLException se) {
            se.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return playerName;
    }

    public static void main(String[] args) {
        // Gọi hàm để lấy tên của người chơi với ID cụ thể
        int playerId = 1; // Điền ID cần tìm kiếm
        String playerName = getPlayerNameById(playerId);

        // In ra tên của người chơi (hoặc xử lý nó theo nhu cầu của bạn)
        System.out.println("Player Name: " + playerName);
    }
}
