package com.example.an_droids;

import java.io.*;

/**
 * Utility class providing methods for handling file I/O operations.
 * This includes methods for converting files to byte arrays and writing byte arrays to files.
 */
public class Utils {

    /**
     * Converts a file to a byte array.
     * Reads the contents of the given file and returns it as a byte array.
     *
     * @param file The file to convert to a byte array.
     * @return A byte array containing the contents of the file.
     * @throws IOException If an I/O error occurs while reading the file.
     */
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

    /**
     * Writes the given byte array to a file.
     * This method writes the provided byte array to the specified file, overwriting
     * the file if it already exists.
     *
     * @param data The byte array containing the data to write to the file.
     * @param file The file to write the byte array to.
     * @throws IOException If an I/O error occurs while writing to the file.
     */
    public static void writeBytesToFile(byte[] data, File file) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
        }
    }
}
