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
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.aegiswallet.utils.Constants;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by bsimic on 3/17/14.
 */
public class GetCurrencyInfoTask extends AsyncTask<String, Void, Void> {

    private Context context;
    private String urlString = Constants.BLOCKCHAIN_CURRENCY_CALL;
    private JSONObject jsonObject;
    private SharedPreferences sharedPreferences;

    public GetCurrencyInfoTask(Context context) {
        this.context = context;
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    protected Void doInBackground(String... strings) {

        if(fileExistance(Constants.BLOCKCHAIN_CURRENCY_FILE_NAME) && !shouldRefreshFile(Constants.BLOCKCHAIN_CURRENCY_FILE_NAME)){
            return null;
        }

        HttpURLConnection urlConnection = null;
        URL url = null;
        jsonObject = null;
        InputStream inStream = null;
        try {
            url = new URL(urlString.toString());
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            urlConnection.connect();
            inStream = urlConnection.getInputStream();
            BufferedReader bReader = new BufferedReader(new InputStreamReader(inStream));
            String temp, response = "";
            while ((temp = bReader.readLine()) != null) {
                response += temp;
            }
            jsonObject = (JSONObject) new JSONTokener(response).nextValue();
        } catch (Exception e) {
        } finally {
            if (inStream != null) {
                try {
                    // this will close the bReader as well
                    inStream.close();
                } catch (IOException ignored) {
                    Log.e("Currency Task", "File Close IO Exception: " + ignored.getMessage());
                }
            }
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        if (jsonObject != null) {

            try {
                FileOutputStream fos = context.getApplicationContext().openFileOutput(Constants.BLOCKCHAIN_CURRENCY_FILE_NAME, Context.MODE_PRIVATE);
                fos.write(jsonObject.toString().getBytes());
                fos.close();
            } catch (IOException e) {
                Log.e("Currency Task", "Cannot save or create file " + e.getMessage());
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);

        //UPDATE views
    }

    public boolean fileExistance(String fname){
        File file = context.getApplicationContext().getFileStreamPath(fname);
        return file.exists();
    }

    public boolean shouldRefreshFile(String fname){
        File file = context.getApplicationContext().getFileStreamPath(fname);

        long msBetweenDates = new Date().getTime() - file.lastModified();
        long minutes = TimeUnit.MILLISECONDS.toMinutes(msBetweenDates);
        if(minutes > 15)
            return true;
        else
            return false;
    }
}
