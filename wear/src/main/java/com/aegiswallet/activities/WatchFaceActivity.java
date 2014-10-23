package com.aegiswallet.activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.widget.TextView;

import com.aegiswallet.R;

import java.util.Calendar;

public class WatchFaceActivity extends Activity {

    private final static IntentFilter intentFilter;
    private boolean isDimmed = false;

    private String TAG = "AegisWearWatchFace";

    private Handler mHandler;

    TextView time;
    TextView timeAmPm;
    TextView btcValue;
    TextView walletBalance;

    SharedPreferences prefs;

    static {
        intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_TIME_TICK);
        intentFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        intentFilter.addAction(Intent.ACTION_TIME_CHANGED);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "inside the watch face activity");
        WatchViewStub stub = new WatchViewStub(this);
        stub.setRectLayout(R.layout.rect_activity_watch_face);
        stub.setRoundLayout(R.layout.round_activity_watch_face);

        stub.requestApplyInsets();


        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(final WatchViewStub stub) {
                Log.d(TAG, "Layout is inflated!");
                time = (TextView) stub.findViewById(R.id.time);
                timeAmPm = (TextView) stub.findViewById(R.id.time_ampm);
                btcValue = (TextView) stub.findViewById(R.id.bitcoin_value);
                walletBalance = (TextView) stub.findViewById(R.id.wallet_balance);

                View rootView = stub.getRootView();

                rootView.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                    @Override
                    public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                        stub.onApplyWindowInsets(insets);
                        final boolean round = insets.isRound();
                        Log.d("WatchFaceActivity", "Is the screen round: " + round);
                        return insets;
                    }
                });
            }
        });

        setContentView(stub);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        timeInfoReceiver.onReceive(this, registerReceiver(null, intentFilter));
        registerReceiver(timeInfoReceiver, intentFilter);
    }

    public BroadcastReceiver timeInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context arg0, Intent intent) {
            Log.v("WatchFace", "timeChanged();");
            updateLayout();
        }
    };


    @Override
    protected void onPause() {
        super.onPause();
        isDimmed = true;
        Log.d(TAG, "dimmed");
        updateLayout();
    }

    @Override
    protected void onResume() {
        super.onResume();
        isDimmed = false;
        Log.d(TAG, "not dimmed");
        updateLayout();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(timeInfoReceiver);
    }

    public void updateLayout() {

        Calendar calendar = Calendar.getInstance();

        int hour = calendar.get(Calendar.HOUR);
        int minute = calendar.get(Calendar.MINUTE);
        String am_pm;

        if(calendar.get(Calendar.AM_PM) == 0)   am_pm = "AM";   else   am_pm = "PM";

        String btcValueString = prefs.getString("BTCAMOUNT", "");

        String hourText = hour + "";

        if(hour == 0)
           hour = 12;

        if(hour < 10)
            hourText = "0" + hourText;

        String minuteText = minute + "";
        if(minute < 10)
            minuteText = "0" + minuteText;

        if(time != null) {
            time.setText(hourText + ":" + minuteText);
            timeAmPm.setText(am_pm);
            btcValue.setText(btcValueString);
            walletBalance.setText(prefs.getString("BALANCE", ""));
        }
        else {
            Log.d("WatchFace", "time is null for some reason...");
        }

    }
}
