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
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.text.method.LinkMovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.aegiswallet.PayBitsApplication;
import com.aegiswallet.R;
import com.aegiswallet.helpers.FileSelector;
import com.aegiswallet.helpers.PasswordCheckHelper;
import com.aegiswallet.listeners.ImportCompletedListener;
import com.aegiswallet.listeners.PasswordProvidedListener;
import com.aegiswallet.listeners.WalletDecryptedListener;
import com.aegiswallet.listeners.WalletEncryptedListener;
import com.aegiswallet.tasks.ChangePasswordTask;
import com.aegiswallet.tasks.DecryptWalletAndAddKeysTask;
import com.aegiswallet.tasks.DecryptWalletTask;
import com.aegiswallet.tasks.EncryptWalletTask;
import com.aegiswallet.utils.BasicUtils;
import com.aegiswallet.utils.Constants;
import com.aegiswallet.utils.NfcUtils;
import com.aegiswallet.utils.WalletUtils;
import com.google.bitcoin.core.ECKey;

import java.util.List;

/**
 * Created by bsimic on 5/12/14.
 */
public class SettingsActivity extends Activity implements WalletDecryptedListener, WalletEncryptedListener, ImportCompletedListener, PasswordProvidedListener {

    private String TAG = this.getClass().getName();

    Context context = this;
    SharedPreferences prefs;

    Button aboutButton;
    Button resetButton;
    Button changePasswordButton;
    Button restoreWalletButton;
    Button switchToPasswordButton;
    Button switchToNFCButton;
    Button importPrivateKey;
    Button copyNFCButton;

    FileSelector backupFileSelector;
    List<ECKey> keyList;

    private boolean nfcEnabled;
    private boolean switchToNFCFlag;

    PayBitsApplication application;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getActionBar().setCustomView(R.layout.aegis_send_actionbar);

        TextView titleTextView = (TextView) findViewById(R.id.action_bar_title_text);
        titleTextView.setText(getString(R.string.settings_string));

        String message = getIntent().getStringExtra("message");
        if(message != null && message.length() > 0){
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        }

        ImageButton backButton = (ImageButton) findViewById(R.id.action_bar_icon_back);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent openMainActivity = new Intent(context, MainActivity.class);
                openMainActivity.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                context.startActivity(openMainActivity);
                finish();
            }
        });

        application = (PayBitsApplication) getApplication();
        determineNFCEnabled();

        initButtons();
    }

    private void initButtons() {
        aboutButton = (Button) findViewById(R.id.settings_about_button);
        resetButton = (Button) findViewById(R.id.settings_reset_button);
        changePasswordButton = (Button) findViewById(R.id.settings_change_password_button);
        restoreWalletButton = (Button) findViewById(R.id.settings_import_button);
        switchToPasswordButton = (Button) findViewById(R.id.settings_switch_to_password_button);
        switchToNFCButton = (Button) findViewById(R.id.settings_switch_to_nfc_button);
        copyNFCButton = (Button) findViewById(R.id.settings_copy_nfc_tag_button);

        if (nfcEnabled) {
            switchToPasswordButton.setVisibility(View.VISIBLE);
            switchToNFCButton.setVisibility(View.GONE);
            copyNFCButton.setVisibility(View.VISIBLE);
        } else {
            changePasswordButton.setVisibility(View.VISIBLE);
            switchToNFCButton.setVisibility(View.VISIBLE);
            switchToPasswordButton.setVisibility(View.GONE);
            copyNFCButton.setVisibility(View.GONE);
        }
    }

    public void toggleAboutSection(View view) {
        Intent intent = new Intent(context, AboutActivity.class);
        context.startActivity(intent);
    }

    public void initBlockchainReset(View view) {

        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.settings_reset_blockchain);


        Button cancelButton = (Button) dialog.findViewById(R.id.settings_reset_cancel_button);
        Button okButton = (Button) dialog.findViewById(R.id.settings_reset_ok_button);

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                application.deleteBlockchainAndRestartApp(context, dialog);
            }
        });

        dialog.show();

    }

    public void initPasswordChange(View view) {
        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.settings_change_password);


        Button cancelButton = (Button) dialog.findViewById(R.id.settings_change_password_cancel_button);
        Button okButton = (Button) dialog.findViewById(R.id.settings_change_password_ok_button);

        final EditText currentPassword = (EditText) dialog.findViewById(R.id.settings_reset_password_current_field);
        final EditText newPassword = (EditText) dialog.findViewById(R.id.settings_reset_password_one_field);
        final EditText newPasswordConfirm = (EditText) dialog.findViewById(R.id.settings_reset_password_confirm_field);

        TextView passwordStrength = (TextView) dialog.findViewById(R.id.settings_password_strength_textbox);
        final PasswordCheckHelper passwordCheckHelper = new PasswordCheckHelper(context, null, passwordStrength, newPassword);
        passwordCheckHelper.setupProgressMeter();

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //do change password stuff
                String newPassString = newPassword.getText().toString();
                String newPassConfirmString = newPasswordConfirm.getText().toString();

                if (!newPassString.equals(newPassConfirmString)) {
                    Toast.makeText(context, getResources().getString(R.string.settings_change_password_mismatch), Toast.LENGTH_SHORT).show();
                } else if (!WalletUtils.checkPassword(currentPassword.getText().toString(), prefs)) {
                    Toast.makeText(context, getResources().getString(R.string.settings_change_password_wrong), Toast.LENGTH_SHORT).show();
                }
                else if(!BasicUtils.isPasswordInDictionary(context, newPassString)){
                    Toast.makeText(context, getString(R.string.password_guessable), Toast.LENGTH_LONG).show();
                }
                else if (passwordCheckHelper.getScore(newPassString) < Constants.MINIMUM_PASSWORD_SCORE) {
                    Toast.makeText(context, getResources().getString(R.string.invalid_password_strength), Toast.LENGTH_LONG).show();
                } else {

                    if (application.getWallet().isEncrypted()) {
                        ChangePasswordTask changePasswordTask = new ChangePasswordTask(context,
                                application.getWallet(),
                                application,
                                currentPassword.getText().toString(),
                                newPassword.getText().toString());

                        changePasswordTask.execute();
                    } else {
                        WalletUtils.changePassword(newPassword.getText().toString(), prefs);
                        Intent intent = new Intent(context, MainActivity.class);
                        intent.putExtra("message", context.getString(R.string.settings_password_changed));
                        startActivity(intent);
                    }

                }
            }
        });

        dialog.show();
    }

    public void initSwitchToNFC(View view) {

        switchToNFCFlag = true;

        if (application.getWallet().isEncrypted()) {
            application.showPasswordPrompt(context, Constants.ACTION_SWITCH_TO_NFC);
        } else {
            launchSwitchToNFCActivity(false);
        }
    }

    public void initNFCCopy(View view){
        Intent intent = new Intent(this, NFCActivity.class);
        intent.putExtra("nfc_action", "copy_tag");
        intent.putExtra("justDecrypted", false);
        startActivity(intent);
    }

    private void launchSwitchToNFCActivity(boolean justDecrypted) {
        Intent intent = new Intent(this, NFCActivity.class);
        intent.putExtra("nfc_action", "switch_to_nfc");
        intent.putExtra("justDecrypted", justDecrypted);
        startActivity(intent);
    }

    public void initSwitchToPassword(View view) {

        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.settings_switch_to_password);

        Button cancelButton = (Button) dialog.findViewById(R.id.settings_switch_to_password_cancel_button);
        Button okButton = (Button) dialog.findViewById(R.id.settings_switch_to_password_ok_button);

        final EditText newPassword = (EditText) dialog.findViewById(R.id.settings_switch_to_password_one_field);
        final EditText newPasswordConfirm = (EditText) dialog.findViewById(R.id.settings_switch_to_password_confirm_field);

        TextView passwordStrength = (TextView) dialog.findViewById(R.id.settings_password_strength_textbox);
        final PasswordCheckHelper passwordCheckHelper = new PasswordCheckHelper(context, null, passwordStrength, newPassword);
        passwordCheckHelper.setupProgressMeter();

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //do switch to password stuff
                String newPassString = newPassword.getText().toString();
                String newPassConfirmString = newPasswordConfirm.getText().toString();

                if (!newPassString.equals(newPassConfirmString)) {
                    Toast.makeText(context, getResources().getString(R.string.settings_change_password_mismatch), Toast.LENGTH_SHORT).show();
                }
                else if(!BasicUtils.isPasswordInDictionary(context, newPassString)){
                    Toast.makeText(context, getString(R.string.password_guessable), Toast.LENGTH_LONG).show();
                }
                else if (passwordCheckHelper.getScore(newPassString) < Constants.MINIMUM_PASSWORD_SCORE) {
                    Toast.makeText(context, getResources().getString(R.string.invalid_password_strength), Toast.LENGTH_LONG).show();
                } else {
                    dialog.dismiss();
                    Intent intent = new Intent(context, NFCActivity.class);
                    intent.putExtra("nfc_action", "switch_to_password");
                    intent.putExtra("new_password", newPassString);
                    startActivity(intent);
                }
            }
        });

        dialog.show();


    }

    public void initWalletImport(View view) {
        initiateBackupRestore();
    }

    public void initPrivateKeyImport(View view){
        Intent importKeyIntent = new Intent(this, ImportPrivateKeyActivity.class);
        startActivity(importKeyIntent);
    }

    public void initAddWatchAddress(View view){
        Intent addWatchAddress = new Intent(this, AddWatchAddressActivity.class);
        startActivity(addWatchAddress);
    }

    private void initiateBackupRestore() {
        backupFileSelector = new FileSelector(this, BasicUtils.loadFileList(), application);
        backupFileSelector.showFileSelector();
    }

    @Override
    public void onWalletDecrypted(String newPasswordString) {
        if (switchToNFCFlag) {
            launchSwitchToNFCActivity(true);
        } else {
            EncryptWalletTask encryptWalletTask = new EncryptWalletTask(context, application.getWallet(), newPasswordString, application, false);
            encryptWalletTask.execute();
        }
    }

    @Override
    public void onWalletEncrypted() {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("message", context.getString(R.string.settings_password_changed));
        startActivity(intent);
    }

    @Override
    public void onImportCompleted(String fileName, List<ECKey> keyList) {
        //Will NOT return a file name if the wallet was unencrypted.
        if (!application.getWallet().isEncrypted()) {
            application.showImportCompletedPrompt(context, fileName);
        } else {
            this.keyList = keyList;

            //IF the app has NFC enabled...
            if (nfcEnabled)
                application.showNFCPrompt(context);
            else {
                if (backupFileSelector != null
                        && backupFileSelector.getFilePassword() != null
                        && WalletUtils.checkPassword(backupFileSelector.getFilePassword(), prefs)
                        && this.keyList != null) {

                    DecryptWalletAndAddKeysTask decryptWalletAndAddKeysTask =
                            new DecryptWalletAndAddKeysTask(application,
                                    application.getWallet(),
                                    context,
                                    backupFileSelector.getFilePassword(),
                                    this.keyList);

                    decryptWalletAndAddKeysTask.execute();
                } else {
                    application.showPasswordPrompt(context, Constants.ACTION_RESTORE_BACKUP);
                }
            }
        }
    }

    @Override
    public void onPasswordProvided(String password, int action) {
        if (action == Constants.ACTION_RESTORE_BACKUP) {
            //Do action backup restore
            //This is in the event the wallet is previously encrypted.
            //If it was already decrypted, then the keys would already be added.

            if (WalletUtils.checkPassword(password, prefs) && keyList != null) {
                DecryptWalletAndAddKeysTask decryptWalletAndAddKeysTask = new DecryptWalletAndAddKeysTask(application, application.getWallet(), context, password, keyList);
                decryptWalletAndAddKeysTask.execute();
            }
        } else if (action == Constants.ACTION_SWITCH_TO_NFC) {
            DecryptWalletTask decryptWalletTask = new DecryptWalletTask(context, application.getWallet(), password, application);
            decryptWalletTask.execute();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        NfcUtils.listen(this, getClass());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (keyList != null && application.getWallet().isEncrypted()) {
            byte[] result = NfcUtils.getData(intent);

            Vibrator v = (Vibrator) this.context.getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(500);

            String resultString = null;

            if (result != null)
                resultString = new String(result);

            if (resultString != null) {
                DecryptWalletAndAddKeysTask decryptWalletAndAddKeysTask =
                        new DecryptWalletAndAddKeysTask(application, application.getWallet(), context, resultString, this.keyList);

                decryptWalletAndAddKeysTask.execute();
            }
        }
    }

    private void determineNFCEnabled() {
        nfcEnabled = prefs.contains(Constants.SHAMIR_ENCRYPTED_KEY) ? false : true;
    }
}
