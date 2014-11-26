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
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.aegiswallet.PayBitsApplication;
import com.aegiswallet.R;
import com.aegiswallet.helpers.PaymentProtocolHelper;
import com.aegiswallet.listeners.PasswordProvidedListener;
import com.aegiswallet.objects.SMSTransactionPojo;
import com.aegiswallet.tasks.SendBTCTask;
import com.aegiswallet.utils.BasicUtils;
import com.aegiswallet.utils.Constants;
import com.aegiswallet.utils.NfcUtils;
import com.aegiswallet.utils.SMSTools;
import com.aegiswallet.utils.WalletUtils;
import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.core.WrongNetworkException;
import com.google.zxing.client.android.CaptureActivity;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by bsimic on 3/11/14.
 */
public class AddressScanActivity extends Activity implements PasswordProvidedListener {

    private String TAG = this.getClass().getName();

    private PayBitsApplication application;
    private ImageButton backButton;
    private Button scanButton;
    private Button sendButton;
    private TextView titleTextView;
    private TextView balanceOnSendView;
    private TextView balanceOnSendViewCurrency;
    private TextView balanceOnSendCurrencyType;
    private EditText amountText;
    private AutoCompleteTextView toAddressText;
    private TextView amountInCurrency;
    private Context context = this;
    private Wallet wallet;
    private SharedPreferences prefs;
    private Spinner currencySpinner;
    private int currentSpinnerSelection = 0;
    private BigInteger latestSendAmountInSatoshis;
    private String[] tagOptions;
    private AutoCompleteTextView tagTextView;
    private SharedPreferences tagPrefs;
    private SharedPreferences smsTxnsPrefs;

    private ArrayList<Map<String, String>> peopleList;
    private String selectedNumber;
    private String selectedName;
    private SimpleAdapter simpleAdapter;

    private boolean nfcEnabled;
    private boolean sendCoinsFlag;
    private Pattern numberPattern = Pattern.compile("^[+]?[0-9]{10,13}$");


    private String smsTransactionPhoneNumber;
    private boolean isSMSTransaction;


    private String ACTION_START_SCAN = "com.google.zxing.client.android.SCAN";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_send);

        getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getActionBar().setCustomView(R.layout.aegis_send_actionbar);

        titleTextView = (TextView) findViewById(R.id.action_bar_title_text);
        titleTextView.setText(getString(R.string.send_btc_header));

        application = (PayBitsApplication) getApplication();
        wallet = application.getWallet();

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        currencySpinner = (Spinner) findViewById(R.id.currency_spinner);
        String[] items = new String[]{getString(R.string.btc_string), prefs.getString(Constants.CURRENCY_PREF_KEY, null)};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.spinner_item, items);
        currencySpinner.setAdapter(adapter);

        //Make the default USD or user defined currency
        currencySpinner.setSelection(1);

        balanceOnSendView = (TextView) findViewById(R.id.balance_on_send_view);
        balanceOnSendView.setText(" " + BasicUtils.satoshiToBTC(application.getWallet().getBalance(Wallet.BalanceType.AVAILABLE)) + " ");

        balanceOnSendViewCurrency = (TextView) findViewById(R.id.balance_in_currency_send_view);
        balanceOnSendViewCurrency.setText(WalletUtils.getWalletCurrencyValue(getApplicationContext(),
                prefs, wallet.getBalance(Wallet.BalanceType.AVAILABLE)) + " ");

        balanceOnSendCurrencyType = (TextView) findViewById(R.id.send_balance_currency_type);
        balanceOnSendCurrencyType.setText(prefs.getString(Constants.CURRENCY_PREF_KEY, null));

        tagOptions = getResources().getStringArray(R.array.default_tags);

        tagPrefs = application.getSharedPreferences(
                getString(R.string.tag_pref_filename), Context.MODE_PRIVATE);
        smsTxnsPrefs = application.getSharedPreferences(getString(R.string.sms_transaction_filename), Context.MODE_PRIVATE);
        tagOptions = createTagOptionsFromPrefs(tagPrefs, tagOptions);

        ArrayAdapter<String> autocompleteAdapter = new ArrayAdapter<String>(this,
                R.layout.autocomplete1line, tagOptions);
        tagTextView = (AutoCompleteTextView)
                findViewById(R.id.transaction_tag);
        tagTextView.setAdapter(autocompleteAdapter);

        peopleList = new ArrayList<Map<String, String>>();
        peopleList = application.getPeopleList();

        handleButtons();
        determineNFCEnabled();
    }

    private String[] createTagOptionsFromPrefs(SharedPreferences tagPrefs, String[] tagOptions) {
        String[] result = tagOptions;

        Map<String, ?> keys = tagPrefs.getAll();

        ArrayList<String> defaultTagList = new ArrayList<String>(Arrays.asList(tagOptions));

        for (Map.Entry<String, ?> entry : keys.entrySet()) {
            if (!defaultTagList.contains(entry.getValue().toString())) {
                String value = entry.getValue().toString();
                defaultTagList.add(value);
            }
        }

        result = defaultTagList.toArray(new String[defaultTagList.size()]);


        return result;
    }

    @Override
    protected void onResume() {
        super.onResume();
        determineNFCEnabled();
        peopleList = application.getPeopleList();
        NfcUtils.listen(this, getClass());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        byte[] result = NfcUtils.getData(intent);

        Vibrator v = (Vibrator) this.context.getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(500);

        String resultString = null;

        if (result != null)
            resultString = new String(result);

        String shamirX2Hashed = application.getPrefs().getString(Constants.SHAMIR_X2_HASHED, null);


        if (resultString != null && shamirX2Hashed.equals(WalletUtils.convertToSha256(resultString))) {
            if (sendCoinsFlag) {
                String amountStr = amountText.getText().toString();
                sendCoinsFlag = false;
                application.cancelNFCPrompt(context);
                initiateSendCoins(amountStr, resultString);
            }
        } else {
            Toast.makeText(context, getString(R.string.nfc_tag_invalid_string), Toast.LENGTH_SHORT).show();
        }

    }

    private void handleButtons() {

        currencySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                currentSpinnerSelection = position;
                handleCurrencyChange(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
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
        toAddressText = (AutoCompleteTextView) findViewById(R.id.send_address);

        simpleAdapter = new SimpleAdapter(this, peopleList, R.layout.contact_autocomplete,
                new String[]{"Name", "Phone", "Type"}, new int[]{
                R.id.contactName, R.id.contactNumber, R.id.contactNumberType});
        toAddressText.setAdapter(simpleAdapter);
        toAddressText.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> av, View arg1, int index,
                                    long arg3) {
                Map<String, String> map = (Map<String, String>) av.getItemAtPosition(index);

                String name = map.get("Name");
                String number = map.get("Phone");
                String value = "" + name + " <" + number + ">";
                toAddressText.setText(value);
                selectedNumber = number;
                selectedName = name;
                toAddressText.setSelection(value.length());
            }
        });


        toAddressText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void afterTextChanged(Editable editable) {



            }
        });

        String addressFromIntent = this.getIntent().getStringExtra("address");
        String nameFromIntent = this.getIntent().getStringExtra("name");
        String numberFromIntent = this.getIntent().getStringExtra("number");
        String amountFromIntent = this.getIntent().getStringExtra("amount");
        String timeStampFromIntent = this.getIntent().getStringExtra("timestamp");
        String tagFromIntent = this.getIntent().getStringExtra("tag");

        scanButton = (Button) findViewById(R.id.scan_qr_code_button);

        if (addressFromIntent != null) {
            toAddressText.setText(addressFromIntent);
        }

        if(numberFromIntent != null && amountFromIntent != null){
            if(nameFromIntent == null)
                nameFromIntent = "";

            String message = getString(R.string.sms_received_message, nameFromIntent, numberFromIntent);
            String amount = BasicUtils.satoshiToBTC(new BigInteger(amountFromIntent));

            currencySpinner.setSelection(0);
            amountText.setText(amount);

            TextView sendMessageTextView = (TextView) findViewById(R.id.send_message);
            sendMessageTextView.setText(message);
            sendMessageTextView.setVisibility(View.VISIBLE);

            scanButton.setVisibility(View.GONE);

            amountInCurrency.setText(WalletUtils.getBTCCurrencryValue(getApplicationContext(),
                    prefs, new BigDecimal(amount)) + " " + getString(R.string.btc_string));

            if(tagFromIntent != null){
                tagTextView.setText(tagFromIntent);
            }

            isSMSTransaction = true;
            smsTransactionPhoneNumber = numberFromIntent.replaceAll("[^0-9]","");
        }
        else {
            isSMSTransaction = false;
        }

        sendButton = (Button) findViewById(R.id.confirm_send_coins);

        sendButton.setEnabled(true);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                decodeQRCode();
            }
        });

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
            }
        });

        if (application.getWallet().isEncrypted()) {

            sendButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (checkAddressAndAmountValid()) {

                        if (isSMSTransaction) {
                            sendTextMessage();
                        } else if (nfcEnabled) {
                            application.showNFCPrompt(context);
                            sendCoinsFlag = true;
                        } else {
                            application.showPasswordPrompt(context, Constants.ACTION_DECRYPT);
                        }
                    }

                }
            });

        } else {
            sendButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (checkAddressAndAmountValid()) {
                        if (isSMSTransaction) {
                            sendTextMessage();
                        } else {
                            String amountStr = amountText.getText().toString();
                            initiateSendCoins(amountStr, null);
                        }
                    }

                }
            });
        }

    }

    private void sendTextMessage() {
        final Dialog textMessageDialog = new Dialog(context);
        textMessageDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        textMessageDialog.setContentView(R.layout.sms_initate_transaction_prompt);

        Button cancelButton = (Button) textMessageDialog.findViewById(R.id.sms_init_cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                textMessageDialog.dismiss();
            }
        });

        final Button continueButton = (Button) textMessageDialog.findViewById(R.id.sms_init_continue_button);

        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String textToSend = getString(R.string.sms_send_message) + " "
                        + amountText.getText().toString() + " "
                        + currencySpinner.getItemAtPosition(currentSpinnerSelection)
                        + " (" + amountInCurrency.getText().toString()
                        + ").";

                String plainNumber = selectedNumber.replaceAll("[^0-9]","");

                try {
                    textToSend = URLEncoder.encode(textToSend, "UTF-8");
                    textToSend = URLDecoder.decode(textToSend, "UTF-8");

                    String gsm7String = new String(SMSTools.convertUnicode2GSM(textToSend));
                    SmsManager sms = SmsManager.getDefault();
                    ArrayList<String> msgStringArray = sms.divideMessage(gsm7String);

                    if (smsTxnsPrefs.contains(plainNumber)) {
                        Toast.makeText(context, getString(R.string.sms_pendngtx) + " " + selectedName, Toast.LENGTH_LONG).show();
                    }
                    {
                        sms.sendMultipartTextMessage(selectedNumber, null, msgStringArray, null, null);

                        SMSTransactionPojo smsTransactionPojo = new SMSTransactionPojo(selectedNumber, selectedName, latestSendAmountInSatoshis, "", Constants.SMS_STATUS_INIT, tagTextView.getText().toString());
                        smsTxnsPrefs.edit().putString(plainNumber, smsTransactionPojo.getJSONBase64()).commit();
                        Intent openMainActivity = new Intent(context, MainActivity.class);
                        openMainActivity.putExtra("message", getString(R.string.sms_sent_message, toAddressText.getText().toString()));
                        openMainActivity.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        context.startActivity(openMainActivity);
                        finish();
                    }
                } catch (UnsupportedEncodingException e) {
                    Log.d(TAG, e.getMessage());
                }


            }
        });

        textMessageDialog.show();
    }

    private void handleCurrencyChange(int position) {
        String amountString = amountText.getText().toString();

        if (position == 0) {
            try {
                BigDecimal btcAmountBigInt = new BigDecimal(amountString);
                amountInCurrency.setText(WalletUtils.getBTCCurrencryValue(getApplicationContext(),
                        prefs, btcAmountBigInt) + " " + prefs.getString(Constants.CURRENCY_PREF_KEY, null));

                latestSendAmountInSatoshis = BasicUtils.toNanoCoins(btcAmountBigInt.toString(), 0);

            } catch (NumberFormatException e) {
                Log.d(TAG, e.getMessage());
            }
        } else {
            try {
                BigDecimal testDecimalAmount = new BigDecimal(amountString);
                BigDecimal oneBTCDecimal = BigDecimal.valueOf(100000000);
                BigDecimal exchangeRate = WalletUtils.getExchangeRate(context, prefs);

                BigDecimal btcAmountDecimal = testDecimalAmount.multiply(oneBTCDecimal).divide(exchangeRate, 5, RoundingMode.HALF_EVEN);
                BigInteger btcAmountInteger = btcAmountDecimal.toBigInteger();

                if (btcAmountInteger != null) {
                    amountInCurrency.setText(BasicUtils.satoshiToBTC(btcAmountInteger) + " " + getString(R.string.plain_btc_string));
                }

                latestSendAmountInSatoshis = btcAmountInteger;

            } catch (NumberFormatException e) {
                Log.d(TAG, e.getMessage());
            }
        }
    }

    private void decodeQRCode() {

        Intent intent = new Intent(this, CaptureActivity.class);
        intent.setAction(ACTION_START_SCAN);

        intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
        startActivityForResult(intent, 1);
    }

    private void initiateSendCoins(String amountStr, String passOrNFC) {

        BigInteger newAmount = null;
        BigDecimal currAmountBigInt = null;

        if (currentSpinnerSelection == 0) {
            newAmount = BasicUtils.toNanoCoins(amountStr, 0);
        } else if (currentSpinnerSelection == 1) {

            BigDecimal testDecimalAmount = new BigDecimal(amountStr);
            BigDecimal oneBTCDecimal = BigDecimal.valueOf(100000000);
            BigDecimal exchangeRate = WalletUtils.getExchangeRate(context, prefs);

            BigDecimal btcAmountDecimal = testDecimalAmount.multiply(oneBTCDecimal).divide(exchangeRate, 5, RoundingMode.HALF_EVEN);
            BigInteger btcAmountInteger = btcAmountDecimal.toBigInteger();

            newAmount = btcAmountInteger;
        }

        if (BasicUtils.isNetworkAvailable(context)) {
            String tagText = "";
            if (tagTextView != null && tagTextView.getText().toString() != null) {
                tagText = tagTextView.getText().toString();
            }

            SendBTCTask sendBTCTask = new SendBTCTask(this, wallet, passOrNFC, toAddressText.getText().toString(), newAmount, application, tagText);
            //sendBTCTask.execute();
            sendBTCTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);

            SharedPreferences prefs = context.getSharedPreferences(context.getString(R.string.sms_transaction_filename), Context.MODE_PRIVATE);

            // If it's an SMS transaction, we remove it because it is now completed.
            if(prefs.contains(smsTransactionPhoneNumber)){
                SharedPreferences.Editor editor = prefs.edit();
                editor.remove(smsTransactionPhoneNumber);
                editor.commit();
            }

            sendButton.setEnabled(false);
            //finish();
        } else {
            Toast.makeText(context, getString(R.string.no_internet_connection_available_string), Toast.LENGTH_LONG).show();
        }
    }

    private boolean checkAddressAndAmountValid() {
        boolean result = false;
        boolean isValidAddress = false;
        boolean isValidPhoneNumber = false;

        String addressTextBoxValue = toAddressText.getText().toString();

        if(selectedName == null){
            selectedNumber = addressTextBoxValue;
        }

        try {
            Log.d(TAG, "Trying address: " + addressTextBoxValue);

            Address a = new Address(Constants.NETWORK_PARAMETERS, addressTextBoxValue);
            result = true;
            isSMSTransaction = false;
            isValidAddress = true;

            Log.d(TAG, "isValidAddress: " + isValidAddress);
        } catch (WrongNetworkException e) {
            Log.d(TAG, "WrongNetworkException" + e.getMessage());
            result = false;
        } catch (AddressFormatException e) {
            Log.d(TAG, "AddressFormatException" + e.getMessage());
            result = false;
        }
        catch (Exception e){
            Log.d(TAG, "Some other Exception" + e.getMessage());
        }

        Log.d(TAG, "selectedNumber: " + selectedNumber);

        if (selectedNumber != null && !isValidAddress) {

            String parsedAddressValue = addressTextBoxValue.replaceAll("[^\\d+]", "");
            Log.d(TAG, "parsedAddressValue: " + parsedAddressValue);

            String selectedNumberValue = selectedNumber.replaceAll("[^\\d+]", "");
            Log.d(TAG, "parsedNumberValue: " + parsedAddressValue);


            Matcher addressMatcher = numberPattern.matcher(parsedAddressValue);
            Matcher numberMatcher = numberPattern.matcher(selectedNumberValue);

            boolean addressValueMatches = addressMatcher.matches();
            boolean selectedNumberMatches = numberMatcher.matches();

            //boolean isValidNumber = PhoneNumberUtils.isGlobalPhoneNumber(selectedNumber.replaceAll("[^0-9]","")) || PhoneNumberUtils.isGlobalPhoneNumber(parsedAddressValue);
            boolean isValidNumber = addressValueMatches || selectedNumberMatches;

            if (!isValidNumber) {
                Toast.makeText(this, getString(R.string.invalid_address_string), Toast.LENGTH_LONG).show();
                Log.d(TAG, "INVALID NUMBER " + selectedNumber);
                return false;
            }
            else{
                Log.d(TAG, "VALID NUMBER " + selectedNumber);
                isSMSTransaction = true;
                isValidPhoneNumber = true;
                result = true;
            }
        }

        if (amountText.length() <= 0) {
            Toast.makeText(context, getString(R.string.amount_cannot_be_empty), Toast.LENGTH_LONG).show();
            return false;
        }

        if (latestSendAmountInSatoshis == null || (latestSendAmountInSatoshis != null && latestSendAmountInSatoshis.longValue() <= Constants.MIN_AMOUNT_IN_SATOSHI)) {
            Toast.makeText(context, getString(R.string.send_amount_less_than_minimum_amount), Toast.LENGTH_LONG).show();
            return false;
        }

        if (latestSendAmountInSatoshis != null) {
            BigInteger availMinusInput = wallet.getBalance(Wallet.BalanceType.AVAILABLE).subtract(latestSendAmountInSatoshis);

            if (availMinusInput.longValue() < 0) {
                Toast.makeText(this, getString(R.string.invalid_amount_string), Toast.LENGTH_LONG).show();
                return false;
            }
        }

        if(!isValidAddress && !isValidPhoneNumber){
            Toast.makeText(this, getString(R.string.invalid_address_string), Toast.LENGTH_LONG).show();
        }

        return result;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == RESULT_OK) {

            String contents = data.getStringExtra("SCAN_RESULT");

            PaymentProtocolHelper helper = new PaymentProtocolHelper(contents);
            if (helper.getAddress() != null) {
                toAddressText.setText(helper.getAddress().toString());
            }

            if (helper.getAmount() != null) {

                currencySpinner.setSelection(0);

                BigInteger amount = helper.getAmount();
                String btcAmount = BasicUtils.satoshiToBTC(amount);

                amountText.setText(btcAmount);

                amountInCurrency.setText(WalletUtils.getBTCCurrencryValue(getApplicationContext(),
                        prefs, new BigDecimal(btcAmount)) + " " + getString(R.string.btc_string));
            }
        }
    }

    @Override
    public void onPasswordProvided(String password, int action) {
        String amountStr = amountText.getText().toString();
        initiateSendCoins(amountStr, password);
    }

    private void determineNFCEnabled() {
        nfcEnabled = prefs.contains(Constants.SHAMIR_ENCRYPTED_KEY) ? false : true;
    }


}
