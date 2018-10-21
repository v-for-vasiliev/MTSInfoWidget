package ru.android.mtsinfowidget;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

/**
 * Created by Vasiliev on 07.10.2015.
 */
public class BroadcastManager {


    public static void broadcastMtsInfoUpdated(Context c) {
        Intent intent = new Intent(Constants.ACTION_MTS_INFO_UPDATED);
        // Put some extra
        //intent.putExtra(null, null);
        LocalBroadcastManager.getInstance(c).sendBroadcast(intent);
    }
}
