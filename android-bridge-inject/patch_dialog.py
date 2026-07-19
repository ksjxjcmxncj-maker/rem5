#!/usr/bin/env python3
"""
patch_dialog.py v3.0 — Xóa dialog "Nhập IP và PORT" khỏi smali

Chiến lược (theo thứ tự):
  A. Scan DragonBoy11 smali → log toàn bộ nội dung để debug
  B. Tìm method gọi show() trên Dialog/AlertDialog trong DragonBoy11 → return-void đầu method
  C. Tìm method có onCreate/onResume của Activity trong DragonBoy11 → chèn skip logic
  D. Fallback: scan TẤT CẢ smali tìm chuỗi "show()V" gần EditText creation
  E. Patch "auto-connect": tìm method đọc IP/Port từ EditText → chèn hardcode
"""
import os
import sys
import re

SMALI_DIR = sys.argv[1] if len(sys.argv) > 1 else "/tmp/game_src/smali"

TARGET_IP   = "127.0.0.1"
TARGET_PORT = "15000"

patched = 0
logged_dragonboy = False


def find_dragonboy11_files():
    """Tìm tất cả smali trong com/DefaultCompany/DragonBoy11/"""
    files = []
    for root, dirs, fnames in os.walk(SMALI_DIR):
        if "DefaultCompany" in root and "DragonBoy11" in root:
            for f in fnames:
                if f.endswith(".smali"):
                    files.append(os.path.join(root, f))
    return files


def find_all_smali():
    """Lấy tất cả .smali files"""
    files = []
    for root, dirs, fnames in os.walk(SMALI_DIR):
        for f in fnames:
            if f.endswith(".smali"):
                files.append(os.path.join(root, f))
    return files


def extract_methods(content):
    """Parse smali content → trả về list of (method_name, start_line, end_line, method_text)"""
    methods = []
    lines = content.split("\n")
    i = 0
    while i < len(lines):
        if lines[i].strip().startswith(".method"):
            start = i
            j = i + 1
            while j < len(lines):
                if lines[j].strip() == ".end method":
                    methods.append((
                        lines[start].strip(),
                        start,
                        j,
                        "\n".join(lines[start:j+1])
                    ))
                    i = j
                    break
                j += 1
        i += 1
    return methods


def patch_add_return_void_at_top(method_text):
    """
    Chèn return-void (hoặc return) vào đầu method body, sau khai báo registers/annotations.
    Dành cho void methods.
    """
    lines = method_text.split("\n")
    insert_after = 0
    for idx, line in enumerate(lines[1:], start=1):
        stripped = line.strip()
        # Bỏ qua directive, annotation, empty lines
        if (stripped.startswith(".registers") or
            stripped.startswith(".locals") or
            stripped.startswith(".annotation") or
            stripped.startswith(".param") or
            stripped.startswith("#") or
            stripped.startswith(".prologue") or
            stripped == ""):
            insert_after = idx
        else:
            break

    # Xác định kiểu return
    first_line = lines[0]
    return_type = "V"  # void
    m = re.search(r'\)(\S+)$', first_line)
    if m:
        return_type = m.group(1)

    if return_type == "V":
        ret_instruction = "    return-void"
    elif return_type in ("I", "B", "S", "C", "Z"):
        ret_instruction = "    const/4 v0, 0x0\n    return v0"
    elif return_type == "J":
        ret_instruction = "    const-wide/16 v0, 0x0\n    return-wide v0"
    elif return_type == "F":
        ret_instruction = "    const/4 v0, 0x0\n    return v0"
    elif return_type == "D":
        ret_instruction = "    const-wide/16 v0, 0x0\n    return-wide v0"
    else:
        ret_instruction = "    const/4 v0, 0x0\n    return-object v0"

    lines.insert(insert_after + 1, ret_instruction)
    lines.insert(insert_after + 1, "    # [PATCHED by patch_dialog.py — skip dialog]")
    return "\n".join(lines)


def method_looks_like_dialog(method_text):
    """Heuristic: method này có vẻ show dialog không?"""
    indicators = [
        "->show()V",                           # Dialog.show()
        "->setContentView(",                   # Dialog/Activity set layout
        "AlertDialog",
        "Landroid/app/Dialog",
        "Landroid/app/AlertDialog",
        "Ldialog",
        "Dialog;->",
        "->create()Landroid/app/AlertDialog",
        "->create()Landroid/app/Dialog",
        "setView(",                            # AlertDialog.Builder.setView()
    ]
    for ind in indicators:
        if ind in method_text:
            return True
    return False


def method_has_edit_texts(method_text):
    """Method này thao tác EditText không?"""
    return ("EditText" in method_text or
            "->setText(" in method_text or
            "->getText(" in method_text)


def method_is_dialog_show(method_text):
    """Method này CÓ gọi show()V không?"""
    return "->show()V" in method_text


def patch_strategy_b_dragonboy11(files):
    """
    Strategy B: DragonBoy11 smali → tìm method gọi Dialog.show() → return-void đầu method
    """
    global patched
    for fpath in files:
        with open(fpath, encoding="utf-8", errors="ignore") as f:
            content = f.read()

        methods = extract_methods(content)
        new_content = content

        for method_sig, start, end, method_text in methods:
            if not method_is_dialog_show(method_text):
                continue
            # Chỉ patch nếu trông giống dialog setup (có EditText hoặc AlertDialog)
            if not (method_looks_like_dialog(method_text) or method_has_edit_texts(method_text)):
                # Vẫn patch nếu có show() — nhưng chỉ sau khi log
                pass

            print(f"  [B] Patch method '{method_sig}' trong {os.path.basename(fpath)}")
            print(f"      Reason: calls show()V + dialog indicators")
            patched_method = patch_add_return_void_at_top(method_text)
            new_content = new_content.replace(method_text, patched_method)
            patched += 1

        if new_content != content:
            with open(fpath, "w", encoding="utf-8") as f:
                f.write(new_content)
            print(f"  [B] ✅ Saved {os.path.basename(fpath)}")


def patch_strategy_c_activity_oncreate(files):
    """
    Strategy C: Nếu DragonBoy11 có Activity → tìm onCreate/onStart → thêm skip dialog call
    (Chỉ dùng khi B không tìm thấy gì)
    """
    global patched
    for fpath in files:
        with open(fpath, encoding="utf-8", errors="ignore") as f:
            content = f.read()

        # Kiểm tra xem file có extend Activity không
        if not ("Landroid/app/Activity" in content or
                "Landroid/app/Fragment" in content or
                ".super Landroid/app/Activity" in content):
            continue

        methods = extract_methods(content)
        new_content = content

        for method_sig, start, end, method_text in methods:
            # Tìm onCreate, onStart, onResume của Activity
            if not any(m in method_sig for m in [
                "onCreate(Landroid/os/Bundle;)V",
                "onStart()V",
                "onResume()V",
            ]):
                continue

            if method_is_dialog_show(method_text):
                print(f"  [C] Activity method '{method_sig}' tạo dialog trong {os.path.basename(fpath)}")
                patched_method = patch_add_return_void_at_top(method_text)
                new_content = new_content.replace(method_text, patched_method)
                patched += 1

        if new_content != content:
            with open(fpath, "w", encoding="utf-8") as f:
                f.write(new_content)
            print(f"  [C] ✅ Saved {os.path.basename(fpath)}")


def patch_strategy_d_any_smali_show(all_files, dragonboy_files):
    """
    Strategy D: Fallback — scan tất cả smali tìm method có show()V
    gần AlertDialog hoặc EditText, ưu tiên các smali KHÔNG phải unity3d/player.
    """
    global patched
    dragonboy_set = set(dragonboy_files)
    unity_player_skip = ["com/unity3d/player/U.smali",
                         "com/unity3d/player/S.smali",
                         "com/unity3d/player/P.smali"]

    for fpath in all_files:
        if fpath in dragonboy_set:
            continue  # đã xử lý ở B/C
        if any(skip in fpath for skip in unity_player_skip):
            continue  # unity internal keyboard dialogs

        with open(fpath, encoding="utf-8", errors="ignore") as f:
            content = f.read()

        # Chỉ quan tâm file có cả Dialog + EditText
        if not (method_is_dialog_show(content) and method_has_edit_texts(content)):
            continue

        # Loại bỏ file unity player nội bộ
        if "com/unity3d/player" in fpath and "DefaultCompany" not in fpath:
            continue

        methods = extract_methods(content)
        new_content = content

        for method_sig, start, end, method_text in methods:
            if not (method_is_dialog_show(method_text) and method_looks_like_dialog(method_text)):
                continue

            print(f"  [D] Fallback patch: '{method_sig}' in {os.path.relpath(fpath, SMALI_DIR)}")
            patched_method = patch_add_return_void_at_top(method_text)
            new_content = new_content.replace(method_text, patched_method)
            patched += 1

        if new_content != content:
            with open(fpath, "w", encoding="utf-8") as f:
                f.write(new_content)
            print(f"  [D] ✅ Saved {os.path.relpath(fpath, SMALI_DIR)}")


def patch_strategy_e_hardcode_server(files):
    """
    Strategy E: Trong DragonBoy11, tìm method đọc text từ EditText (IP/Port nhập tay)
    → chèn hardcode TARGET_IP và TARGET_PORT trước khi đọc.
    Hướng dẫn: thay const-string chứa IP/Port default bằng 127.0.0.1:15000.
    """
    for fpath in files:
        with open(fpath, encoding="utf-8", errors="ignore") as f:
            content = f.read()

        new_content = content

        # Pattern: const-string vX, "14.225...." → đổi thành 127.0.0.1
        for old_ip in ["14.225.203.242", "14.225.203.243", "14.225.203.244",
                       "103.90.224", "103.90.225"]:
            if old_ip in content:
                new_content = new_content.replace(
                    f'const-string v0, "{old_ip}"',
                    f'const-string v0, "{TARGET_IP}"'
                )
                new_content = new_content.replace(
                    f'const-string v1, "{old_ip}"',
                    f'const-string v1, "{TARGET_IP}"'
                )
                new_content = new_content.replace(
                    f'const-string v2, "{old_ip}"',
                    f'const-string v2, "{TARGET_IP}"'
                )

        # Đổi port 14445 → 15000
        new_content = re.sub(r'const-string (v\d+), "14445"',
                             r'const-string \1, "15000"', new_content)
        new_content = re.sub(r'const/16 (v\d+), 0x386[0-9]?',  # 0x3875 = 14453, 0x3870 = 14448
                             r'const/16 \1, 0x3A98', new_content)  # 0x3A98 = 15000
        new_content = re.sub(r'const/4 (v\d+), 0x3875',
                             r'const/4 \1, 0x3A98', new_content)

        if new_content != content:
            with open(fpath, "w", encoding="utf-8") as f:
                f.write(new_content)
            print(f"  [E] ✅ Hardcoded IP/Port trong {os.path.basename(fpath)}")


# ═══════════════════════════════════════════════════════════
# MAIN
# ═══════════════════════════════════════════════════════════
print(f"{'='*60}")
print(f"patch_dialog.py v3.0 — SMALI_DIR: {SMALI_DIR}")
print(f"{'='*60}")

# 1. Tìm DragonBoy11 smali
db11_files = find_dragonboy11_files()
print(f"\n[A] Tìm DragonBoy11 smali: {len(db11_files)} files")

if db11_files:
    for fpath in sorted(db11_files):
        print(f"\n{'─'*55}")
        print(f"=== DragonBoy11/{os.path.basename(fpath)} ===")
        with open(fpath, encoding="utf-8", errors="ignore") as f:
            content = f.read()
        print(content[:4000])
        if len(content) > 4000:
            print(f"... [{len(content)-4000} bytes truncated — xem full ở artifact]")
        print(f"{'─'*55}")
else:
    print("  ⚠ Không tìm thấy com/DefaultCompany/DragonBoy11/ smali")
    print("  → Tìm kiếm theo tên package khác...")
    # Thử tên khác (đôi khi apktool decompile thành tên khác)
    for root, dirs, fnames in os.walk(SMALI_DIR):
        for f in fnames:
            if f.endswith(".smali") and "DefaultCompany" in root:
                print(f"  Found: {os.path.join(root, f)}")
                db11_files.append(os.path.join(root, f))

# 2. Strategy B: patch dialog show() trong DragonBoy11
print(f"\n[B] Strategy B: patch Dialog.show() trong DragonBoy11")
if db11_files:
    patch_strategy_b_dragonboy11(db11_files)
    if patched == 0:
        print("  Không tìm thấy show()V trong DragonBoy11 — thử Strategy C")

# 3. Strategy C: Activity.onCreate trong DragonBoy11
if patched == 0:
    print(f"\n[C] Strategy C: patch Activity.onCreate trong DragonBoy11")
    if db11_files:
        patch_strategy_c_activity_oncreate(db11_files)

# 4. Strategy D: Fallback scan tất cả smali
if patched == 0:
    print(f"\n[D] Strategy D: fallback scan toàn bộ smali")
    all_files = find_all_smali()
    print(f"  Total smali files: {len(all_files)}")
    patch_strategy_d_any_smali_show(all_files, db11_files)

# 5. Strategy E: Hardcode IP/Port trong DragonBoy11
if db11_files:
    print(f"\n[E] Strategy E: hardcode IP/Port trong DragonBoy11")
    patch_strategy_e_hardcode_server(db11_files)

print(f"\n{'='*60}")
print(f"Kết quả: {patched} method(s) patched")
if patched > 0:
    print("✅ Dialog đã bị vô hiệu hóa ở tầng smali")
else:
    print("⚠  Không tìm thấy dialog smali — AutoFill sẽ xử lý lúc runtime")
    print("   → Kiểm tra log bên trên để xem nội dung DragonBoy11 smali")
print(f"{'='*60}")
