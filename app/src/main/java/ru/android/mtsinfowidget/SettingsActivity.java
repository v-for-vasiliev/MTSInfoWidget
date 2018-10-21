package ru.android.mtsinfowidget;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import ru.android.mtsinfowidget.objects.H2OProfile;
import ru.android.mtsinfowidget.objects.LkHeader;

public class SettingsActivity extends Activity {
    private static final String TAG = SettingsActivity.class.getSimpleName();

    MtsApi mMtsApi = null;

    private Button mRunButton = null;
    private TextView mOutputView = null;

    private BroadcastReceiver mProfileInfoUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.ACTION_MTS_INFO_UPDATED)) {
                LkHeader header = mMtsApi.getLkHeader();
                H2OProfile H2O = mMtsApi.getH2OProfile();
                if (header != null && H2O != null) {
                    mOutputView.setText(header + "\n" + H2O);
                }
                Toast.makeText(SettingsActivity.this, "info updated", Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mMtsApi = MtsApi.getInstance();
        mRunButton = (Button) findViewById(R.id.test_button);
        mOutputView = (TextView) findViewById(R.id.output);

        mRunButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMtsApi.updateProfileInfoAsync(SettingsActivity.this);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupReceiver();
    }

    @Override
    protected void onPause() {
        super.onPause();
        removeReceiver();
    }

    private void setupReceiver() {
        IntentFilter filter = new IntentFilter(Constants.ACTION_MTS_INFO_UPDATED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mProfileInfoUpdateReceiver, filter);
    }

    private void removeReceiver() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mProfileInfoUpdateReceiver);
    }
}
