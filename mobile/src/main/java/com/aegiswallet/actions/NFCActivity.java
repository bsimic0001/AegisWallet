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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.aegiswallet.PayBitsApplication;
import com.aegiswallet.R;
import com.aegiswallet.listeners.BackupCompletedListener;
import com.aegiswallet.listeners.ImportCompletedListener;
import com.aegiswallet.listeners.PasswordProvidedListener;
import com.aegiswallet.listeners.WalletDecryptedListener;
import com.aegiswallet.listeners.WalletEncryptedListener;
import com.aegiswallet.tasks.BackupWalletTask;
import com.aegiswallet.tasks.DecryptWalletAndAddKeysTask;
import com.aegiswallet.tasks.DecryptWalletTask;
import com.aegiswallet.tasks.EncryptWalletTask;
import com.aegiswallet.tasks.ImportWalletTask;
import com.aegiswallet.utils.BasicUtils;
import com.aegiswallet.utils.Constants;
import com.aegiswallet.utils.NfcUtils;
import com.aegiswallet.utils.WalletUtils;
import com.google.bitcoin.core.ECKey;

import java.util.List;

/**
 * Created by bsimic on 5/12/14.
 */
public class NFCActivity extends Activity implements
        WalletDecryptedListener,
        WalletEncryptedListener,
        BackupCompletedListener,
        ImportCompletedListener,
        PasswordProvidedListener {

    private String TAG = this.getClass().getName();

    private String action;
    private Context context = this;
    private PayBitsApplication application;
    private String fileNameForRestoreBackup;

    private TextView messageTextView;

    private boolean restoreBackupFile;
    private boolean restoreBackupFilePart2;

    private boolean switchToPassword;
    private boolean switchToNFC = false;
    private String newPassword;

    private boolean copyStepOne;
    private boolean copyStepTwo;
    private String copyString;

    private long lastScanTime = 0;

    private List<ECKey> providedKeyList;

    private boolean nfcEnabled;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_nfc);
        messageTextView = (TextView) findViewById(R.id.nfc_activity_message);

        action = getIntent().getStringExtra("nfc_action");

        if (action != null && action.equals("restore_backup")) {
            fileNameForRestoreBackup = getIntent().getStringExtra("fileNameForRestoreBackup");
            messageTextView.setText(getString(R.string.nfc_activity_message) + " " + fileNameForRestoreBackup);
            messageTextView.setVisibility(View.VISIBLE);
            restoreBackupFile = true;
        } else if (action != null && action.equals("switch_to_password")) {
            newPassword = getIntent().getStringExtra("new_password");
            switchToPassword = true;
        } else if (action != null && action.equals("switch_to_nfc")) {
            switchToNFC = true;
        } else if (action != null && action.equals("copy_tag")) {
            messageTextView.setText(getString(R.string.nfc_activity_message_main));
            copyStepOne = true;
        }

        application = (PayBitsApplication) getApplication();
        determineNFCEnabled();
    }

    @Override
    protected void onResume() {
        super.onResume();

        NfcUtils.listen(this, getClass());
        determineNFCEnabled();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if(System.currentTimeMillis() - lastScanTime < 1500)
            return;

        if (switchToNFC && !application.getWallet().isEncrypted()) {
            String x2Value = application.getPrefs().getString(Constants.SHAMIR_ENCRYPTED_KEY, null);

            if (x2Value != null) {
                String result = NfcUtils.write(intent, x2Value.getBytes());

                Vibrator v = (Vibrator) this.context.getSystemService(Context.VIBRATOR_SERVICE);
                v.vibrate(500);

                if (result.equals(Constants.NFC_TAG_WRITTEN)) {

                    EncryptWalletTask encryptWalletTask = new EncryptWalletTask(context,
                            application.getWallet(),
                            application.getPrefs().getString(Constants.SHAMIR_ENCRYPTED_KEY, null),
                            application,
                            true);

                    encryptWalletTask.execute();
                    application.getPrefs().edit().putString(Constants.SHAMIR_X2_HASHED, WalletUtils.convertToSha256(x2Value)).commit();
                    application.getPrefs().edit().remove(Constants.SHAMIR_ENCRYPTED_KEY).commit();

                } else {
                    String errorMessage = BasicUtils.getNFCErrorMessage(context, result);
                    if (errorMessage != null)
                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show();
                }
            }

            return;
        }

        byte[] result = NfcUtils.getData(intent);

        if(result != null){
            lastScanTime = System.currentTimeMillis();
        }
        Vibrator v = (Vibrator) this.context.getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(500);

        String shamirX2Hashed = application.getPrefs().getString(Constants.SHAMIR_X2_HASHED, null);

        String resultString = null;

        if (result != null)
            resultString = new String(result);

        if(copyStepOne){

            String resultSha256Value = WalletUtils.convertToSha256(resultString);
            String currentX2ShaValue = shamirX2Hashed;

            if(resultSha256Value != null && resultSha256Value.equals(currentX2ShaValue)){
                copyString = resultString;
                messageTextView.setText(getString(R.string.nfc_tag_copy_steptwo_message));
                messageTextView.setVisibility(View.VISIBLE);
                copyStepTwo = true;
                copyStepOne = false;
            }
            else{
                Toast.makeText(context, getString(R.string.nfc_tag_copy_invalid_string), Toast.LENGTH_SHORT).show();
            }
        }
        else if(copyStepTwo){
            String writeResult = NfcUtils.write(intent, copyString.getBytes());
            v.vibrate(500);

            if (writeResult.equals(Constants.NFC_TAG_WRITTEN)) {
                Intent openSettingsActivity = new Intent(context, SettingsActivity.class);
                openSettingsActivity.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                openSettingsActivity.putExtra("message", getString(R.string.nfc_tag_copy_successful));
                context.startActivity(openSettingsActivity);
                finish();
            }
        }

        if (resultString != null && restoreBackupFile) {
            ImportWalletTask importWalletTask = new ImportWalletTask(application, application.getWallet(), context, resultString, fileNameForRestoreBackup);
            importWalletTask.execute();
            return;
        }

        if (resultString != null && shamirX2Hashed.equals(WalletUtils.convertToSha256(resultString)) && action != null) {
            if (action.equals("decrypt")) {
                DecryptWalletTask decryptWalletTask = new DecryptWalletTask(context, application.getWallet(), resultString, application);
                decryptWalletTask.execute();
            } else if (action.equals("encrypt")) {
                EncryptWalletTask encryptWalletTask = new EncryptWalletTask(context, application.getWallet(), resultString, application, true);
                encryptWalletTask.execute();
            } else if (action.equals("backup")) {
                BackupWalletTask backupWalletTask = new BackupWalletTask(application, context, application.getWallet(), resultString);
                backupWalletTask.execute();
            } else if (restoreBackupFilePart2) {
                DecryptWalletAndAddKeysTask decryptWalletAndAddKeysTask = new DecryptWalletAndAddKeysTask(application, application.getWallet(), context, resultString, providedKeyList);
                decryptWalletAndAddKeysTask.execute();
            } else if (switchToPassword) {

                if (application.getWallet().isEncrypted()) {
                    DecryptWalletTask decryptWalletTask = new DecryptWalletTask(context, application.getWallet(), resultString, application);
                    decryptWalletTask.execute();
                } else {
                    doPasswordReset(false, resultString);
                }
            }
        } else {
            if(!action.equals("copy_tag"))
                Toast.makeText(context, getString(R.string.nfc_tag_invalid_string), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onWalletDecrypted(String passOrNFC) {
        if (switchToPassword && newPassword != null) {
            doPasswordReset(true, passOrNFC);
        } else {
            startMainActivity();
        }
    }

    public void doPasswordReset(boolean justDecrypted, String valueFromNFC) {

        if (newPassword != null) {
            application.getPrefs().edit().putString(Constants.SHAMIR_ENCRYPTED_KEY, valueFromNFC).commit();
            String passwordSalt = BasicUtils.generateSecureKey();
            String passwordHash = passwordSalt + WalletUtils.convertToSha256(passwordSalt + newPassword);

            application.getPrefs().edit().putString(Constants.PASSWORD_HASH, passwordHash).commit();
            application.getPrefs().edit().putString(Constants.PASSWORD_SALT, passwordSalt).commit();

            application.setKeyCache(null);

            if (justDecrypted) {
                EncryptWalletTask encryptWalletTask = new EncryptWalletTask(context, application.getWallet(), newPassword, application, false);
                encryptWalletTask.execute();
            } else {
                startMainActivity();
            }

        }
    }

    @Override
    public void onWalletEncrypted() {
        startMainActivity();
    }

    @Override
    public void onImportCompleted(String fileName, List<ECKey> keyList) {
        if (!application.getWallet().isEncrypted()) {
            application.showImportCompletedPrompt(context, fileNameForRestoreBackup);
        } else if (restoreBackupFile && nfcEnabled) {
            providedKeyList = keyList;
            restoreBackupFile = false;
            restoreBackupFilePart2 = true;
            messageTextView.setText(getString(R.string.nfc_activity_message_main));
        } else if (restoreBackupFile && !nfcEnabled) {
            providedKeyList = keyList;
            restoreBackupFile = false;
            application.showPasswordPrompt(this, Constants.ACTION_RESTORE_BACKUP);
            restoreBackupFilePart2 = true;

        } else if (restoreBackupFilePart2) {
            //application.deleteBlockchainAndRestartApp(context, null);
            application.showImportCompletedPrompt(context, fileNameForRestoreBackup);
            restoreBackupFilePart2 = false;
        }
    }

    @Override
    public void onBackupCompleted(String fileName) {

        Intent openMainActivity = new Intent(this, MainActivity.class);
        openMainActivity.putExtra("backup_done", true);
        openMainActivity.putExtra("backup_file", fileName);

        startActivity(openMainActivity);
    }

    private void startMainActivity() {
        Intent openMainActivity = new Intent(context, MainActivity.class);
        openMainActivity.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        context.startActivity(openMainActivity);
        finish();
    }

    private void determineNFCEnabled() {
        nfcEnabled = application.getPrefs().contains(Constants.SHAMIR_ENCRYPTED_KEY) ? false : true;
    }

    @Override
    //This is IF the wallet is password encrypted but we are importing a NFC encrypted file.
    public void onPasswordProvided(String password, int encrypt) {
        if (restoreBackupFilePart2 && providedKeyList != null) {
            DecryptWalletAndAddKeysTask decryptWalletAndAddKeysTask = new DecryptWalletAndAddKeysTask(application, application.getWallet(), context, password, providedKeyList);
            decryptWalletAndAddKeysTask.execute();
        }
    }
}
