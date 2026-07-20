// OBITO
package server;

import db.DbManager;
import io.Session;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {

    private static final int MAX_CONNECTIONS = 500; // FIX: giới hạn kết nối

    private static final Server instance = new Server();
    private ServerSocket listen;
    private Config config = new Config("server.ini");
    private ServerManager manager = new ServerManager();
    private ServerService service = new ServerService(this.manager);
    private boolean running;

    public static Server getInstance() {
        return instance;
    }

    public void start() {
        activeCommandLine();
        DbManager.getInstance().start();
        this.running = true;
        try {
            this.listen = new ServerSocket(this.config.getListen(), MAX_CONNECTIONS);
            System.out.println("listening port: " + this.config.getListen());
            int i = 0;
            while (this.running) {
                Session session = new Session(this.listen.accept(), i++);
                System.out.println("client " + session.sessionName + " connected!");
            }
        } catch (IOException ex) {
            if (this.running) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void shutdown() {
        this.running = false;
        try {
            if (this.listen != null) {
                this.listen.close();
            }
        } catch (IOException iOException) {
        }
        DbManager.getInstance().shutdown();
    }

    private void activeCommandLine() {
        new Thread(() -> {
            // FIX: bắt NoSuchElementException khi stdin bị đóng
            try (Scanner sc = new Scanner(System.in)) {
                while (true) {
                    try {
                        String line = sc.nextLine();
                        if (line == null) break;
                        if (line.equals("baotri")) {
                            shutdown();
                            break;
                        }
                    } catch (NoSuchElementException e) {
                        System.out.println("[Server] stdin đã đóng, thoát command line");
                        break;
                    }
                }
            }
        }, "Active line").start();
    }

    public static void main(String[] args) {
        instance.start();
    }

    public Config getConfig() { return this.config; }
    public ServerManager getManager() { return this.manager; }
    public ServerService getService() { return this.service; }
}
