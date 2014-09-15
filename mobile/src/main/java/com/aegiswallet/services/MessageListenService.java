package com.aegiswallet.services;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.aegiswallet.PayBitsApplication;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by HyperCorp on 8/26/14.
 */
public class MessageListenService extends WearableListenerService {

    private String TAG = "WearServiceMessageListener";
    private GoogleApiClient mGoogleApiClient;
    private PayBitsApplication application;

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "Inside message service");

        application = (PayBitsApplication) getApplication();
        mGoogleApiClient = application.getmGoogleApiClient();

        if (null != mGoogleApiClient && !mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
    }

    /**
     * Here, the device actually receives the message that the phone sent, as a path.
     * We simply check that path's last segment and act accordingly.
     *
     * @param messageEvent
     */
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

        Log.v(TAG, "msg rcvd");
        Log.v(TAG, messageEvent.getPath());

        Intent intent;

        Log.d(TAG, "GOT MESSAGE: " + new String(messageEvent.getData()));
        intent = new Intent("velocity-event");
        intent.putExtra("message", new String(messageEvent.getData()));
        LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(intent);

    }

}