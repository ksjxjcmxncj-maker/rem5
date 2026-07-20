# NRO Server Info

## Kết nối game
- **IP:** `frp.freefrp.net` (frp tunnel)
- **Port:** `21445` (frp.freefrp.net)

## Tài khoản Admin
- **Username:** `admin`
- **Password:** `12345678`
- **Character:** memeiue (admin=99, is_admin=1)

## Cấu trúc Codespace
- JAR: `/home/codespace/nro/SRC/NgocRongOnline.jar`
- Config: `/home/codespace/nro/SRC/Config.properties`
- DB: `nro1` (MariaDB)
- Logs: `~/logs/`

## Khởi động server
```bash
sudo service mariadb start && sleep 3
# Tunnel tự khởi động qua frpc (xem scripts/keepalive_codespace.sh)
# frpc -c /tmp/frpc_nro.toml → frp.freefrp.net:21445
sleep 6
cd /home/codespace/nro/SRC
nohup java -Xms256m -Xmx1g -jar NgocRongOnline.jar > ~/logs/server.log 2>&1 &
```
