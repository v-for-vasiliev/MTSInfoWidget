package ru.android.mtsinfowidget.objects;

import android.util.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by vasiliev on 23.12.15.
 */
public class LkHeader {
    private static final String TAG = LkHeader.class.getSimpleName();

    private static final String NAME_PATTERN = "<div class=\"b-header_lk__name\">(.*?)</div>";
    private static final String BALANCE_PATTERN = "Баланс: <span class=\"\"><b>(.*?)</b>";

    private String mData = null;

    private String mName = null;
    private float mBalance = 0.0f;

    private LkHeader(String data) {
        mData = data;
        mName = parseName();
        mBalance = parseBalance();
    }

    public static final LkHeader create(String data) {
        if (data == null) {
            Log.d(TAG, "Error creating LK Header object: null data");
            return null;
        } else {
            return new LkHeader(data);
        }
    }

    private String parseName() {
        Pattern pattern = Pattern.compile(NAME_PATTERN);
        Matcher matcher = pattern.matcher(mData);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private float parseBalance() {
        Pattern pattern = Pattern.compile(BALANCE_PATTERN);
        Matcher matcher = pattern.matcher(mData);
        if (matcher.find()) {
            String match = matcher.group(1);
            try {
                return Float.parseFloat(match);
            } catch (NumberFormatException ignore) {
            }
        }
        return 0.0f;
    }

    public String getName() {
        return mName;
    }

    public float getBalance() {
        return mBalance;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("LK Header Object:\n");
        sb.append("Name: " + mName + "\n");
        sb.append("Balance: " + mBalance + "\n");
        return sb.toString();
    }
}
