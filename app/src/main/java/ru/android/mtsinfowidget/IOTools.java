package ru.android.mtsinfowidget;

import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

/**
 * Created by vasiliev on 22.12.15.
 */
public class IOTools {
    public static final String TAG = IOTools.class.getSimpleName();

    public static String ungzipString(byte[] compressedString) throws IOException {
        final int BUFFER_SIZE = 256;
        ByteArrayInputStream is = new ByteArrayInputStream(compressedString);
        GZIPInputStream gis = new GZIPInputStream(is, BUFFER_SIZE);
        StringBuilder string = new StringBuilder();
        byte[] data = new byte[BUFFER_SIZE];
        int bytesRead;
        while ((bytesRead = gis.read(data)) != -1) {
            string.append(new String(data, 0, bytesRead));
        }
        gis.close();
        is.close();
        return string.toString();
    }

    public static String readStringFromStream(InputStream source) {
        String result = null;
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(source, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (IOException e) {
            Log.d(TAG, "", e);
            return null;
        }
    }

    public static String ungzipStringFromStream(InputStream source) {
        try {
            GZIPInputStream gzis = new GZIPInputStream(source);
            InputStreamReader reader = new InputStreamReader(gzis);
            BufferedReader in = new BufferedReader(reader);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (IOException e) {
            Log.d(TAG, "", e);
            return null;
        }
    }
}
