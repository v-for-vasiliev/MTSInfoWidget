package ru.android.mtsinfowidget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Calendar;

/**
 * Created by vasiliev on 24.12.15.
 */
public class InfoWidgetProvider extends AppWidgetProvider {
    private static final String TAG = InfoWidgetProvider.class.getSimpleName();

    private PendingIntent mWorkService = null;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        Log.d(TAG, "onUpdate()");

        final AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        final Calendar alarmTime = Calendar.getInstance();
        alarmTime.set(Calendar.MINUTE, 0);
        alarmTime.set(Calendar.SECOND, 0);
        alarmTime.set(Calendar.MILLISECOND, 0);

        final Intent intent = new Intent(context, WorkService.class);

        if (mWorkService == null) {
            mWorkService = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        }

        int updateInterval = context.getResources().getInteger(R.integer.update_interval_ms);

        // Set the alarm type to either ELAPSED_REALTIME or RTC, which will only deliver the alarm when the device is awake
        am.setRepeating(AlarmManager.RTC, alarmTime.getTime().getTime(), updateInterval, mWorkService);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        Log.d(TAG, "onDeleted()");
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        Log.d(TAG, "onEnabled()");
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        Log.d(TAG, "onDisabled()");

        final AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (mWorkService != null) {
            am.cancel(mWorkService);
        }
    }
}
