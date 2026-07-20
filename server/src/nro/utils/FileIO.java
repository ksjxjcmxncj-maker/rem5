package nro.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FileIO {

    private static final Map<String, byte[]> CACHE = new HashMap<>();

    public static byte[] readFile(String url) {
        try {
            byte[] ab = CACHE.get(url);
            if (ab == null) {
                try (FileInputStream fis = new FileInputStream(url)) {
                    ab = new byte[fis.available()];
                    fis.read(ab, 0, ab.length);
                }
            }
            return ab;
        } catch (IOException e) {
            System.err.println("[FileIO] Không đọc được file: " + url + " — " + e.getMessage());
        }
        return null;
    }

    public static ByteArrayOutputStream loadFile(String url) {
        try (FileInputStream openFileInput = new FileInputStream(url)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int read;
            while ((read = openFileInput.read(buf)) != -1) {
                baos.write(buf, 0, read);
            }
            return baos;
        } catch (IOException e) {
            System.err.println("[FileIO] Lỗi loadFile: " + url + " — " + e.getMessage());
            return null;
        }
    }

    public static void writeFile(String url, byte[] ab) {
        // FIX: log lỗi thay vì nuốt im lặng
        File f = new File(url);
        try {
            if (f.exists()) {
                f.delete();
            }
            f.createNewFile();
            try (FileOutputStream fos = new FileOutputStream(f)) {
                fos.write(ab);
                fos.flush();
            }
        } catch (IOException e) {
            System.err.println("[FileIO] Ghi file thất bại: " + url + " — " + e.getMessage());
            e.printStackTrace();
        }
    }
}
