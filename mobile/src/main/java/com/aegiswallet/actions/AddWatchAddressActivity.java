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
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.aegiswallet.PayBitsApplication;
import com.aegiswallet.R;
import com.aegiswallet.tasks.DecryptWalletAndAddKeysTask;
import com.aegiswallet.utils.Constants;
import com.aegiswallet.utils.NfcUtils;
import com.aegiswallet.utils.WalletUtils;
import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.Wallet;
import com.google.zxing.client.android.CaptureActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bsimic on 6/6/14.
 */
public class AddWatchAddressActivity extends Activity {

    private Button importButton;
    private ImageButton backButton;
    private TextView titleTextView;
    private EditText addressTextBox;
    private Button scanButton;
    private SharedPreferences prefs;
    private PayBitsApplication application;
    private Wallet wallet;
    private Context context = this;
    private boolean addKeyFlag;
    private boolean nfcEnabled;
    private ECKey keyToAdd;
    private String passwordOrX2;

    private String ACTION_START_SCAN = "com.google.zxing.client.android.SCAN";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_add_watch_address);
        getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getActionBar().setCustomView(R.layout.aegis_send_actionbar);

        titleTextView = (TextView) findViewById(R.id.action_bar_title_text);
        titleTextView.setText(getString(R.string.add_watch_address_header));

        application = (PayBitsApplication) getApplication();
        wallet = application.getWallet();
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        backButton = (ImageButton) findViewById(R.id.action_bar_icon_back);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent openSettingsActivity = new Intent(context, SettingsActivity.class);
                openSettingsActivity.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                context.startActivity(openSettingsActivity);
                finish();
            }
        });

        handleButtons();
    }

    private void handleButtons() {
        addressTextBox = (EditText) findViewById(R.id.watch_address_textbox);
        scanButton = (Button) findViewById(R.id.scan_qr_code_button);
        importButton = (Button) findViewById(R.id.confirm_add_private_key);

        importButton.setEnabled(true);

        importButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String addressString = addressTextBox.getText().toString();
                if (addressString != null) {
                    try {
                        Address address = new Address(Constants.NETWORK_PARAMETERS, addressString);

                        if (!wallet.isAddressWatched(address) && !WalletUtils.isAddressMine(wallet, address)) {
                            addKeyFlag = true;
                            wallet.addWatchedAddress(address);
                            showAddressAddedPrompt();
                        } else {
                            Toast.makeText(context, getString(R.string.already_watching_address), Toast.LENGTH_SHORT).show();
                        }
                    } catch (AddressFormatException e) {
                        Toast.makeText(context, getString(R.string.invalid_watch_address), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });



        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                decodeQRCode();
            }
        });
    }

    private void showAddressAddedPrompt(){
        final Dialog addressAddedDialog = new Dialog(context);
        addressAddedDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        addressAddedDialog.setContentView(R.layout.watch_address_added_prompt);

        Button cancelButton = (Button) addressAddedDialog.findViewById(R.id.watch_address_added_cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addressAddedDialog.dismiss();
            }
        });

        final Button okButton = (Button) addressAddedDialog.findViewById(R.id.watch_address_added_ok_button);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                application.deleteBlockchainAndRestartApp(context, addressAddedDialog);
                Intent intent = new Intent(context, MainActivity.class);
                startActivity(intent);
            }
        });

        addressAddedDialog.show();
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
            if (addKeyFlag) {
                addKeyFlag = false;

                List<ECKey> keyList = new ArrayList<ECKey>();
                keyList.add(keyToAdd);

                application.cancelNFCPrompt(context);
                DecryptWalletAndAddKeysTask decryptWalletAndAddKeysTask =
                        new DecryptWalletAndAddKeysTask(application,
                                application.getWallet(),
                                context,
                                resultString,
                                keyList);

                //decryptWalletAndAddKeysTask.execute();
                decryptWalletAndAddKeysTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
                //initiateSendCoins(amountStr, resultString);
            }
        } else {
            Toast.makeText(context, getString(R.string.nfc_tag_invalid_string), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == RESULT_OK) {
            String contents = data.getStringExtra("SCAN_RESULT");
            if (contents != null) {
                try {
                    Address address = new Address(Constants.NETWORK_PARAMETERS, contents);
                    addressTextBox.setText(contents);
                } catch (AddressFormatException e) {
                    Toast.makeText(context, getString(R.string.invalid_address_string), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void decodeQRCode() {
        Intent intent = new Intent(this, CaptureActivity.class);
        intent.setAction(ACTION_START_SCAN);

        intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
        startActivityForResult(intent, 1);
    }

}
