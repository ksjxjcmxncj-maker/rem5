# ╔══════════════════════════════════════════════════════════════╗
#   CHẠY TRONG GOOGLE COLAB — SSH + Cloudflare Tunnel + Keep-Alive
#   Tác giả: Replit Agent
# ╚══════════════════════════════════════════════════════════════╝

import subprocess, re, threading, time, os, sys
from IPython.display import display, Javascript

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
#  ⚙️  CẤU HÌNH — chỉ chỉnh phần này
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
CF_TOKEN  = "cfat_GYQf1C56FQe6JWBEgCBq2n9oGQVifjtGYVveRVA173cba810"
SSH_PASS  = "colab2024"   # Đổi mật khẩu SSH tại đây
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

def log(msg): print(f"  {msg}", flush=True)
def banner(msg):
    w = 56
    print(f"\n╔{'═'*w}╗")
    for line in msg.strip().split('\n'):
        print(f"  {line}")
    print(f"╚{'═'*w}╝\n")

# ────────────────────────────────────────────────────────────
#  BƯỚC 0: Keep-Alive — chống Colab ngủ
# ────────────────────────────────────────────────────────────
log("🔄 Khởi động Keep-Alive...")

# JS: giả lập chuột/bàn phím mỗi 30 giây
display(Javascript("""
(function startKeepAlive() {
    let tick = 0;

    // Giả lập activity
    setInterval(() => {
        tick++;
        document.dispatchEvent(new MouseEvent('mousemove', {bubbles:true, clientX: tick%500, clientY: tick%300}));
        document.dispatchEvent(new KeyboardEvent('keydown',  {bubbles:true, key:'Shift'}));
        document.dispatchEvent(new KeyboardEvent('keyup',    {bubbles:true, key:'Shift'}));
    }, 30000);

    // Tự reconnect nếu bị disconnect
    setInterval(() => {
        const reconnectBtn = document.querySelector('colab-connect-button');
        if (reconnectBtn) { try { reconnectBtn.click(); } catch(e){} }
    }, 60000);

    console.log('[KeepAlive] ✅ đang chạy — tick mỗi 30 giây');
})();
"""))

# Python thread: tính toán nhẹ để VM không idle
def _keep_alive_loop():
    while True:
        _ = sum(i * i for i in range(10_000))
        time.sleep(25)

threading.Thread(target=_keep_alive_loop, daemon=True).start()
log("✅ Keep-Alive đang hoạt động (JS + Python thread)")

# ────────────────────────────────────────────────────────────
#  BƯỚC 1: Cài và khởi động SSH server
# ────────────────────────────────────────────────────────────
log("⏳ Cài đặt SSH server...")

subprocess.run(
    "apt-get install -qq -y openssh-server 2>/dev/null",
    shell=True, check=True
)

# Ghi cấu hình SSH
sshd_cfg = """
PermitRootLogin yes
PasswordAuthentication yes
ChallengeResponseAuthentication no
UsePAM yes
X11Forwarding yes
PrintMotd no
AcceptEnv LANG LC_*
Subsystem sftp /usr/lib/openssh/sftp-server
"""
with open("/etc/ssh/sshd_config", "w") as f:
    f.write(sshd_cfg)

subprocess.run("mkdir -p /var/run/sshd", shell=True)
subprocess.run(f"echo 'root:{SSH_PASS}' | chpasswd", shell=True, check=True)

# Tắt SSH cũ nếu có, khởi động lại
subprocess.run("pkill sshd 2>/dev/null || true", shell=True)
time.sleep(1)
subprocess.run("/usr/sbin/sshd", shell=True)
log("✅ SSH server đang chạy  (user=root)")

# ────────────────────────────────────────────────────────────
#  BƯỚC 2: Cài cloudflared
# ────────────────────────────────────────────────────────────
log("⏳ Tải cloudflared...")

subprocess.run(
    "wget -qO /usr/local/bin/cloudflared "
    "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64 "
    "&& chmod +x /usr/local/bin/cloudflared",
    shell=True, check=True
)

try:
    ver = subprocess.check_output(
        ["cloudflared", "--version"], stderr=subprocess.STDOUT, text=True
    ).strip().split('\n')[0]
    log(f"✅ {ver}")
except Exception:
    log("✅ cloudflared đã cài")

# ────────────────────────────────────────────────────────────
#  BƯỚC 3: Chạy Cloudflare Tunnel
# ────────────────────────────────────────────────────────────
log("⏳ Mở Cloudflare Tunnel...")

# Thử Named Tunnel (token) trước, fallback sang Quick Tunnel
use_named = bool(CF_TOKEN and len(CF_TOKEN) > 20)

if use_named:
    cmd = ["cloudflared", "tunnel", "--no-autoupdate",
           "run", "--token", CF_TOKEN]
    log("   Chế độ: Named Tunnel (token)")
else:
    cmd = ["cloudflared", "tunnel", "--no-autoupdate",
           "--url", "tcp://localhost:22"]
    log("   Chế độ: Quick Tunnel (tạm thời)")

proc = subprocess.Popen(
    cmd,
    stdout=subprocess.PIPE,
    stderr=subprocess.STDOUT,
    text=True
)

# ────────────────────────────────────────────────────────────
#  BƯỚC 4: Đọc log và in thông tin kết nối
# ────────────────────────────────────────────────────────────
TUNNEL_HOST = None
CONNECTED   = False
print("\n--- Log cloudflared ---")

for _ in range(120):
    line = proc.stdout.readline()
    if not line:
        break
    stripped = line.rstrip()
    if stripped:
        print(f"  {stripped}")

    # Quick tunnel → bắt URL
    m = re.search(r'https://([a-zA-Z0-9\-]+\.trycloudflare\.com)', line)
    if m:
        TUNNEL_HOST = m.group(1)
        break

    # Named tunnel → đã kết nối
    if any(k in line for k in ["Registered tunnel connection",
                                 "conns=1", "connection registered"]):
        CONNECTED = True
        break

print("--- Hết log ---\n")

# ────────────────────────────────────────────────────────────
#  BƯỚC 5: In hướng dẫn kết nối
# ────────────────────────────────────────────────────────────
if TUNNEL_HOST:
    # Quick Tunnel
    banner(f"""✅  TUNNEL SẴN SÀNG  (Quick Tunnel)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  Host     : {TUNNEL_HOST}
  User     : root
  Password : {SSH_PASS}
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  📋 SSH từ Replit/Termux:
  ssh -o StrictHostKeyChecking=no \\
      -o "ProxyCommand cloudflared access ssh --hostname {TUNNEL_HOST}" \\
      root@{TUNNEL_HOST}

  📋 Copy file từ Colab về máy:
  scp -o "ProxyCommand cloudflared access ssh --hostname {TUNNEL_HOST}" \\
      root@{TUNNEL_HOST}:/content/file.csv .

  📋 Copy file từ máy lên Colab:
  scp -o "ProxyCommand cloudflared access ssh --hostname {TUNNEL_HOST}" \\
      ./localfile.txt root@{TUNNEL_HOST}:/content/
""")

elif CONNECTED:
    # Named Tunnel
    banner(f"""✅  NAMED TUNNEL ĐÃ KẾT NỐI
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  User     : root
  Password : {SSH_PASS}
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  → Vào Cloudflare Dashboard để lấy hostname:
    Workers & Pages → Tunnels → Chọn tunnel → Hostname

  📋 SSH:
  ssh -o StrictHostKeyChecking=no \\
      -o "ProxyCommand cloudflared access ssh --hostname YOUR_HOSTNAME" \\
      root@YOUR_HOSTNAME
""")

else:
    log("❌ Không bắt được thông tin tunnel. Kiểm tra token hoặc cấu hình dashboard.")
    proc.terminate()
    sys.exit(1)

# ────────────────────────────────────────────────────────────
#  BƯỚC 6: Giữ tunnel sống — cell sẽ chạy mãi
# ────────────────────────────────────────────────────────────
log("🔄 Tunnel đang giữ kết nối... (Đừng dừng cell này)")
try:
    while True:
        line = proc.stdout.readline()
        if not line:
            # Tunnel bị ngắt, thử khởi động lại
            log("⚠️  Tunnel ngắt — đang khởi động lại...")
            proc = subprocess.Popen(
                cmd,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                text=True
            )
            time.sleep(5)
        time.sleep(0.5)
except KeyboardInterrupt:
    log("⛔ Đã dừng tunnel.")
    proc.terminate()
