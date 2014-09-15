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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.aegiswallet.PayBitsApplication;
import com.aegiswallet.R;
import com.aegiswallet.listeners.ImportCompletedListener;
import com.aegiswallet.listeners.PasswordProvidedListener;
import com.aegiswallet.listeners.WalletEncryptedListener;
import com.aegiswallet.tasks.DecryptWalletAndAddKeysTask;
import com.aegiswallet.tasks.EncryptWalletTask;
import com.aegiswallet.utils.Constants;
import com.aegiswallet.utils.NfcUtils;
import com.aegiswallet.utils.WalletUtils;
import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.DumpedPrivateKey;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.Wallet;
import com.google.zxing.client.android.CaptureActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bsimic on 6/6/14.
 */
public class ImportPrivateKeyActivity extends Activity implements WalletEncryptedListener, PasswordProvidedListener, ImportCompletedListener {

    private Button importButton;
    private ImageButton backButton;
    private TextView titleTextView;
    private EditText privateKeyTextBox;
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

        setContentView(R.layout.activity_import_private_key);
        getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getActionBar().setCustomView(R.layout.aegis_send_actionbar);

        titleTextView = (TextView) findViewById(R.id.action_bar_title_text);
        titleTextView.setText(getString(R.string.import_paper_wallet_header));

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

    @Override
    protected void onResume() {
        super.onResume();
        NfcUtils.listen(this, getClass());
    }

    private void handleButtons() {
        privateKeyTextBox = (EditText) findViewById(R.id.private_key_textbox);
        scanButton = (Button) findViewById(R.id.scan_qr_code_button);
        importButton = (Button) findViewById(R.id.confirm_add_private_key);

        importButton.setEnabled(true);

        importButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                determineNFCEnabled();
                String privKey = privateKeyTextBox.getText().toString();

                if (privKey != null) {
                    try {
                        DumpedPrivateKey key = new DumpedPrivateKey(Constants.NETWORK_PARAMETERS, privKey);
                        ECKey ecKey = key.getKey();

                        if (!wallet.hasKey(ecKey)) {
                            addKeyFlag = true;
                            initiateKeyImport(ecKey);
                        } else {
                            Toast.makeText(context, getString(R.string.wallet_already_has_key), Toast.LENGTH_SHORT).show();
                        }

                    } catch (AddressFormatException e) {
                        Toast.makeText(context, getString(R.string.invalid_private_key), Toast.LENGTH_SHORT).show();
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

                decryptWalletAndAddKeysTask.execute();
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
                    DumpedPrivateKey key = new DumpedPrivateKey(Constants.NETWORK_PARAMETERS, contents);
                    privateKeyTextBox.setText(contents);
                    Toast.makeText(context, getString(R.string.valid_private_key), Toast.LENGTH_SHORT).show();
                } catch (AddressFormatException e) {
                    Toast.makeText(context, getString(R.string.invalid_private_key), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void initiateKeyImport(ECKey key) {
        keyToAdd = key;

        if (nfcEnabled) {
            application.showNFCPrompt(context);
        } else {
            application.showPasswordPrompt(context, Constants.ACTION_IMPORT_KEY);
        }
    }

    private void decodeQRCode() {
        Intent intent = new Intent(this, CaptureActivity.class);
        intent.setAction(ACTION_START_SCAN);

        intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
        startActivityForResult(intent, 1);
    }

    private void determineNFCEnabled() {
        nfcEnabled = prefs.contains(Constants.SHAMIR_ENCRYPTED_KEY) ? false : true;
    }

    @Override
    public void onPasswordProvided(String password, int encrypt) {
        if (keyToAdd != null) {

            List<ECKey> keyList = new ArrayList<ECKey>();
            keyList.add(keyToAdd);

            passwordOrX2 = password;

            DecryptWalletAndAddKeysTask decryptWalletAndAddKeysTask =
                    new DecryptWalletAndAddKeysTask(application,
                            application.getWallet(),
                            context,
                            password,
                            keyList);

            decryptWalletAndAddKeysTask.execute();
        }
    }

    @Override
    public void onImportCompleted(String fileName, List<ECKey> keyList) {
        EncryptWalletTask encryptWalletTask;

        if (passwordOrX2 != null) {
            if (nfcEnabled)
                encryptWalletTask = new EncryptWalletTask(context, wallet, passwordOrX2, application, true);
            else
                encryptWalletTask = new EncryptWalletTask(context, wallet, passwordOrX2, application, false);

            encryptWalletTask.execute();
        } else {
            application.showImportCompletedPrompt(context, null);
        }

    }

    @Override
    public void onWalletEncrypted() {
        application.showImportCompletedPrompt(context, null);
    }
}
