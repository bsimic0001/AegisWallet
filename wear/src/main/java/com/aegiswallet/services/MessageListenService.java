package com.aegiswallet.services;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.aegiswallet.activities.MyActivity;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by HyperCorp on 8/26/14.
 */
public class MessageListenService extends WearableListenerService {

    private String TAG = "TAVONWear";
    //    private LFXNetworkContext networkContext;
    private GoogleApiClient mGoogleApiClient;

    @Override
    public void onCreate() {
        super.onCreate();

        //  Needed for communication between watch and device.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Log.d(TAG, "onConnected: " + connectionHint);
                    }

                    @Override
                    public void onConnectionSuspended(int cause) {
                        Log.d(TAG, "onConnectionSuspended: " + cause);
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.d(TAG, "onConnectionFailed: " + result);
                    }
                })
                .addApi(Wearable.API)
                .build();

        mGoogleApiClient.connect();
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


        if(messageEvent.getPath().equals("MessageAddress")) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            preferences.edit().putString("ADDRESS", new String(messageEvent.getData())).commit();

        }
        else if(messageEvent.getPath().equals("MessageBalance")){
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            String message = new String(messageEvent.getData());
            preferences.edit().putString("BALANCE", message).commit();
        }
        else if(messageEvent.getPath().equals("MessageBitcoinValue")){
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            preferences.edit().putString("BTCAMOUNT", new String(messageEvent.getData())).commit();
        }

        Intent mainIntent = new Intent(getBaseContext(), MyActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mainIntent.putExtra("type", messageEvent.getPath());
        mainIntent.putExtra("data", new String(messageEvent.getData()));
        startActivity(mainIntent);


    }
}