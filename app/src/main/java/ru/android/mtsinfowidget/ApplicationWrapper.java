package ru.android.mtsinfowidget;

import android.app.Application;
import android.content.Context;

/**
 * Created by vasiliev on 15.12.15.
 */
public class ApplicationWrapper extends Application {
    private static Context sAppContext;

    public static Context getAppContext() {
        return sAppContext;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sAppContext = getApplicationContext();
    }
}
