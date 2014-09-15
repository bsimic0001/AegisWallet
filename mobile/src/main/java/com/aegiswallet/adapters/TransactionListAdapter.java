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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.aegiswallet.R;
import com.aegiswallet.utils.BasicUtils;
import com.aegiswallet.utils.Constants;
import com.aegiswallet.utils.WalletUtils;
import com.google.bitcoin.core.ScriptException;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionConfidence;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.script.Script;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;

/**
 * Created by bsimic on 3/11/14.
 */
public class TransactionListAdapter extends ArrayAdapter<Transaction> {

    private String TAG = this.getClass().getName();

    private ArrayList<Transaction> transactions;
    private Context context;
    private Wallet wallet;
    private SharedPreferences tagPrefs;

    public TransactionListAdapter(Context context,
                                  int resourceId,
                                  ArrayList<Transaction> transactions,
                                  Wallet wallet) {

        //super(context, resourceId, WalletUtils.getRelevantTransactions(transactions, wallet));
        //this.transactions = WalletUtils.getRelevantTransactions(transactions, wallet);
        super(context, resourceId, transactions);
        this.transactions = transactions;
        this.context = context;
        this.wallet = wallet;

        this.tagPrefs = context.getSharedPreferences(context.getString(R.string.tag_pref_filename), Context.MODE_PRIVATE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = null;

        LayoutInflater vi = (LayoutInflater) getContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        v = vi.inflate(R.layout.transaction_detail_row, null);

        Transaction t = null;
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

        LinearLayout parentLayout = (LinearLayout) v.findViewById(R.id.transaction_detail_row_parent);
        View dividerView = (View) v.findViewById(R.id.transaction_divider);

        if (t != null) {
            boolean validTx = WalletUtils.isTransactionRelevant(t, wallet);
            if (!validTx) {
                parentLayout.setVisibility(View.GONE);
                dividerView.setVisibility(View.GONE);
                v.setVisibility(View.GONE);
                return v;
            }

            ImageView image = (ImageView) v.findViewById(R.id.transaction_send_receive_image);
            TextView toView = (TextView) v.findViewById(R.id.transaction_to_address);

            TextView amountView = (TextView) v.findViewById(R.id.transaction_detail_amount);
            TextView dayOfWeekView = (TextView) v.findViewById(R.id.transaction_detail_dayofweek);
            TextView dateView = (TextView) v.findViewById(R.id.transaction_detail_date);
            TextView timeView = (TextView) v.findViewById(R.id.transaction_detail_time);
            TextView timeZoneView = (TextView) v.findViewById(R.id.transaction_detail_timezone);
            TextView btcAmountView = (TextView) v.findViewById(R.id.transaction_amount_btc);
            TextView tagTextView = (TextView) v.findViewById(R.id.transaction_tag_text);

            Date date = t.getUpdateTime();
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

            ImageButton infoButton = (ImageButton) v.findViewById(R.id.transaction_info_button);
            TransactionOnClickListener listener = new TransactionOnClickListener(t);
            infoButton.setOnClickListener(listener);

            TextView transactionStatus = (TextView) v.findViewById(R.id.transaction_status);

            BigInteger value = t.getValue(wallet);
            boolean sent = value.signum() < 0;

            TransactionConfidence confidence = t.getConfidence();
            TransactionConfidence.ConfidenceType confidenceType = confidence.getConfidenceType();

            BigInteger txValue = t.getValue(wallet);
            boolean txSent = txValue.signum() < 0;

            boolean isTimeLocked = t.isTimeLocked();

            if (sent && confidenceType == confidenceType.PENDING && t.getConfidence().numBroadcastPeers() <= 1) {
                transactionStatus.setText(R.string.transaction_not_broadcasted);
                transactionStatus.setVisibility(View.VISIBLE);
            } else if (!txSent && confidenceType == TransactionConfidence.ConfidenceType.PENDING && isTimeLocked) {
                transactionStatus.setText(R.string.transaction_unconfirmed_locked);
                transactionStatus.setVisibility(View.VISIBLE);
            } else if (!txSent && confidenceType == TransactionConfidence.ConfidenceType.PENDING && !isTimeLocked) {
                transactionStatus.setText(R.string.transaction_unconfirmed_not_locked);
                transactionStatus.setVisibility(View.VISIBLE);
            } else if (!txSent && confidenceType == TransactionConfidence.ConfidenceType.DEAD) {
                transactionStatus.setText(R.string.transaction_dead);
                transactionStatus.setVisibility(View.VISIBLE);
            } else {
                transactionStatus.setVisibility(View.GONE);
            }

            String currencyAmount = "0";

            if (sent) {
                currencyAmount = WalletUtils.getWalletCurrencyValue(context,
                        PreferenceManager.getDefaultSharedPreferences(context),
                        value.abs().subtract(Constants.MIN_AMOUN_BIG_INT));
            } else {
                currencyAmount = WalletUtils.getWalletCurrencyValue(context,
                        PreferenceManager.getDefaultSharedPreferences(context),
                        value.abs());
            }

            //amountView.setText(BasicUtils.satoshiToBTC(value));
            if (txHasWatchedOutputs(t)) {
                amountView.setTextColor(context.getResources().getColor(R.color.guava_color));
            }

            amountView.setText(currencyAmount);
            btcAmountView.setText("  (" + BasicUtils.satoshiToBTC(value) + " " + context.getString(R.string.btc_string) + ")");

            if (sent) {
                image.setBackground(context.getResources().getDrawable(R.drawable.aegis_send_icon));
            } else {
                image.setBackground(context.getResources().getDrawable(R.drawable.aegis_receive_icon));
            }

            toView.setVisibility(View.GONE);

            String txHash = t.getHashAsString();
            if (tagPrefs.contains(txHash)) {
                String tag = tagPrefs.getString(txHash, null);
                if (tag != null && tag.length() > 0) {
                    tagTextView.setVisibility(View.VISIBLE);
                    tagTextView.setText(tag);
                }
            }

        }

        return v;
    }

    private boolean txHasWatchedOutputs(Transaction transaction) {
        for (TransactionOutput output : transaction.getOutputs()) {
            if (output.isWatched(wallet))
                return true;
        }

        return false;
    }

    public class TransactionOnClickListener implements View.OnClickListener {
        Transaction transaction;

        public TransactionOnClickListener(Transaction transaction) {
            this.transaction = transaction;
        }

        @Override
        public void onClick(View v) {
            if (BasicUtils.isNetworkAvailable(context)) {
                String url = Constants.BLOCKCHAIN_TX_URL + transaction.getHashAsString();
                Intent viewTxIntent = new Intent(Intent.ACTION_VIEW);
                viewTxIntent.setData(Uri.parse(url));
                context.startActivity(viewTxIntent);
            } else {
                Toast.makeText(context, context.getString(R.string.no_internet_connection_available_string), 500).show();
            }
        }
    };

    @Override
    public Transaction getItem(int position) {
        return transactions.get(position);
    }

    @Override
    public int getCount() {
        return transactions.size();
    }
}
