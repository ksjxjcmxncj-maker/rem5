package nro.server;

import nro.services.Service;
import nro.utils.Log;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.logging.Level;
import java.util.Random;
import nro.server.io.Message;

public class MenuQuanLy extends JPanel implements ActionListener {

    private JButton baotri, thaydoiexp, chatserver, kickplayer, xemplayer;

    public MenuQuanLy() {
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 0.5;
        gbc.weighty = 0.5;
        gbc.insets = new Insets(10, 10, 10, 10); // Khoảng cách giữa các nút

        baotri = createButton("BẢO TRÌ");
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(baotri, gbc);

        thaydoiexp = createButton("ĐỔI EXP SERVER");
        gbc.gridx = 1;
        gbc.gridy = 0;
        add(thaydoiexp, gbc);

        chatserver = createButton("THÔNG BÁO GAME");
        gbc.gridx = 0;
        gbc.gridy = 1;
        add(chatserver, gbc);

        kickplayer = createButton("KICK ALL PLAYER");
        gbc.gridx = 1;
        gbc.gridy = 1;
        add(kickplayer, gbc);

        xemplayer = createButton("TỔNG PLAYER ONLINE");
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        add(xemplayer, gbc);
    }

    private JButton createButton(String text) {
        JButton button = new JButton(text) {
            private Color defaultColor = new Color(255, 105, 180);
            private Color hoverColor = new Color(255, 182, 193);
            private Color clickColor = new Color(173, 216, 230);
            private boolean isClicked = false;
            private Timer shakeTimer;
            private int shakeX = 0;
            private int shakeY = 0;

            {
                shakeTimer = new Timer(50, new ActionListener() {
                    private Random rand = new Random();

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (!isClicked) {
                            shakeX = rand.nextInt(5) - 2;
                            shakeY = rand.nextInt(5) - 2;
                            repaint();
                        } else {
                            shakeX = 0;
                            shakeY = 0;
                            repaint();
                        }
                    }
                });
                shakeTimer.start();
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Đặt màu nền dựa trên trạng thái
                if (isClicked) {
                    g2.setColor(clickColor);
                } else if (getModel().isRollover()) {
                    g2.setColor(hoverColor);
                } else {
                    g2.setColor(defaultColor);
                }

                // Tạo hiệu ứng rung rinh
                g2.translate(shakeX, shakeY);

                // Màu nền
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);

                // Viền màu cam nhẹ
                g2.setColor(new Color(255, 165, 0, 128));
                g2.setStroke(new BasicStroke(2));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 30, 30);

                // Vẽ chữ
                g2.setFont(getFont().deriveFont(Font.BOLD));
                FontMetrics fm = g2.getFontMetrics();
                Rectangle r = getBounds();
                int x = (r.width - fm.stringWidth(getText())) / 2;
                int y = (r.height - fm.getHeight()) / 2 + fm.getAscent();
                g2.setColor(getForeground());
                g2.drawString(getText(), x, y);

                g2.dispose();
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(180, 50);
            }

            @Override
            protected void processMouseEvent(MouseEvent e) {
                super.processMouseEvent(e);
                if (e.getID() == MouseEvent.MOUSE_PRESSED) {
                    isClicked = true;
                } else if (e.getID() == MouseEvent.MOUSE_RELEASED) {
                    isClicked = false;
                }
                repaint();
            }
        };

        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setBorderPainted(false); // Vô hiệu hóa viền mặc định
        button.setFont(new Font("Arial", Font.BOLD, 16));
        button.setForeground(Color.BLACK);

        // Thêm hiệu ứng hover
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setOpaque(true);
                button.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setOpaque(false);
                button.repaint();
            }
        });

        button.addActionListener(this);
        return button;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == baotri) {
            Maintenance.gI().start(15);
            Log.error("Tiến Hành Bảo Trì !\n");
        } else if (e.getSource() == thaydoiexp) {
            String exp = JOptionPane.showInputDialog(this, "Bảng Exp Server\n"
                    + "Exp Server hiện tại: " + Manager.RATE_EXP_SERVER);
            if (exp != null) {
                Manager.RATE_EXP_SERVER = Byte.parseByte(exp);
                Log.error("Exp hiện tại là: " + exp + "\n");
            }
        } else if (e.getSource() == chatserver) {
            String chat = JOptionPane.showInputDialog(this, "Thông Báo Server\n");
            if (chat != null) {
                Message msg = new Message(93);
                try {
                    msg.writer().writeUTF(chat);
                } catch (IOException ex) {
                    java.util.logging.Logger.getLogger(Panel.class.getName()).log(Level.SEVERE, null, ex);
                }
                Service.getInstance().sendMessAllPlayer(msg);
                msg.cleanup();
                Log.error("Thông báo: " + chat + "\n");
            }
        } else if (e.getSource() == kickplayer) {
            new Thread(() -> {
                Client.gI().close();
            }).start();
        } else if (e.getSource() == xemplayer) {
            String message = "TỔNG PLAYER ONLINE: " + Client.gI().getPlayers().size() + "\n"
                    + "TỔNG THREAD: " + Thread.activeCount();
            JOptionPane.showMessageDialog(this, message);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFrame frame = new JFrame("BY BA CÔN THÍCH TRÔN");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.getContentPane().add(new MenuQuanLy());
                frame.setSize(400, 300);
                frame.setVisible(true);
            }
        });
    }
}
