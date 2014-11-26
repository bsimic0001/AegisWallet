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
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.aegiswallet.PayBitsApplication;
import com.aegiswallet.R;
import com.aegiswallet.listeners.ImportCompletedListener;
import com.aegiswallet.tasks.DecryptWalletAndAddKeysTask;
import com.aegiswallet.tasks.ImportWalletTask;
import com.aegiswallet.utils.Constants;
import com.aegiswallet.utils.NfcUtils;
import com.aegiswallet.utils.WalletUtils;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.Wallet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by bsimic on 5/19/14.
 */
public class ImportWalletActivity extends Activity implements ImportCompletedListener {

    private String TAG = this.getClass().getName();

    private PayBitsApplication application;
    private Wallet wallet;
    private Uri backupFile;
    private String fileName;

    boolean isNFCEncrypted = false;
    boolean validFile = false;
    private boolean isWalletEncrypted;
    private boolean isWalletNFCEnabled;

    private TextView instructionText;
    private EditText passwordField;
    private EditText walletPasswordField;
    private Button continueButton;
    private ImageView nfcIcon;

    private List<ECKey> keyList;

    private Context context = this;
    private SharedPreferences prefs;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_import_from_email);

        instructionText = (TextView) findViewById(R.id.email_import_instruction);
        passwordField = (EditText) findViewById(R.id.import_backup_password_field);
        walletPasswordField = (EditText) findViewById(R.id.import_backup_wallet_password_field);
        continueButton = (Button) findViewById(R.id.backup_continue_button);
        nfcIcon = (ImageView) findViewById(R.id.backup_nfc_icon);

        this.application = (PayBitsApplication) getApplication();

        prefs = application.getPrefs();


        //If app is not initated, we send the user to the initiate screen.
        if (!prefs.getBoolean(Constants.APP_INIT_COMPLETE, false)) {
            Intent openMainActivity = new Intent(this, InitAppAction.class);
            openMainActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(openMainActivity);
            finish();
        }

        determineNFCEnabled();
        isWalletEncrypted = application.getWallet().isEncrypted();

        this.wallet = application.getWallet();

        Intent intent = getIntent();

        backupFile = intent.getData();

        if (backupFile != null) {
            application.setBackupFileUri(backupFile);
        }

        checkFile();
        setupViews();


    }

    private void setupViews() {
        if (validFile) {

            //Check if WALLET is encrypted
            if (isWalletEncrypted) {

                //If wallet is password encrypted AND file is password encrypted
                if (!isWalletNFCEnabled && !isNFCEncrypted) {
                    instructionText.setText(R.string.email_import_filepass_walletpass);
                    nfcIcon.setVisibility(View.GONE);
                    walletPasswordField.setVisibility(View.VISIBLE);
                    passwordField.setVisibility(View.VISIBLE);
                    continueButton.setVisibility(View.VISIBLE);
                    handleContinueButton();
                }
                //If wallet is NFC encrypted AND file IS Password Encrypted
                else if (isWalletNFCEnabled && !isNFCEncrypted) {
                    instructionText.setText(R.string.email_import_filepass_walletnfc);
                    nfcIcon.setVisibility(View.VISIBLE);
                    passwordField.setVisibility(View.VISIBLE);
                    walletPasswordField.setVisibility(View.GONE);
                    continueButton.setVisibility(View.GONE);
                }
                //If wallet is PASSWORD encrypted AND file IS NFC encrypted
                else if (!isWalletNFCEnabled && isNFCEncrypted) {
                    instructionText.setText(R.string.email_import_filenfc_walletpass);
                    walletPasswordField.setVisibility(View.VISIBLE);
                    nfcIcon.setVisibility(View.VISIBLE);

                    passwordField.setVisibility(View.GONE);
                    continueButton.setVisibility(View.GONE);
                }
                //File is NFC encrypted AND wallet is NFC encrypted
                else if (isWalletNFCEnabled && isNFCEncrypted) {
                    instructionText.setText(R.string.email_import_nfc_instruction);
                    nfcIcon.setVisibility(View.VISIBLE);

                    passwordField.setVisibility(View.GONE);
                    continueButton.setVisibility(View.GONE);
                    walletPasswordField.setVisibility(View.GONE);
                }

            }
            //Wallet is not encrypted
            else {

                walletPasswordField.setVisibility(View.GONE);

                //if file is NFC encrypted
                if (isNFCEncrypted) {
                    continueButton.setVisibility(View.GONE);
                    nfcIcon.setVisibility(View.VISIBLE);
                    passwordField.setVisibility(View.GONE);
                    instructionText.setText(R.string.email_import_nfc_instruction);
                }
                //if file is password encrypted
                else if (!isNFCEncrypted) {
                    instructionText.setText(R.string.email_import_password_instruction);
                    continueButton.setVisibility(View.VISIBLE);
                    nfcIcon.setVisibility(View.GONE);
                    passwordField.setVisibility(View.VISIBLE);
                    handleContinueButton();
                }
            }
            //Invalid file...show error message.
        } else {
            instructionText.setText(R.string.email_import_invalid_file);
            instructionText.setTextColor(getResources().getColor(R.color.custom_red));

            continueButton.setVisibility(View.GONE);
            nfcIcon.setVisibility(View.GONE);
            passwordField.setVisibility(View.GONE);
        }
    }

    private void handleContinueButton() {

        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String password = passwordField.getText().toString();

                boolean isFilePasswordCorrect = WalletUtils.checkPasswordForBackupFile(fileName, password);

                //if file is passwrod encrypted
                if (!isNFCEncrypted) {
                    if (isFilePasswordCorrect) {
                        //do something if the file passwrod is correct
                        if (application.getWallet().isEncrypted() && !isWalletNFCEnabled) {
                            if(checkWalletPassword()){
                                List<ECKey> keys = WalletUtils.restoreWalletFromBackupFile(fileName, password, application.getWallet(), false);
                                DecryptWalletAndAddKeysTask decryptWalletAndAddKeysTask = new DecryptWalletAndAddKeysTask(application, application.getWallet(), context, walletPasswordField.getText().toString(), keys);
                                //decryptWalletAndAddKeysTask.execute();
                                decryptWalletAndAddKeysTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
                            }
                            else{
                                Toast.makeText(context, context.getString(R.string.email_import_invalid_wallet_password), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            ImportWalletTask importWalletTask = new ImportWalletTask(application, application.getWallet(), context, password, fileName);
                            importWalletTask.execute();
                        }
                    } else {
                        Toast.makeText(context, context.getString(R.string.email_import_invalid_file_password), Toast.LENGTH_SHORT).show();
                    }
                }


                //If wallet is encrypted using a password...
                if (application.getWallet().isEncrypted() && !isWalletNFCEnabled) {
                    if (!checkWalletPassword()) {
                        Toast.makeText(context, context.getString(R.string.email_import_invalid_wallet_password), Toast.LENGTH_SHORT).show();
                    } else {
                        if (isFilePasswordCorrect) {

                        }
                    }
                }

            }
        });
    }

    private void checkFile() {

        ContentResolver contentResolver = getContentResolver();
        DateFormat dateFormat = Constants.backupDateFormat;
        dateFormat.setTimeZone(TimeZone.getDefault());
        String date = dateFormat.format(new Date());

        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {

            inputStream = contentResolver.openInputStream(application.getBackupFileUri());

            fileName = "FromEmail-" + Constants.WALLET_BACKUP_FILENAME + "-" + date;
            outputStream = new FileOutputStream(new File(Constants.WALLET_BACKUP_DIRECTORY, fileName));

            int read = 0;
            byte[] bytes = new byte[1024];

            while ((read = inputStream.read(bytes)) != -1) {

                String readStr = new String(bytes);
                if (readStr != null && readStr.contains("#ENCTYPE:PASSWORD")) {
                    validFile = true;
                    isNFCEncrypted = false;
                } else if (readStr != null && readStr.contains("#ENCTYPE:NFC")) {
                    validFile = true;
                    isNFCEncrypted = true;
                }

                outputStream.write(bytes, 0, read);
            }

        } catch (FileNotFoundException e) {
            Log.e(TAG, e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                }

            }
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

        byte[] result = NfcUtils.getData(intent);

        Vibrator v = (Vibrator) this.context.getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(500);

        String resultString = null;

        if (result != null)
            resultString = new String(result);


        //Means the wallet was already decrypted using NFC...
        //This should only be the case when the File AND wallet are NFC encrypted
        //If the provided password is correct for the wallet
        if (this.keyList != null && nfcWalletPasswordCorrect(resultString)) {

            application.cancelNFCPrompt(context);
            DecryptWalletAndAddKeysTask decryptWalletAndAddKeysTask =
                    new DecryptWalletAndAddKeysTask(application, application.getWallet(), context, resultString, this.keyList);

            //decryptWalletAndAddKeysTask.execute();
            decryptWalletAndAddKeysTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
            return;
        }
        //If wallet is NFC encrypted and file is password encrypted
        else if (isWalletNFCEnabled && application.getWallet().isEncrypted() && !isNFCEncrypted) {

            String filePassword = passwordField.getText().toString();

            if (!nfcWalletPasswordCorrect(resultString)) {
                Toast.makeText(context, R.string.email_import_invalid_wallet_password, Toast.LENGTH_LONG).show();
                return;
            }

            if (WalletUtils.checkPasswordForBackupFile(fileName, filePassword)) {
                List<ECKey> keys = WalletUtils.restoreWalletFromBackupFile(fileName, filePassword, application.getWallet(), false);

                DecryptWalletAndAddKeysTask decryptWalletAndAddKeysTask =
                        new DecryptWalletAndAddKeysTask(application, application.getWallet(), context, resultString, keys);

                //decryptWalletAndAddKeysTask.execute();
                decryptWalletAndAddKeysTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
                return;
            } else {
                Toast.makeText(context, R.string.email_import_invalid_file_password, Toast.LENGTH_LONG).show();
            }
        }
        //If wallet is passwrod encrypted and file is NFC encrypted
        else if (isNFCEncrypted && application.getWallet().isEncrypted() && !isWalletNFCEnabled) {

            String walletPassword = walletPasswordField.getText().toString();
            boolean walletPasswordValid = WalletUtils.checkPassword(walletPassword, prefs);

            if (walletPasswordValid) {
                List<ECKey> keys = WalletUtils.restoreWalletFromBackupFile(fileName, resultString, application.getWallet(), false);

                DecryptWalletAndAddKeysTask decryptWalletAndAddKeysTask =
                        new DecryptWalletAndAddKeysTask(application, application.getWallet(), context, walletPassword, keys);

                //decryptWalletAndAddKeysTask.execute();
                decryptWalletAndAddKeysTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
                return;
            } else {
                Toast.makeText(context, R.string.email_import_invalid_wallet_password, Toast.LENGTH_LONG).show();
                return;
            }

        } else if (this.keyList == null && isNFCEncrypted) {
            ImportWalletTask importWalletTask = new ImportWalletTask(application, application.getWallet(), context, resultString, fileName);
            //importWalletTask.execute();
            importWalletTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
        }
        //IF wallet is password encrypted AND file is password encrypted we don't want to do anything.
        else if (!isWalletEncrypted && !isNFCEncrypted) {
            return;
        }
    }

    @Override
    public void onImportCompleted(String fileName, List<ECKey> keyList) {
        this.keyList = keyList;

        if (!application.getWallet().isEncrypted()) {
            application.cancelNFCPrompt(context);
            application.showImportCompletedPrompt(context, fileName);
        } else if (isWalletNFCEnabled) {
            application.showNFCPrompt(context);
        }
    }

    private boolean checkWalletPassword() {

        boolean walletPassCorrect = true;

        if (isWalletEncrypted && !isWalletNFCEnabled) {
            //Handle wallet password
            walletPassCorrect = WalletUtils.checkPassword(walletPasswordField.getText().toString(), prefs);

            if (!walletPassCorrect) {
                Toast.makeText(context, context.getString(R.string.email_import_invalid_wallet_password), Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        return walletPassCorrect;
    }

    private boolean nfcWalletPasswordCorrect(String providedNFCPassword) {
        String shamirX2Hashed = application.getPrefs().getString(Constants.SHAMIR_X2_HASHED, null);
        String hashedProvidedPass = WalletUtils.convertToSha256(providedNFCPassword);

        if (shamirX2Hashed.equals(hashedProvidedPass))
            return true;
        else
            return false;
    }

    private void determineNFCEnabled() {
        isWalletNFCEnabled = prefs.contains(Constants.SHAMIR_ENCRYPTED_KEY) ? false : true;
    }
}
