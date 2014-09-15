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

package com.aegiswallet.actions;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.aegiswallet.PayBitsApplication;
import com.aegiswallet.R;
import com.aegiswallet.utils.BasicUtils;
import com.aegiswallet.utils.Constants;
import com.aegiswallet.utils.WalletUtils;
import com.google.bitcoin.core.Wallet;
import com.google.zxing.BarcodeFormat;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * Created by bsimic on 3/11/14.
 */
public class RequestBitcoinActivity extends Activity {

    private String TAG = this.getClass().getName();

    private PayBitsApplication application;
    private ImageButton backButton;
    private TextView titleTextView;
    private TextView balanceOnSendView;
    private TextView balanceOnSendViewCurrency;
    private TextView balanceOnSendCurrencyType;
    private ImageView qrCodeImage;
    private EditText amountText;
    private TextView amountInCurrency;
    private Context context = this;
    private Wallet wallet;
    private SharedPreferences prefs;
    private Spinner currencySpinner;
    private int currentSpinnerSelection = 0;
    private Bitmap requestBitmap = null;

    private Button shareButton;
    private Button copyButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_request);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getActionBar().setCustomView(R.layout.aegis_send_actionbar);

        qrCodeImage = (ImageView) findViewById(R.id.request_coins_qrcode);

        updateQRCode(prefs.getString(Constants.PREFS_KEY_SELECTED_ADDRESS, null));

        titleTextView = (TextView) findViewById(R.id.action_bar_title_text);
        titleTextView.setText(getString(R.string.request_activity_label));

        application = (PayBitsApplication) getApplication();
        wallet = application.getWallet();

        currencySpinner = (Spinner) findViewById(R.id.currency_spinner);
        String[] items = new String[]{getString(R.string.btc_string), prefs.getString(Constants.CURRENCY_PREF_KEY, null)};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.spinner_item, items);
        currencySpinner.setAdapter(adapter);

        //Make default user defined currency.
        currencySpinner.setSelection(1);

        balanceOnSendView = (TextView) findViewById(R.id.balance_on_send_view);
        balanceOnSendView.setText(" " + BasicUtils.satoshiToBTC(application.getWallet().getBalance(Wallet.BalanceType.ESTIMATED)) + " ");

        balanceOnSendViewCurrency = (TextView) findViewById(R.id.balance_in_currency_send_view);
        balanceOnSendViewCurrency.setText(WalletUtils.getWalletCurrencyValue(getApplicationContext(),
                prefs, wallet.getBalance(Wallet.BalanceType.ESTIMATED)) + " ");

        balanceOnSendCurrencyType = (TextView) findViewById(R.id.send_balance_currency_type);
        balanceOnSendCurrencyType.setText(prefs.getString(Constants.CURRENCY_PREF_KEY, null));

        handleButtons();
    }

    private void handleButtons() {

        shareButton = (Button) findViewById(R.id.request_share_button);
        copyButton = (Button) findViewById(R.id.request_copy_button);

        //shareButton.setBackground(getResources().getDrawable(R.drawable.share_image));
        //copyButton.setBackground(getResources().getDrawable(R.drawable.copy_image));

        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.request_subject));
                sendIntent.putExtra(Intent.EXTRA_TEXT, getPayloadString());

                if (requestBitmap != null) {
                    //String pathofBmp = MediaStore.Images.Media.insertImage(getContentResolver(), requestBitmap, "BitcoinRequest", null);
                    //Uri uri = Uri.parse(pathofBmp);
                    Uri uri = saveImage(requestBitmap);
                    if(uri != null)
                        sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
                }

                sendIntent.setType("text/plain");
                startActivity(sendIntent);
            }
        });

        copyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);


                ClipData clip = ClipData.newPlainText("Request", getPayloadString());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(context, context.getString(R.string.request_copied_to_clipboard_string), 250).show();

            }
        });

        currencySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                currentSpinnerSelection = position;
                handleCurrencyChange(position);
                // your code here
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
            }

        });

        backButton = (ImageButton) findViewById(R.id.action_bar_icon_back);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent openMainActivity = new Intent(context, MainActivity.class);
                openMainActivity.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                context.startActivity(openMainActivity);
                finish();
            }
        });

        amountText = (EditText) findViewById(R.id.sent_amount);
        amountInCurrency = (TextView) findViewById(R.id.send_action_currency_value);

        amountText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                handleCurrencyChange(currentSpinnerSelection);
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (amountText.getText().length() == 0) {
                    amountInCurrency.setText("");
                    updateQRCode(prefs.getString(Constants.PREFS_KEY_SELECTED_ADDRESS, null));
                }
            }
        });
    }

    private Uri saveImage(Bitmap finalBitmap) {

        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root + "/aegist_request_images");
        myDir.mkdirs();
        String fname = "AegisRequest.jpg";
        File file = new File (myDir, fname);
        if (file.exists ()) file.delete ();
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        Uri returnUri = Uri.fromFile(file);
        return returnUri;

    }

    private String getPayloadString() {
        String payload = null;
        String amountString = amountText.getText().toString();
        if (amountString != null && amountString.length() > 0 && !amountString.equals(".")) {
            payload = getString(R.string.request_str_part_1) +
                    " " + amountInCurrency.getText().toString() +
                    " (" + amountText.getText().toString() + " " +
                    currencySpinner.getItemAtPosition(currentSpinnerSelection).toString() +
                    ") " +
                    " " + getString(R.string.request_str_part_2) +
                    " " + prefs.getString(Constants.PREFS_KEY_SELECTED_ADDRESS, null);
        } else {
            payload = getString(R.string.request_str_part_1) +
                    " " + getString(R.string.bitcoin_string) +
                    " " + getString(R.string.request_str_part_2) +
                    " " + prefs.getString(Constants.PREFS_KEY_SELECTED_ADDRESS, null);
        }

        return payload;
    }

    private void handleCurrencyChange(int position) {
        String amountString = amountText.getText().toString();
        String currentAddress = prefs.getString(Constants.PREFS_KEY_SELECTED_ADDRESS, null);
        String bitmapString = "bitcoin:" + currentAddress + "?amount=";

        if (amountString != null && amountString.length() == 0) {
            amountInCurrency.setText("");
            updateQRCode(prefs.getString(Constants.PREFS_KEY_SELECTED_ADDRESS, null));
            return;
        }

        if (position == 0) {
            try {
                BigDecimal btcAmountBigInt = new BigDecimal(amountString);
                bitmapString += btcAmountBigInt.toString();
                updateQRCode(bitmapString);

                amountInCurrency.setText(WalletUtils.getBTCCurrencryValue(getApplicationContext(),
                        prefs, btcAmountBigInt) + " " + prefs.getString(Constants.CURRENCY_PREF_KEY, null));
            } catch (NumberFormatException e) {
                Log.d(TAG, e.getMessage());
            }

        } else if (position == 1) {
            try {
                BigDecimal testDecimalAmount = new BigDecimal(amountString);
                BigDecimal oneBTCDecimal = new BigDecimal("100000000");

                BigDecimal exchangeRate = WalletUtils.getExchangeRate(context, prefs);

                BigDecimal btcAmountDecimal = testDecimalAmount.multiply(oneBTCDecimal).divide(exchangeRate, 5, RoundingMode.HALF_EVEN);
                BigInteger btcAmountInteger = btcAmountDecimal.toBigInteger();

                if (btcAmountInteger != null) {
                    amountInCurrency.setText(BasicUtils.satoshiToBTC(btcAmountInteger) + " " + getString(R.string.btc_string));
                    bitmapString += BasicUtils.satoshiToBTC(btcAmountInteger);
                    updateQRCode(bitmapString);
                }

            } catch (NumberFormatException e) {
                Log.d(TAG, e.getMessage());
                //Could not convert string to bigint
            }
        }
    }

    private void updateQRCode(String payload) {
        requestBitmap = BasicUtils.encodeAsBitmap(payload, BarcodeFormat.QR_CODE, 500);
        qrCodeImage.setImageBitmap(requestBitmap);
    }

}
