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

package com.aegiswallet.adapters;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.aegiswallet.R;
import com.aegiswallet.actions.AddressScanActivity;
import com.aegiswallet.objects.SMSTransactionPojo;
import com.aegiswallet.utils.BasicUtils;
import com.aegiswallet.utils.Constants;
import com.aegiswallet.utils.WalletUtils;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by bsimic on 3/11/14.
 */
public class PendingTransactionListAdapter extends ArrayAdapter<SMSTransactionPojo> {

    private String TAG = this.getClass().getName();

    private ArrayList<SMSTransactionPojo> transactions;
    private Context context;
    private SharedPreferences smsTxnsPrefs;

    public PendingTransactionListAdapter(Context context,
                                         int resourceId,
                                         ArrayList<SMSTransactionPojo> transactions
    ) {

        super(context, resourceId, transactions);
        this.transactions = transactions;
        this.context = context;
        this.smsTxnsPrefs = context.getSharedPreferences(context.getString(R.string.sms_transaction_filename), Context.MODE_PRIVATE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = null;

        LayoutInflater vi = (LayoutInflater) getContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        v = vi.inflate(R.layout.pending_transaction_detail_row, null);

        SMSTransactionPojo t = null;
        if (transactions != null) {
            try {
                t = transactions.get(position);
            } catch (NullPointerException e) {
                v.setVisibility(View.GONE);
                return v;
            } catch (IndexOutOfBoundsException e) {
                v.setVisibility(View.GONE);
                return v;
            }
        }

        if (t != null) {

            Date date = new Date(t.getTimestamp());

            TextView nameView = (TextView) v.findViewById(R.id.pending_transaction_detail_name);

            if(!t.getName().isEmpty()){
                nameView.setText(t.getName() + " (" + t.getPhoneNumber() + ")");
            }
            else {
                nameView.setText(t.getPhoneNumber());
            }

            ImageView image = (ImageView) v.findViewById(R.id.pending_transaction_send_receive_image);

            TextView amountView = (TextView) v.findViewById(R.id.pending_transaction_detail_amount);
            TextView dayOfWeekView = (TextView) v.findViewById(R.id.pending_transaction_detail_dayofweek);
            TextView dateView = (TextView) v.findViewById(R.id.pending_transaction_detail_date);
            TextView timeView = (TextView) v.findViewById(R.id.pending_transaction_detail_time);
            TextView timeZoneView = (TextView) v.findViewById(R.id.pending_transaction_detail_timezone);
            TextView btcAmountView = (TextView) v.findViewById(R.id.pending_transaction_amount_btc);
            TextView tagTextView = (TextView) v.findViewById(R.id.pending_transaction_tag_text);

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);

            SimpleDateFormat dayOfWeekFormat = new SimpleDateFormat("EEEE");
            String dayOfWeek = dayOfWeekFormat.format(date);
            dayOfWeekView.setText(dayOfWeek);

            SimpleDateFormat dateFormat = new SimpleDateFormat("d MMM, yyyy");
            String dateString = dateFormat.format(date);
            dateView.setText(dateString);

            String minutePrepend = calendar.get(Calendar.MINUTE) < 10 ? "0" : "";
            timeView.setText(calendar.get(Calendar.HOUR) + ":" + minutePrepend + calendar.get(Calendar.MINUTE));

            String amPm = new SimpleDateFormat("aa").format(date);

            timeZoneView.setText(" " + amPm);

            ImageButton actionButton = (ImageButton) v.findViewById(R.id.pending_transaction_action_button);
            TransactionOnClickListener listener = new TransactionOnClickListener(t);
            actionButton.setOnClickListener(listener);
            v.setOnClickListener(listener);

            TextView transactionStatus = (TextView) v.findViewById(R.id.pending_transaction_status);
            BigInteger value = t.getAmount();
            boolean sent = true;

            if (t.getStatus() == Constants.SMS_STATUS_INIT) {
                transactionStatus.setText(R.string.sms_status_sent);
                transactionStatus.setVisibility(View.VISIBLE);
            } else if (t.getStatus() == Constants.SMS_STATUS_REC) {
                transactionStatus.setText(R.string.sms_status_rec);
                transactionStatus.setVisibility(View.VISIBLE);
            } else {
                transactionStatus.setVisibility(View.GONE);
            }

            String currencyAmount = "0";

            if (sent && value != null) {
                currencyAmount = WalletUtils.getWalletCurrencyValue(context,
                        PreferenceManager.getDefaultSharedPreferences(context),
                        value.abs().subtract(Constants.MIN_AMOUN_BIG_INT));
            }


            if(value != null){
                amountView.setText(currencyAmount);
                btcAmountView.setText("  (" + BasicUtils.satoshiToBTC(value) + " " + context.getString(R.string.btc_string) + ")");
            }

            if (sent) {
                image.setBackground(context.getResources().getDrawable(R.drawable.aegis_send_icon));
            }

            if (t.getTag() != null) {
                String tag = t.getTag();
                if (tag != null && tag.length() > 0) {
                    tagTextView.setVisibility(View.VISIBLE);
                    tagTextView.setText(tag);
                }
            }

        }

        return v;
    }

    public class TransactionOnClickListener implements View.OnClickListener {
        SMSTransactionPojo transaction;

        public TransactionOnClickListener(SMSTransactionPojo transaction) {
            this.transaction = transaction;
        }

        @Override
        public void onClick(View v) {

            final Dialog smsDialog = new Dialog(context);
            smsDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            smsDialog.setContentView(R.layout.sms_transaction_detail_prompt);

            TextView name = (TextView) smsDialog.findViewById(R.id.sms_transaction_name);
            TextView amount = (TextView) smsDialog.findViewById(R.id.sms_transaction_amount);
            TextView date = (TextView) smsDialog.findViewById(R.id.sms_transaction_date);

            if(!transaction.getName().isEmpty()){
                name.setText(transaction.getName() + " (" + transaction.getPhoneNumber() + ")");
            }
            else{
                name.setText(transaction.getPhoneNumber());
            }


            String currencyAmount = WalletUtils.getWalletCurrencyValue(context,
                        PreferenceManager.getDefaultSharedPreferences(context),
                        transaction.getAmount().abs().subtract(Constants.MIN_AMOUN_BIG_INT));
            String btcAmount = "(" + BasicUtils.satoshiToBTC(transaction.getAmount()) + " " + context.getString(R.string.btc_string) + ")";
            amount.setText(currencyAmount + " " + btcAmount);

            Date dateObject = new Date(transaction.getTimestamp());

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(dateObject);

            SimpleDateFormat dayOfWeekFormat = new SimpleDateFormat("EEEE");
            String dayOfWeek = dayOfWeekFormat.format(dateObject);

            SimpleDateFormat dateFormat = new SimpleDateFormat("d MMMM, yyyy");
            String dateString = dateFormat.format(dateObject);

            String minutePrepend = calendar.get(Calendar.MINUTE) < 10 ? "0" : "";
            String minuteString = calendar.get(Calendar.HOUR) + ":" + minutePrepend + calendar.get(Calendar.MINUTE);

            String amPm = new SimpleDateFormat("aa").format(dateObject);

            date.setText(dayOfWeek + ", " + dateString + " " + minuteString + " " + amPm);


            Button confirm = (Button) smsDialog.findViewById(R.id.sms_transaction_confirm);
            Button cancel = (Button) smsDialog.findViewById(R.id.sms_transaction_delete);

            if(transaction.getStatus() != Constants.SMS_STATUS_REC){
                confirm.setVisibility(View.GONE);
            }

            confirm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //Do confirm
                    Intent sendIntent = new Intent(context, AddressScanActivity.class);
                    sendIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    sendIntent.putExtra("address", transaction.getBtcAddress());
                    sendIntent.putExtra("name", transaction.getName());
                    sendIntent.putExtra("number", transaction.getPhoneNumber());
                    sendIntent.putExtra("amount", transaction.getAmount().toString());
                    sendIntent.putExtra("timestamp", transaction.getTimestamp());
                    sendIntent.putExtra("tag", transaction.getTag());

                    smsDialog.cancel();
                    context.startActivity(sendIntent);
                }
            });


            cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //Do cancel
                    cancelTransaction(transaction);
                    smsDialog.cancel();
                    Toast.makeText(context, context.getString(R.string.sms_transaction_cancelled), Toast.LENGTH_LONG).show();
                }
            });

            Button cancelButton = (Button) smsDialog.findViewById(R.id.sms_transaction_ok_button);
            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    smsDialog.dismiss();
                }
            });

            smsDialog.show();

        }
    };

    @Override
    public SMSTransactionPojo getItem(int position) {
        return transactions.get(position);
    }

    @Override
    public int getCount() {
        return transactions.size();
    }

    public void cancelTransaction(SMSTransactionPojo transactionPojo){
        String plainNumber = transactionPojo.getPhoneNumber().replaceAll("[^0-9]", "");
        SharedPreferences prefs = context.getSharedPreferences(context.getString(R.string.sms_transaction_filename), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(plainNumber);
        editor.commit();

        transactions.remove(transactionPojo);
        notifyDataSetChanged();
    }
}
