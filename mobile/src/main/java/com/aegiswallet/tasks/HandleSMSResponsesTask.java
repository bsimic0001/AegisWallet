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
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;

import com.aegiswallet.R;
import com.aegiswallet.listeners.SMSTaskCompletedListener;
import com.aegiswallet.objects.SMSTransactionPojo;
import com.aegiswallet.utils.BasicUtils;
import com.aegiswallet.utils.Constants;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by bsimic on 3/19/14.
 */
public class HandleSMSResponsesTask extends BaseTask {

    private String TAG = this.getClass().getName();

    private Context context;
    private ArrayList<SMSTransactionPojo> smsTransactionPojos;
    private ArrayList<String> pendingNumbers;
    private SharedPreferences prefs;

    public HandleSMSResponsesTask(Context context) {
        this.context = context;
    }

    @Override
    protected void onPreExecute() {
    }

    @Override
    protected Object doInBackground(Object[] objects) {
        prefs = context.getSharedPreferences(
                context.getString(R.string.sms_transaction_filename),
                Context.MODE_PRIVATE);

        smsTransactionPojos = BasicUtils.getAllNotRespondedSMSTransactions(prefs);
        pendingNumbers = new ArrayList<String>();

        if(smsTransactionPojos.size() > 0){
            checkSMSMessages(smsTransactionPojos);
        }

        return null;
    }

    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);
        ((SMSTaskCompletedListener) context).onSMSTaskCompleted();
    }

    private long getOldestTimestamp(ArrayList<SMSTransactionPojo> pojos){
        long oldest = 0;

        for(SMSTransactionPojo pojo : pojos){
            pendingNumbers.add(pojo.getPhoneNumber().replaceAll("[^0-9]", ""));
            if(pojo.getTimestamp() > oldest){
                oldest = pojo.getTimestamp();
            }
        }

        return oldest;
    }

    private void checkSMSMessages(ArrayList<SMSTransactionPojo> pojos) {
        SharedPreferences smsPrefs = context.getSharedPreferences(context.getString(R.string.sms_transaction_filename), Context.MODE_PRIVATE);

        final Uri SMS_Inbox = Uri.parse("content://sms/inbox");
        String sDirection = "1";
        String sMessageType = "0";
        String SMS_READ_COLUMN = "read";
        String SORT_ORDER = " _id DESC";
        int count = 0;
        Cursor cursor;
        int iLastIDRun = 0;
        String sDetail;

        cursor = context.getContentResolver().query(
                SMS_Inbox,
                new String[]{"_id", "thread_id", "address", "person", "date", "body"},
                " _id > " + String.valueOf(iLastIDRun)
                + " AND date >= " + getOldestTimestamp(pojos),
                null,
                SORT_ORDER);
        sMessageType = "1";

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    String body = "";
                    do {
                        count = count + 1;
                        if (count > count) {
                            break;
                        }
                        long messageId = cursor.getLong(0);
                        long threadId = cursor.getLong(1);
                        String address = cursor.getString(2);
                        long contactId = cursor.getLong(3);
                        String contactId_string = String.valueOf(contactId);
                        long timestamp = cursor.getLong(4);
                        String sBody = cursor.getString(5);
                        sDetail = "";

                        String sReturn = "";

                        if (address.startsWith("1")) {
                            address = address.substring(1);
                        }

                        String btcAddress = BasicUtils.getAddressFromMessage(sBody);
                        String number = new String(address.replaceAll("[^0-9]",""));
                        String pendingNumberToAdjust = doesPendingNumbersContainNumber(number);

                        if(pendingNumberToAdjust != null && btcAddress != null){
                            //Check if the text has a valid address and then
                            //if it does, modify the preferences item and change the status.
                            String encodedPojo = smsPrefs.getString(pendingNumberToAdjust, null);
                            SMSTransactionPojo decodePojo = new SMSTransactionPojo(encodedPojo);

                            decodePojo.setBtcAddress(btcAddress);
                            decodePojo.setStatus(Constants.SMS_STATUS_REC);
                            String json64 = decodePojo.getJSONBase64();
                            smsPrefs.edit().putString(pendingNumberToAdjust, json64).commit();
                        }
                    }
                    while (cursor.moveToNext());

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            cursor.close();
        }
    }

    public String doesPendingNumbersContainNumber(String phoneNumber){

        for(String number : pendingNumbers){
            if(number.contains(phoneNumber) || phoneNumber.contains(number)){
                return number;
            }
        }

        return null;
    }


}
