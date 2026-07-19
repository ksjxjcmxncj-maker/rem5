#!/usr/bin/env python3
"""
Patch NRO APK smali để:
1. Hardcode server IP → 127.0.0.1, port → 14445 (toàn bộ assets + smali)
2. Replace "DragonBoy" server entry trong string-array smali → 127.0.0.1
3. Auto-connect: set selected server index = 0 (local bridge)
"""
import os, re, sys

SMALI_DIR  = sys.argv[1] if len(sys.argv) > 1 else "/tmp/game_src/smali"
ASSETS_DIR = sys.argv[2] if len(sys.argv) > 2 else "/tmp/game_src/assets"

TARGET_HOST     = "127.0.0.1"
TARGET_PORT     = 14445
TARGET_PORT_STR = str(TARGET_PORT)

# Các port NRO thường dùng cần thay thế
OTHER_PORTS = {14449, 21445, 14446, 14447, 14448, 14450, 5798}
OTHER_PORTS_HEX = {
    14449: "0x3871", 21445: "0x53c5", 14446: "0x386e",
    14447: "0x386f", 14448: "0x3870", 14450: "0x3872",
    5798:  "0x16a6",
}

changed = []

# ─────────────────────────────────────────────────────────────────
# 1. Patch assets (JSON / text / properties config files)
# ─────────────────────────────────────────────────────────────────
if os.path.exists(ASSETS_DIR):
    for root, _, files in os.walk(ASSETS_DIR):
        for fname in files:
            fp = os.path.join(root, fname)
            try:
                with open(fp, "r", encoding="utf-8", errors="ignore") as fh:
                    text = fh.read()
            except Exception:
                continue
            orig = text
            # Replace IPv4 addresses (server IPs)
            text = re.sub(
                r'(?<!["\w])(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})(?!["\w])',
                TARGET_HOST, text)
            # Replace common server domains
            text = re.sub(
                r'fw\.patus\.tech|nrolight\.net|nro\d*\.\w+\.\w+|DragonBoy\d*\.\w+',
                TARGET_HOST, text)
            # Replace ports in strings/JSON
            for p in OTHER_PORTS:
                text = text.replace(f':{p}', f':{TARGET_PORT}')
                text = text.replace(f'"{p}"', f'"{TARGET_PORT}"')
                text = text.replace(f"'{p}'", f"'{TARGET_PORT}'")
                text = text.replace(f' {p}\n', f' {TARGET_PORT}\n')
            if text != orig:
                with open(fp, "w", encoding="utf-8") as fh:
                    fh.write(text)
                changed.append(("assets", fp))
                print(f"  [assets] {os.path.relpath(fp, ASSETS_DIR)}")

# ─────────────────────────────────────────────────────────────────
# 2. Scan smali — collect info, then patch
# ─────────────────────────────────────────────────────────────────
RE_IP_CONST    = re.compile(r'(const-string\s+\w+,\s+")((?!127\.0\.0\.1)\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})(")')
RE_DOMAIN_CONST = re.compile(r'(const-string\s+\w+,\s+")(fw\.patus\.tech|nrolight\.net|[\w\-]+\.app\.github\.dev)(?!/ws)(")')
RE_PORT_DEC    = re.compile(r'(\bconst(?:/4|/16|/high|)?\s+\w+,\s+)(\d{5})\b')

# Smali files có server-selection logic
server_select_files = []
socket_files        = []

for root, _, files in os.walk(SMALI_DIR):
    for fname in files:
        if not fname.endswith(".smali"):
            continue
        fp = os.path.join(root, fname)
        with open(fp, "r", encoding="utf-8", errors="ignore") as fh:
            content = fh.read()

        if re.search(r'DragonBoy|ServerList|serverList|chon.*sv|selectServer|server.*list',
                     content, re.IGNORECASE):
            server_select_files.append(fp)

        if any(kw in content for kw in [
            "Ljava/net/Socket;", "SSLSocket", "InetSocketAddress",
            "openConnection", "HttpURLConnection"
        ]):
            socket_files.append(fp)

print(f"\nServer-select smali ({len(server_select_files)}):")
for f in server_select_files:
    print(f"  {os.path.relpath(f, SMALI_DIR)}")

print(f"\nSocket/network smali ({len(socket_files)}):")
for f in socket_files[:20]:
    print(f"  {os.path.relpath(f, SMALI_DIR)}")

# ─── Patch all smali ─────────────────────────────────────────────
for root, _, files in os.walk(SMALI_DIR):
    for fname in files:
        if not fname.endswith(".smali"):
            continue
        fp = os.path.join(root, fname)
        with open(fp, "r", encoding="utf-8", errors="ignore") as fh:
            content = fh.read()
        orig = content

        has_net = fp in socket_files
        has_ip  = RE_IP_CONST.search(content) is not None
        has_dom = RE_DOMAIN_CONST.search(content) is not None

        if has_net or has_ip or has_dom or fp in server_select_files:
            # Patch IPv4 literals → 127.0.0.1
            content = RE_IP_CONST.sub(
                lambda m: m.group(1) + TARGET_HOST + m.group(3), content)
            # Patch domain literals → 127.0.0.1
            content = RE_DOMAIN_CONST.sub(
                lambda m: m.group(1) + TARGET_HOST + m.group(3), content)
            # Patch port hex literals
            for old_port, old_hex in OTHER_PORTS_HEX.items():
                content = content.replace(old_hex, hex(TARGET_PORT))
            # Patch decimal port constants (5-digit game ports)
            def patch_dec_port(m):
                val = int(m.group(2))
                if val in OTHER_PORTS:
                    return m.group(1) + TARGET_PORT_STR
                return m.group(0)
            content = RE_PORT_DEC.sub(patch_dec_port, content)

        if content != orig:
            with open(fp, "w", encoding="utf-8") as fh:
                fh.write(content)
            changed.append(("smali", fp))
            print(f"  [smali] {os.path.relpath(fp, SMALI_DIR)}")

# ─────────────────────────────────────────────────────────────────
# 3. Patch "DragonBoy" server name string → "Local Bridge"
#    và replace server IP string trong cùng file
# ─────────────────────────────────────────────────────────────────
DRAGONBOY_DONE = 0
for fp in server_select_files:
    with open(fp, "r", encoding="utf-8", errors="ignore") as fh:
        content = fh.read()
    orig = content

    # Replace DragonBoy tên server → "Local Bridge"
    content = re.sub(
        r'(const-string\s+\w+,\s+")DragonBoy(\d*)(.*?)(")',
        r'\g<1>Local Bridge\3\4',
        content
    )
    # Replace DragonBoy trong mảng string const
    content = content.replace('"DragonBoy"', '"Local Bridge"')
    content = content.replace('"DragonBoy11"', '"Local Bridge"')
    content = content.replace('"DragonBoy 11"', '"Local Bridge"')

    if content != orig:
        with open(fp, "w", encoding="utf-8") as fh:
            fh.write(content)
        DRAGONBOY_DONE += 1
        changed.append(("dragonboy", fp))
        print(f"  [dragonboy→local] {os.path.relpath(fp, SMALI_DIR)}")

# ─────────────────────────────────────────────────────────────────
# 4. Auto-connect patch — tìm server-select Activity và inject
#    Chiến lược: tìm method onClick/onItemClick/selectServer,
#    thêm auto-call trong onCreate để tự kết nối server đầu tiên
# ─────────────────────────────────────────────────────────────────
AUTO_CONNECT_DONE = False

for fp in server_select_files:
    with open(fp, "r", encoding="utf-8", errors="ignore") as fh:
        content = fh.read()
    orig = content

    # Lấy tên class
    class_match = re.search(r'\.class.*?public.*?L([^;]+);', content)
    if not class_match:
        continue
    cls_path = class_match.group(1)  # vd: com/game/SelectServerActivity

    # Tìm tên method kết nối (onClick, onItemClick, connectServer, ...)
    connect_methods = re.findall(
        r'\.method\s+(?:public|private|protected)[^\n]*?\s+'
        r'((?:on(?:Item)?Click|connect\w*|selectServer\w*|loginServer\w*|startGame\w*|chonSv\w*))'
        r'\s*\(',
        content, re.IGNORECASE
    )

    # Tìm onCreate
    oncreate_match = re.search(
        r'(\.method public onCreate\(Landroid/os/Bundle;\)V\n)(.*?)(\.end method)',
        content, re.DOTALL
    )

    if oncreate_match and connect_methods:
        method_name = connect_methods[0]
        method_sig  = f"L{cls_path};"
        before      = oncreate_match.group(1)
        body        = oncreate_match.group(2)
        end         = oncreate_match.group(3)

        # Chỉ inject 1 lần
        if "AUTO-CONNECT" not in body:
            # Tìm .locals và tăng lên nếu cần (đảm bảo có v0, v1)
            locals_match = re.search(r'(\s+\.locals\s+)(\d+)', body)
            if locals_match:
                n = int(locals_match.group(2))
                if n < 2:
                    body = body.replace(
                        locals_match.group(0),
                        locals_match.group(1) + "2"
                    )

            snippet = f"""
    # [AUTO-CONNECT PATCH v1.4.0] tự động kết nối local bridge
    const/4 v0, 0x0
    invoke-virtual {{p0, v0}}, {method_sig}->{method_name}(I)V

"""
            # Insert trước return-void cuối
            new_body = re.sub(
                r'(\n    return-void\n)(?!.*\n    return-void\n)',
                snippet + r'\1',
                body,
                count=1,
                flags=re.DOTALL
            )
            if new_body != body:
                content = before + new_body + end
                AUTO_CONNECT_DONE = True

    if content != orig:
        with open(fp, "w", encoding="utf-8") as fh:
            fh.write(content)
        changed.append(("auto-connect", fp))
        print(f"  [auto-connect] {os.path.relpath(fp, SMALI_DIR)}")

# ─────────────────────────────────────────────────────────────────
# 5. Fallback: nếu không inject được smali Activity,
#    tìm và patch SharedPreferences/prefs default server index
# ─────────────────────────────────────────────────────────────────
PREFS_DONE = 0
if not AUTO_CONNECT_DONE:
    # Tìm file smali lưu default server index (putInt "server_index" hay tương tự)
    for root, _, files in os.walk(SMALI_DIR):
        for fname in files:
            if not fname.endswith(".smali"):
                continue
            fp = os.path.join(root, fname)
            with open(fp, "r", encoding="utf-8", errors="ignore") as fh:
                content = fh.read()
            orig = content

            # Tìm pattern: putInt/putString "server_index" hoặc "selected_server"
            if re.search(r'"(?:server_index|selected_server|sv_index|serverIndex|current_server)"',
                         content, re.IGNORECASE):
                # Replace default value từ bất kỳ non-zero → 0
                # Pattern: const/4 vX, 0xN (N > 0) → 0x0
                content = re.sub(
                    r'(const/4\s+v\d+,\s+)0x[1-9a-f](\b)',
                    r'\g<1>0x0\2',
                    content
                )
                if content != orig:
                    with open(fp, "w", encoding="utf-8") as fh:
                        fh.write(content)
                    PREFS_DONE += 1
                    changed.append(("prefs-default", fp))
                    print(f"  [prefs-default-server] {os.path.relpath(fp, SMALI_DIR)}")

# ─────────────────────────────────────────────────────────────────
# Summary
# ─────────────────────────────────────────────────────────────────
print(f"\n{'='*55}")
print(f"TỔNG: {len(changed)} files đã patch")
print(f"DragonBoy → Local Bridge: {'✅ ' + str(DRAGONBOY_DONE) + ' files' if DRAGONBOY_DONE else '⚠️ Không tìm thấy'}")
print(f"Auto-connect smali inject: {'✅ OK' if AUTO_CONNECT_DONE else '⚠️ Không inject được Activity — dùng prefs fallback'}")
print(f"Prefs default server:      {'✅ ' + str(PREFS_DONE) + ' files' if PREFS_DONE else '—'}")
print(f"Server IP → {TARGET_HOST}")
print(f"Port      → {TARGET_PORT}")
print(f"{'='*55}")
