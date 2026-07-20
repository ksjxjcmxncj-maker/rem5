package nro.server;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Panel;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import nro.attr.AttributeManager;
import nro.jdbc.DBService;
import nro.jdbc.daos.AccountDAO;
import nro.jdbc.daos.HistoryTransactionDAO;
import nro.jdbc.daos.PlayerDAO;
import nro.login.LoginSession;
import nro.manager.ConsignManager;
import nro.manager.TopPowerManager;
import nro.models.boss.BossFactory;
import nro.models.boss.BossManager;
import nro.models.map.DaiHoiVoThuat.DHVT23Manager;
import nro.models.map.dungeon.DungeonManager;
import nro.models.map.phoban.BanDoKhoBau;
import nro.models.map.phoban.DoanhTrai;
import nro.models.player.Player;
//import nro.netty.NettyServer;
import nro.server.io.Session;
import nro.services.ClanService;
import nro.utils.Log;
import nro.utils.TimeUtil;
import nro.utils.Util;
import lombok.Getter;
import lombok.Setter;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import nro.manager.SieuHangControl;
import nro.manager.SieuHangManager;
import nro.manager.TopBanDoKhoBau;
//import nro.manager.TopBangBDKBManager;
import nro.manager.TopKillWhisManager;
import nro.manager.TopRichManManager;
import nro.models.item.ItemOption;
import nro.models.map.DaiHoiVoThuat.DaiHoiVoThuatService;
import nro.models.map.VoDaiSinhTu.VoDaiSinhTu;
import nro.models.map.VoDaiSinhTu.VoDaiSinhTuManager;
import nro.models.map.phoban.KhiGas;
import nro.models.npc.NpcManager;
import nro.sendEff.SendEffect;
import nro.services.func.minigame.ChonAiDay_Gem;
import nro.services.func.minigame.ChonAiDay_Gold;
import nro.services.func.minigame.ChonAiDay_Ruby;
import nro.services.func.MiniGame;
import org.json.simple.JSONArray;

public class ServerManager {

    public static String timeStart;

    public static final Map CLIENTS = new ConcurrentHashMap(); // FIX: thread-safe

    public static String NAME = "";
    public static int PORT = 14445;

    private Controller controller;

    private static ServerManager instance;

    public static ServerSocket listenSocket;
    public static boolean isRunning;
    private SieuHangControl sieuHangControl;

    @Getter
    private LoginSession login;
    public static boolean updateTimeLogin;
    @Getter
    @Setter
    private AttributeManager attributeManager;
    private long lastUpdateAttribute;
    @Getter
    private DungeonManager dungeonManager;
    private long delaylogin;

    public SieuHangControl getSieuHangController() {
        return this.sieuHangControl;
    }

    public void init() {
        Manager.gI();
        HistoryTransactionDAO.deleteHistory();
        BossFactory.initBoss();
        this.controller = new Controller();
        if (updateTimeLogin) {
            AccountDAO.updateLastTimeLoginAllAccount();
        }
    }

    public static synchronized ServerManager gI() {
        if (instance == null) {
            instance = new ServerManager();
            instance.init();
        }
        return instance;
    }

    public static void main(String[] args) {
        timeStart = TimeUtil.getTimeNow("dd/MM/yyyy HH:mm:ss");
        ServerManager.gI().run();
        //QuanLiServer.main(args);

    }
    public void run() {
        try {
            isRunning = true;
            JFrame frame = new JFrame("");
            frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // Đặt để bắt sự kiện đóng cửa sổ

            // Đặt biểu tượng của frame
            ImageIcon icon = new ImageIcon("path_to_your_icon_file"); // Đường dẫn tới biểu tượng của bạn
            frame.setIconImage(icon.getImage());

            // Thêm WindowListener để bắt sự kiện đóng cửa sổ
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    int choice = JOptionPane.showConfirmDialog(frame,
                            "Bạn có muốn đóng MENU Quản Lý không?",
                            "Xác nhận đóng.",
                            JOptionPane.YES_NO_OPTION);
                    if (choice == JOptionPane.YES_OPTION) {
                        frame.dispose(); // Đóng frame nếu người dùng chọn đồng ý
                        System.exit(0); // Đóng ứng dụng hoặc thực hiện hành động khác
                    }
                }
            });

            // Tạo panel MenuQuanLy và thêm vào frame
            JPanel panel = new MenuQuanLy();
            frame.add(panel);

            // Cấu hình frame và hiển thị
            frame.pack();
            frame.setVisible(true);

            delaylogin = System.currentTimeMillis();
            activeCommandLine();
            activeGame();
            activeLogin();
            autoTask();
            // Vẽ chữ "QUẢN LÝ MÁY CHỦ" trên frame
            JLabel label = new JLabel("QUẢN LÝ MÁY CHỦ", JLabel.CENTER);
            label.setFont(new Font("Arial", Font.BOLD, 24)); // Font in đậm, kích thước 24
            label.setForeground(Color.RED); // Màu đỏ cho chữ
            label.setBorder(BorderFactory.createLineBorder(Color.YELLOW, 2)); // Viền màu vàng
            frame.add(label, BorderLayout.NORTH); // Đặt label ở phía trên cùng của frame
            // Khởi tạo và chạy các công việc khác như bạn đã làm
            //  NettyServer nettyServer = new NettyServer();
//            nettyServer.start();
            TaiXiu.gI().lastTimeEnd = System.currentTimeMillis() + 1000;
            new Thread(TaiXiu.gI(), "Thread Tài Xỉu").start();
            activeServerSocket();
        } catch (Exception e) {
            e.printStackTrace();
            // Xử lý ngoại lệ theo yêu cầu của bạn
        }
    }

    public void activeLogin() {
        login = new LoginSession();
        login.connect(Manager.loginHost, Manager.loginPort);
    }

    private void activeServerSocket() {
        try {
            Log.log("Start server......... Current thread: " + Thread.activeCount());
            listenSocket = new ServerSocket(PORT);
            while (isRunning) {
                try {
                    Socket sc = listenSocket.accept();
                    String ip = (((InetSocketAddress) sc.getRemoteSocketAddress()).getAddress()).toString().replace("/", "");
                    if (canConnectWithIp(ip)) {
                        Session session = new Session(sc, controller, ip);
                        session.ipAddress = ip;
                    } else {
                        sc.close();
                    }
                } catch (Exception e) {
            Log.error(ServerManager.class, e);
        }
            }
            listenSocket.close();
        } catch (Exception e) {
            Log.error(ServerManager.class, e, "Lỗi mở port");
            System.exit(0);
        }
    }

    private boolean canConnectWithIp(String ipAddress) {
        Object o = CLIENTS.get(ipAddress);
        if (o == null) {
            CLIENTS.put(ipAddress, 1);
            return true;
        } else {
            int n = Integer.parseInt(String.valueOf(o));
            if (n < Manager.MAX_PER_IP) {
                n++;
                CLIENTS.put(ipAddress, n);
                return true;
            } else {
                return false;
            }
        }
    }

    public void disconnect(Session session) {
        Object o = CLIENTS.get(session.ipAddress);
        if (o != null) {
            int n = Integer.parseInt(String.valueOf(o));
            n--;
            if (n < 0) {
                n = 0;
            }
            CLIENTS.put(session.ipAddress, n);
        }
    }

    private void activeCommandLine() {
        new Thread(() -> {
            Scanner sc = new Scanner(System.in);
            while (true) {
                String line = sc.nextLine();
                if (line.equals("baotri")) {
                    new Thread(() -> {
                        Maintenance.gI().start(5);
                    }).start();
                } else if (line.equals("athread")) {
                    Log.error("Số thread hiện tại của Server Ngoc Rong DT: " + Thread.activeCount());
                } else if (line.equals("nplayer")) {
                    Log.error("Số lượng người chơi hiện tại của Server Ngoc Rong DT: " + Client.gI().getPlayers().size());
                } else if (line.equals("a")) {
                    new Thread(() -> {
                        Client.gI().close();
                    }).start();
                }
            }
        }, "Active line").start();
    }

    private void activeGame() {
        long delay = 500;
        long delaySecond = 5000;
        new Thread(() -> {
            while (isRunning) {
                long l1 = System.currentTimeMillis();
                BossManager.gI().updateAllBoss();
                long l2 = System.currentTimeMillis() - l1;
                if (l2 < delay) {
                    try {
                        Thread.sleep(delay - l2);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }, "Update boss").start();

        new Thread(() -> {
            while (isRunning) {
                long start = System.currentTimeMillis();
                java.util.List<DoanhTrai> dtSnapshot = new java.util.ArrayList<>(DoanhTrai.DOANH_TRAIS);
                for (DoanhTrai dt : dtSnapshot) {
                    dt.update();
                }
                java.util.List<BanDoKhoBau> bdkbSnapshot = new java.util.ArrayList<>(BanDoKhoBau.BAN_DO_KHO_BAUS);
                for (BanDoKhoBau bdkb : bdkbSnapshot) {
                    bdkb.update();
                }
                java.util.List<KhiGas> kgSnapshot = new java.util.ArrayList<>(KhiGas.KHI_GAS);
                for (KhiGas khiGas : kgSnapshot) {
                    khiGas.update();
                }
                long timeUpdate = System.currentTimeMillis() - start;

                if (timeUpdate < delay) {
                    try {
                        Thread.sleep(delay - timeUpdate);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }, "Update pho ban").start();
        new Thread(() -> {
            while (isRunning) {
                try {
                    long start = System.currentTimeMillis();
                    if (attributeManager != null) {
                        attributeManager.update();
                        if (Util.canDoWithTime(lastUpdateAttribute, 600000)) {
                            Manager.gI().updateAttributeServer();
                        }
                    }
                    long timeUpdate = System.currentTimeMillis() - start;
                    if (timeUpdate < delay) {
                        Thread.sleep(delay - timeUpdate);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, "Update Attribute Server").start();

        dungeonManager = new DungeonManager();
        dungeonManager.start();

        new Thread(dungeonManager, "Con Đường Rắn Độc").start();
        this.sieuHangControl = new SieuHangControl();
        new Thread(this.sieuHangControl, "Sieu hang").start();
        new Thread(() -> {
            while (isRunning) {
                try {
                    long start = System.currentTimeMillis();
                    SieuHangManager.Update();
                    long timeUpdate = System.currentTimeMillis() - start;
                    if (timeUpdate < delay) {
                        Thread.sleep(delay - timeUpdate);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, "Update giai sieu hang").start();

        new Thread(() -> {
            while (isRunning) {
                try {
                    long start = System.currentTimeMillis();
                    SieuHangManager.UpdatePedingFight();
                    long timeUpdate = System.currentTimeMillis() - start;
                    if (timeUpdate < delaySecond) {
                        Thread.sleep(delaySecond - timeUpdate);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, "Update giai sieu hang pending").start();

        new Thread(() -> {
            while (isRunning) {
                try {
                    long start = System.currentTimeMillis();
                    DHVT23Manager.gI().update();
                    VoDaiSinhTuManager.gI().update();
                    long timeUpdate = System.currentTimeMillis() - start;
                    if (timeUpdate < delay) {
                        Thread.sleep(delay - timeUpdate);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, "Update dai hoi vo thuat").start();
    }

    public void close(long delay) {
        try {
            dungeonManager.shutdown();
        } catch (Exception e) {
            Log.error(ServerManager.class, e);
        }
        try {
            Manager.gI().updateEventCount();
        } catch (Exception e) {
            Log.error(ServerManager.class, e);
        }
        try {
            Manager.gI().updateAttributeServer();
        } catch (Exception e) {
            Log.error(ServerManager.class, e);
        }
        try {
            Client.gI().close();
        } catch (Exception e) {
            Log.error(ServerManager.class, e);
        }
        try {
            ClanService.gI().close();
        } catch (Exception e) {
            Log.error(ServerManager.class, e);
        }
        try {
            ConsignManager.getInstance().close();
        } catch (Exception e) {
            Log.error(ServerManager.class, e);
        }
        Client.gI().close();
        Log.success("SUCCESSFULLY MAINTENANCE!...................................");
        System.exit(0);
    }

    public void resetNhanQuaHangNgay() {
        String url = "jdbc:mysql://localhost:3306/ngocrong";
        String username = "root";
        String password = "";

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            String sql = "UPDATE player SET checkNhanQua = ?";
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, "[1,1]");

                int rowsUpdated = statement.executeUpdate();
                Log.success("SUCCESSFULLY UPDATE NHẬN QUÀ HẰNG NGÀY: " + rowsUpdated + ".....................");
            }
        } catch (SQLException e) {
            System.err.println("Lỗi: " + e.getMessage());
        }
    }

    public void saveAll(boolean updateTimeLogout) {
        try {
            List<Player> list = Client.gI().getPlayers();
            Connection conn = DBService.gI().getConnectionForAutoSave();
            for (Player player : list) {
                try {
                    PlayerDAO.updateTimeLogout = updateTimeLogout;
                    PlayerDAO.updatePlayer(player, conn);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void autoTask() {
        ScheduledExecutorService autoSave = Executors.newScheduledThreadPool(1);
        autoSave.scheduleWithFixedDelay(() -> {
            saveAll(false);
        }, 300000, 300000, TimeUnit.MILLISECONDS);

//        ScheduledExecutorService autoMingame = Executors.newScheduledThreadPool(1);
//        autoMingame.scheduleWithFixedDelay(() -> {
        MiniGame.gI().MiniGame_S1.activate(1000);
//        }, 0, 1, TimeUnit.HOURS);

        ScheduledExecutorService autoDHVTM = Executors.newScheduledThreadPool(1);
        autoDHVTM.scheduleWithFixedDelay(() -> {
            DaiHoiVoThuatService.gI().initDaiHoiVoThuat();
        }, 0, 1, TimeUnit.MINUTES);

        ScheduledExecutorService autoTopPower = Executors.newScheduledThreadPool(1);
        autoTopPower.scheduleWithFixedDelay(() -> {
            TopPowerManager.getInstance().load();
            TopRichManManager.getInstance().load();
            TopKillWhisManager.getInstance().load();
            TopBanDoKhoBau.getInstance().load();
        }, 0, 600000, TimeUnit.MILLISECONDS);

        ScheduledExecutorService autoNpcChat = Executors.newScheduledThreadPool(1);
        autoNpcChat.scheduleWithFixedDelay(() -> {
            NpcManager.Autochatnpc();
        }, 0, 30000, TimeUnit.MILLISECONDS);

    }

    public int getNumPlayer() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
}
