package ru.android.mtsinfowidget;

import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import ru.android.mtsinfowidget.objects.H2OProfile;
import ru.android.mtsinfowidget.objects.LkHeader;

/**
 * Created by vasiliev on 25.12.15.
 */
public class WorkService extends IntentService {
    private static final String SERVICE_NAME = WorkService.class.getName();

    private static final String TAG = WorkService.class.getSimpleName();

    public WorkService() {
        super(SERVICE_NAME);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate()");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "onHandleIntent(" + intent + ")");
        MtsApi mtsApi = MtsApi.getInstance();
        if (mtsApi.updateProfileInfo()) {
            LkHeader header = mtsApi.getLkHeader();
            H2OProfile h2o = mtsApi.getH2OProfile();

            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(WorkService.this
                    .getApplicationContext());

            ComponentName infoWidget = new ComponentName(getApplicationContext(), InfoWidgetProvider.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(infoWidget);

            for (int i = 0; i < appWidgetIds.length; ++i) {
                RemoteViews rv = new RemoteViews(getPackageName(), R.layout.info_widget);
                rv.setTextViewText(R.id.name_text, header.getName());
                rv.setTextViewText(R.id.balance_text, Float.toString(header.getBalance()));
                //rv.setTextViewText(R.id.region_text, h2o.getRoaming() + "/" + h2o.getRegionId());
                rv.setTextViewText(R.id.number_text,
                        String.format(Locale.getDefault(), "+%d", h2o.getNumber()));
                rv.setTextViewText(R.id.traffic_text, String.format(Locale.getDefault(), "%d/%d Мб",
                        (h2o.getConsumedQuotaSize() / 1024), (h2o.getSharedQuotaSize() / 1024)));
                rv.setTextViewText(R.id.last_update_text,
                        String.format(Locale.getDefault(), "последнее обновление в %s",
                                new SimpleDateFormat("HH:mm").format(new Date())));

                /*
                // Create an Intent to launch SettingsActivity
                Intent settingsIntent = new Intent(WorkService.this, SettingsActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(WorkService.this, 0, settingsIntent, 0);
                rv.setOnClickPendingIntent(R.id.widget_layout, pendingIntent);
                */

                appWidgetManager.updateAppWidget(appWidgetIds[i], rv);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");
    }
}
