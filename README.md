# 🎮 Teamobi 2026 - Termux Server Manager

Private server cho game Teamobi 2026 chạy trực tiếp trên Android qua Termux.

## ⚡ Cài đặt (1 lệnh)

```bash
curl -fsSL https://raw.githubusercontent.com/akah3674-glitch/rem5/main/setup.sh | bash
```

## 📋 Tính năng Menu

| # | Chức năng | Mô tả |
|---|-----------|-------|
| 1 | **Setup** | Tải Teamobi2026.rar (~630MB), cài packages, init DB |
| 2 | **Chạy Server** | Khởi động Game Server + Login Server + MariaDB |
| 3 | **Tắt Server** | Dừng tất cả server an toàn |
| 4 | **Mèo Lù 🐱** | Quản lý pet đặc biệt: thêm, nâng cấp, đổi tên |
| 5 | **Đăng ký TK** | Tạo tài khoản với mật khẩu + quyền (Player/GM/Admin) |
| 6 | **Thêm Vàng/Ngọc** | Nạp vàng/ngọc cho nhân vật hoặc toàn bộ server |
| 7 | **Danh sách TK** | Xem, đổi mật khẩu, đổi quyền, xóa tài khoản |
| 8 | **Logs** | Xem log realtime của server |

## 🔌 Cổng (Ports)

| Dịch vụ | Port | Giao thức |
|---------|------|-----------|
| Game Server | 14445 | TCP |
| HTTP/Web | 8080 | TCP |
| Database | 3306 | TCP (local) |

## 📦 Yêu cầu

- Termux (Android 7+)
- Kết nối internet để tải server (~630MB lần đầu)
- RAM tối thiểu: 2GB

## 📁 Cấu trúc

```
~/teamobi-server/
├── Teamobi2026.rar     ← Server files gốc
├── bin/
│   ├── start.sh        ← Khởi động server
│   └── stop.sh         ← Dừng server
├── logs/
│   ├── game.log
│   └── login.log
├── .config             ← Đường dẫn JAR
└── .setup_done         ← Flag đã setup
```

---
> By [akah3674-glitch](https://github.com/akah3674-glitch) | rem5
