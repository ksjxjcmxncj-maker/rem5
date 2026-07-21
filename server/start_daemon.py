#!/usr/bin/env python3
"""
NRO Game Server — Python double-fork daemon
Dùng để start Java server không chết khi SSH đóng.

Cách dùng:
  DISPLAY=:99 python3 start_daemon.py <classpath> [--log /path/to/log]

Ví dụ:
  DISPLAY=:99 python3 start_daemon.py "NgocRongOnline.jar:lib/*" --log ~/logs/server.log
"""
import os, sys, subprocess, time, signal

def double_fork_daemon(cmd, log_path, cwd=None):
    """
    Double-fork để tạo orphan process — hoàn toàn tách khỏi terminal/SSH.
    Sau khi fork lần 2, process con trở thành con của init (PID 1).
    """
    # Fork lần 1
    pid = os.fork()
    if pid > 0:
        # Parent: chờ fork 1 exit rồi return
        os.waitpid(pid, 0)
        return

    # Child 1: tạo session mới (tách khỏi controlling terminal)
    os.setsid()

    # Fork lần 2 — đảm bảo không thể re-acquire terminal
    pid2 = os.fork()
    if pid2 > 0:
        # Child 1 thoát ngay → Child 2 trở thành orphan (con của init)
        os._exit(0)

    # Child 2: đây là daemon thực sự
    os.chdir(cwd or os.getcwd())
    os.umask(0o022)

    # Redirect stdin/stdout/stderr
    with open(log_path, 'a') as logf:
        os.dup2(open(os.devnull, 'r').fileno(), sys.stdin.fileno())
        os.dup2(logf.fileno(), sys.stdout.fileno())
        os.dup2(logf.fileno(), sys.stderr.fileno())

    # Ghi PID ra file
    pid_file = os.path.join(os.path.dirname(log_path), '.server.pid')
    with open(pid_file, 'w') as f:
        f.write(str(os.getpid()))

    print(f"[daemon] PID={os.getpid()} CMD={' '.join(cmd)}", flush=True)

    # Exec Java server — thay thế process này bằng Java
    os.execvp(cmd[0], cmd)


def find_jar():
    """Tìm JAR game server trong thư mục hiện tại và ~/nro/"""
    search_dirs = [
        os.getcwd(),
        os.path.expanduser('~/nro/SRC'),
        os.path.expanduser('~/nro'),
    ]
    for d in search_dirs:
        if not os.path.isdir(d):
            continue
        for f in os.listdir(d):
            if f.endswith('.jar') and 'login' not in f.lower():
                return os.path.join(d, f)
    return None


def ensure_xvfb():
    """Khởi động Xvfb :99 nếu chưa chạy."""
    result = subprocess.run(['pgrep', '-x', 'Xvfb'], capture_output=True)
    if result.returncode == 0:
        print("[xvfb] Xvfb đã chạy rồi", flush=True)
        return
    try:
        subprocess.Popen(
            ['Xvfb', ':99', '-screen', '0', '1024x768x24'],
            stdout=open(os.path.expanduser('~/logs/xvfb.log'), 'a'),
            stderr=subprocess.STDOUT,
            start_new_session=True,
        )
        time.sleep(2)
        print("[xvfb] Xvfb :99 đã start", flush=True)
    except FileNotFoundError:
        print("[xvfb] Xvfb không có — dùng headless mode", flush=True)


if __name__ == '__main__':
    import argparse
    parser = argparse.ArgumentParser(description='NRO double-fork daemon')
    parser.add_argument('classpath', nargs='?', help='Java classpath (mặc định: tự tìm JAR)')
    parser.add_argument('--log', default=os.path.expanduser('~/logs/server.log'),
                        help='Đường dẫn log file')
    parser.add_argument('--cwd', default=None, help='Working directory')
    parser.add_argument('--xms', default='256m', help='Java -Xms (mặc định: 256m)')
    parser.add_argument('--xmx', default='1g',  help='Java -Xmx (mặc định: 1g)')
    args = parser.parse_args()

    # Đảm bảo thư mục log tồn tại
    os.makedirs(os.path.dirname(args.log), exist_ok=True)

    # Đảm bảo DISPLAY set
    os.environ.setdefault('DISPLAY', ':99')
    ensure_xvfb()

    # Tìm classpath nếu chưa cung cấp
    cp = args.classpath
    if not cp:
        jar = find_jar()
        if not jar:
            print("[ERR] Không tìm thấy JAR. Truyền classpath thủ công.", file=sys.stderr)
            sys.exit(1)
        lib_dir = os.path.join(os.path.dirname(jar), 'lib')
        cp = jar
        if os.path.isdir(lib_dir):
            cp = f"{jar}:{lib_dir}/*"

    cwd = args.cwd or os.path.dirname(cp.split(':')[0]) or os.getcwd()

    java_cmd = [
        'java',
        f'-Xms{args.xms}',
        f'-Xmx{args.xmx}',
        '-XX:+UseG1GC',
        '-XX:MaxGCPauseMillis=30',
        '-XX:G1HeapRegionSize=4m',
        '-XX:+ParallelRefProcEnabled',
        '-Djava.net.preferIPv4Stack=true',
        '-Djava.awt.headless=true',
        '-cp', cp,
        'Main',
    ]

    print(f"[start_daemon] Launching: {' '.join(java_cmd)}", flush=True)
    print(f"[start_daemon] DISPLAY={os.environ.get('DISPLAY', ':99')}", flush=True)
    print(f"[start_daemon] Log: {args.log}", flush=True)

    double_fork_daemon(java_cmd, args.log, cwd=cwd)
    print("[start_daemon] Daemon launched ✅ (process sẽ chạy độc lập)", flush=True)
