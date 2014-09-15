package com.aegiswallet.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import com.aegiswallet.R;
import com.aegiswallet.actions.AddressScanActivity;
import com.aegiswallet.objects.SMSTransactionPojo;
import com.aegiswallet.utils.BasicUtils;
import com.aegiswallet.utils.Constants;

public class SMSReceiver extends BroadcastReceiver {

    private String TAG = this.getClass().getName();

    private SharedPreferences smsTxnsPrefs;


    public void onReceive(Context context, Intent intent) {

        smsTxnsPrefs = context.getSharedPreferences(context.getString(R.string.sms_transaction_filename), Context.MODE_PRIVATE);

        Bundle pudsBundle = intent.getExtras();
        Object[] pdus = (Object[]) pudsBundle.get("pdus");
        SmsMessage messages = SmsMessage.createFromPdu((byte[]) pdus[0]);
        Log.i(TAG, messages.getMessageBody());

        String phoneNumber = messages.getOriginatingAddress();
        phoneNumber = phoneNumber.replaceAll("[^0-9]","");

        String messageBody = messages.getMessageBody();

        if(phoneNumber != null && smsTxnsPrefs.contains(phoneNumber) && messageBody != null && messageBody.length() > 15){
            String address = BasicUtils.getAddressFromMessage(messageBody);

            if(address != null){
                String valueFromPrefs = smsTxnsPrefs.getString(phoneNumber, null);
                SMSTransactionPojo smsTransactionPojo = new SMSTransactionPojo(valueFromPrefs);

                //Set the bitcoin address from reply and change the status to received.
                smsTransactionPojo.setBtcAddress(address);
                smsTransactionPojo.setStatus(Constants.SMS_STATUS_REC);
                smsTxnsPrefs.edit().putString(phoneNumber, smsTransactionPojo.getJSONBase64()).commit();

                Intent sendIntent = new Intent(context, AddressScanActivity.class);
                sendIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                sendIntent.putExtra("address", smsTransactionPojo.getBtcAddress());
                sendIntent.putExtra("name", smsTransactionPojo.getName());
                sendIntent.putExtra("number", smsTransactionPojo.getPhoneNumber());
                sendIntent.putExtra("amount", smsTransactionPojo.getAmount().toString());
                sendIntent.putExtra("timestamp", smsTransactionPojo.getTimestamp());
                sendIntent.putExtra("tag", smsTransactionPojo.getTag());

                context.startActivity(sendIntent);
                abortBroadcast();
            }

        }
    }

}