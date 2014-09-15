package com.aegiswallet.tasks;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.HashSet;

/**
 * Created by HyperCorp on 8/26/14.
 */
public class SendMessageTask extends AsyncTask<String, Void, String> {

    private String TAG = "SendMessageTask";

    private GoogleApiClient mGoogleApiClient;
    private MessageApi.SendMessageResult result;
    private String type;
    private String data;
    private SharedPreferences prefs;

    public SendMessageTask(GoogleApiClient mGoogleApiClient, String type, String data, SharedPreferences prefs ){
        this.mGoogleApiClient = mGoogleApiClient;
        this.type = type;
        this.data = data;
        this.prefs = prefs;
    }

    @Override
    protected String doInBackground(String... params) {

        sendMessageToNodes();

        /*
        String node = getFirstNode();

        if(node != null) {
            result = Wearable.MessageApi.sendMessage(
                    mGoogleApiClient, node, type, data.getBytes()).await();
        }
        */

        return null;
    }

    private String getFirstNode(){
        HashSet<String> results= new HashSet<String>();
        NodeApi.GetConnectedNodesResult nodes =
                Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
        for (Node node : nodes.getNodes()) {
            return node.getId();
        }

        return null;
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);

        if (result != null && !result.getStatus().isSuccess()) {
            Log.e(TAG, "ERROR: failed to send Message: " + result.getStatus());
        }
        else if(result != null && result.getStatus().isSuccess()){
            Log.d(TAG, "Message sent!");
        }
        else {
            Log.d(TAG, "Message not sent....result is null");
        }
    }

    private String sendMessageToNodes(){
        HashSet<String> results= new HashSet<String>();
        NodeApi.GetConnectedNodesResult nodes =
                Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
        for (Node node : nodes.getNodes()) {
            Log.d(TAG, "Sending message to... " + node.getId());
            sendMessageToNode(node.getId());
        }

        return null;
    }

    private void sendMessageToNode(String node){
        if(node != null && data != null) {
            MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                    mGoogleApiClient, node, type,
                    data.getBytes()).await();

            if(result.getStatus().isSuccess()){
                Log.d(TAG, "message is successful");
            }
            else{
                Log.d(TAG, "message NOT successful");
            }
        }
    }

}
