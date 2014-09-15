/*
 * Aegis Bitcoin Wallet - The secure Bitcoin wallet for Android
 * Copyright 2014 Bojan Simic and specularX.co, designed by Reuven Yamrom
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.aegiswallet.tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.aegiswallet.PayBitsApplication;
import com.aegiswallet.utils.Constants;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpParams;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by bsimic on 5/6/14.
 */
public class SendShamirValueTask extends AsyncTask {

    HttpClient httpclient;
    HttpPost httppost;
    private String x3Value;
    private Context context;
    private PayBitsApplication application;

    private String TAG = this.getClass().getName();

    public SendShamirValueTask(Context context, String x3Value, PayBitsApplication application){
        this.context = context;
        this.x3Value = x3Value;
        this.application = application;
    }

    @Override
    protected Object doInBackground(Object[] objects) {

        httpclient = new DefaultHttpClient();
        httppost = new HttpPost(Constants.AEGIS_SITE);

        Log.d(TAG, "sending shamir");
        try {
            // Add your data

            TelephonyManager tManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            String uid = tManager.getDeviceId();

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair("device", uid));
            nameValuePairs.add(new BasicNameValuePair("message", x3Value));
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            // Execute HTTP Post Request
            HttpResponse response = httpclient.execute(httppost);
            int statusCode = response.getStatusLine().getStatusCode();

            Log.d(TAG, "response for shamir: " + statusCode);


            if(statusCode != 200){
                //TODO: did not succeed
            }
            else{
                //Means we have successfully exported the key to BitcoinSecurityProject.org
                application.getPrefs().edit().remove(Constants.SHAMIR_EXPORTED_KEY).commit();
            }

        } catch (ClientProtocolException e) {
            Log.d(TAG, e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, e.getMessage());
        }
        return null;
    }

    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);
    }
}
