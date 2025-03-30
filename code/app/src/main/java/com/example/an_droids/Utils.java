package com.example.an_droids;

import java.io.*;

public class Utils {
    public static byte[] fileToByteArray(File file) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             FileInputStream fis = new FileInputStream(file)) {
            byte[] buf = new byte[1024];
            int n;
            while ((n = fis.read(buf)) > -1) {
                baos.write(buf, 0, n);
            }
            return baos.toByteArray();
        }
    }

    public static void writeBytesToFile(byte[] data, File file) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
        }
    }
}
