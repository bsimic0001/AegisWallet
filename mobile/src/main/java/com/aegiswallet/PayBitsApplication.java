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

package com.aegiswallet;

import android.app.AlarmManager;
import android.app.Application;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.aegiswallet.actions.MainActivity;
import com.aegiswallet.helpers.secretshare.SecretShare;
import com.aegiswallet.listeners.PasswordProvidedListener;
import com.aegiswallet.listeners.WalletUpdateListener;
import com.aegiswallet.objects.KeyCache;
import com.aegiswallet.services.PeerBlockchainService;
import com.aegiswallet.tasks.PopulateContactsTask;
import com.aegiswallet.tasks.SendMessageTask;
import com.aegiswallet.tasks.SendShamirValueTask;
import com.aegiswallet.utils.Constants;
import com.aegiswallet.utils.WalletUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.store.UnreadableWalletException;
import com.google.bitcoin.store.WalletProtobufSerializer;
import com.google.bitcoin.wallet.WalletFiles;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

/**
 * Created by bsimic on 2/12/14.
 */
public class PayBitsApplication extends Application {

    private String TAG = this.getClass().getName();

    private Wallet wallet;
    private File walletFile;
    private Intent blockchainServiceIntent;
    private Intent blockchainServiceResetBlockchainIntent;
    private WalletUpdateListener walletListener;
    private KeyCache keyCache;
    private Dialog passwordDialog;
    private Dialog nfcDialog;
    private Uri backupFileUri;
    public static long lastReminderTime;
    private ArrayList<Map<String, String>> peopleList;
    private GoogleApiClient mGoogleApiClient;
    private SharedPreferences prefs;

    public static final NetworkParameters params = Constants.NETWORK_PARAMETERS;

    @Override
    public void onCreate() {

        walletFile = getFileStreamPath(Constants.WALLET_FILENAME_PROTOBUF);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        migrateWalletToProtobuf();
        loadWalletFromProtobuf();

        keyCache = null;
        wallet.autosaveToFile(walletFile, 1, TimeUnit.SECONDS, new WalletAutosaveEventListener());
        blockchainServiceResetBlockchainIntent = new Intent(PeerBlockchainService.ACTION_RESET_BLOCKCHAIN, null, this, PeerBlockchainService.class);
        blockchainServiceIntent = new Intent(this, PeerBlockchainService.class);

        peopleList = new ArrayList<Map<String, String>>();
        PopulateContactsTask populateContactsTask = new PopulateContactsTask(getApplicationContext(), peopleList);
        populateContactsTask.execute();
        setKeyDefaults();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new ConnectionCallbacks())
                .addOnConnectionFailedListener(new ConnectionFailedListener())
                .addApi(Wearable.API)
                .build();

        if (null != mGoogleApiClient && !mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
    }

    public void sendMessage(String type, String data){
        SendMessageTask sendMessageTask = new SendMessageTask(mGoogleApiClient, type, data, prefs);
        sendMessageTask.execute();
    }

    public GoogleApiClient getmGoogleApiClient(){
        return this.mGoogleApiClient;
    }

    private class ConnectionCallbacks implements
            GoogleApiClient.ConnectionCallbacks {
        @Override
        public void onConnected(Bundle bundle) {
            Log.d(TAG, "connected to device...calling send message");

            sendMessage("MessageAddress", prefs.getString(Constants.PREFS_KEY_SELECTED_ADDRESS, null));

            if(wallet != null) {
                String currencyValue = WalletUtils.getWalletCurrencyValue(getApplicationContext(),
                        prefs, wallet.getBalance(Wallet.BalanceType.ESTIMATED));

                String currencyType = prefs.getString(Constants.CURRENCY_PREF_KEY, null);
                sendMessage("MessageBalance", currencyValue);
            }

        }

        @Override
        public void onConnectionSuspended(int i) {
            Log.d(TAG, "Connection suspended");
        }
    }

    private class ConnectionFailedListener implements
            GoogleApiClient.OnConnectionFailedListener {
        @Override
        public void onConnectionFailed(ConnectionResult result) {
            Log.d(TAG, "Connection failed...");
        }
    }

    public void addWalletListener(Handler handler) {
        walletListener = new WalletUpdateListener(handler);
        wallet.addEventListener(walletListener);
    }

    public Wallet getWallet() {
        return wallet;
    }

    private void migrateWalletToProtobuf() {
        final File oldWalletFile = getFileStreamPath(Constants.WALLET_FILENAME);

        if (oldWalletFile.exists()) {
            final long start = System.currentTimeMillis();

            // read
            wallet = restoreWalletFromBackup();

            try {
                // write
                protobufSerializeWallet(wallet);

                // delete
                oldWalletFile.delete();
            } catch (final IOException x) {
                throw new Error("cannot migrate wallet", x);
            }
        } else {
            Log.i("PB App", "File does not exit");
        }
    }

    private void loadWalletFromProtobuf() {
        if (walletFile.exists()) {
            final long start = System.currentTimeMillis();

            FileInputStream walletStream = null;

            try {
                walletStream = new FileInputStream(walletFile);
                wallet = new WalletProtobufSerializer().readWallet(walletStream);
            } catch (final FileNotFoundException x) {
                Toast.makeText(PayBitsApplication.this, x.getClass().getName(), Toast.LENGTH_LONG).show();
                wallet = restoreWalletFromBackup();
            } catch (final UnreadableWalletException x) {
                Toast.makeText(PayBitsApplication.this, x.getClass().getName(), Toast.LENGTH_LONG).show();
                wallet = restoreWalletFromBackup();
            } finally {
                if (walletStream != null) {
                    try {
                        walletStream.close();
                    } catch (final IOException x) {
                        // swallow
                    }
                }
            }

            if (!wallet.getParams().equals(params))
                throw new Error("bad wallet network parameters: " + wallet.getParams().getId());
        } else {
            wallet = new Wallet(params);
        }
    }

    private void protobufSerializeWallet(@Nonnull final Wallet wallet) throws IOException {
        wallet.saveToFile(walletFile);
    }

    private Wallet restoreWalletFromBackup() {

        try {
            final Wallet wallet = readKeys(openFileInput(Constants.WALLET_KEY_BACKUP_BASE58));

            resetBlockchain();

            Toast.makeText(this, "wallet reset", Toast.LENGTH_LONG).show();
            return wallet;
        } catch (final IOException x) {
            throw new RuntimeException(x);
        }
    }

    public void startBlockchainService(final boolean cancelCoinsReceived) {
        startService(blockchainServiceIntent);
    }

    public void stopBlockchainService() {
        stopService(blockchainServiceIntent);
    }

    private void setKeyDefaults() {
        for (final ECKey key : wallet.getKeys())
            if (!wallet.isKeyRotating(key)) {
                return; // found
            }

        addNewKeyToWallet();
    }

    public void addNewKeyToWallet() {
        wallet.addKey(new ECKey());
        prefs.edit().putBoolean(Constants.PREFS_KEY_REMIND_BACKUP, true).commit();
    }

    private static Wallet readKeys(@Nonnull final InputStream is) throws IOException {
        final BufferedReader in = new BufferedReader(new InputStreamReader(is, Constants.UTF_8));
        final List<ECKey> keys = WalletUtils.readKeys(in);
        in.close();

        final Wallet wallet = new Wallet(Constants.NETWORK_PARAMETERS);
        for (final ECKey key : keys)
            wallet.addKey(key);

        return wallet;
    }

    public void resetBlockchain() {
        // actually stops the service
        startService(blockchainServiceResetBlockchainIntent);
    }

    private static final class WalletAutosaveEventListener implements WalletFiles.Listener {
        @Override
        public void onBeforeAutoSave(final File file) {
        }

        @Override
        public void onAfterAutoSave(final File file) {

        }
    }

    public void showImportCompletedPrompt(final Context context, String fileName) {

        final Dialog importCompleteDialog = new Dialog(context);
        importCompleteDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        importCompleteDialog.setContentView(R.layout.import_completed_prompt);

        Button cancelButton = (Button) importCompleteDialog.findViewById(R.id.import_prompt_cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                importCompleteDialog.dismiss();
            }
        });

        TextView fileNameView = (TextView) importCompleteDialog.findViewById(R.id.import_prompt_completed_filename_message);


        if(fileName != null)
            fileNameView.setText(context.getString(R.string.import_completed_start) + " " + fileName);

        final Button okButton = (Button) importCompleteDialog.findViewById(R.id.import_prompt_ok_button);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteBlockchainAndRestartApp(context, importCompleteDialog);
                //System.exit(0);
            }
        });

        importCompleteDialog.show();
    }

    public void deleteBlockchainAndRestartApp(Context context, Dialog dialog){
        Log.d(TAG, "RESETTING BLOCKCHAIN");
        resetBlockchain();

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, MainActivity.class);
        //PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        Date date = new Date();
        //alarmManager.set(AlarmManager.RTC_WAKEUP, date.getTime() + 3000, pendingIntent);

        if(dialog != null)
            dialog.dismiss();

        context.startActivity(intent);

        //android.os.Process.killProcess(android.os.Process.myPid());
    }

    public void showNFCPrompt(final Context context){
        nfcDialog = new Dialog(context);
        nfcDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        nfcDialog.setContentView(R.layout.nfc_prompt);

        Button cancel = (Button) nfcDialog.findViewById(R.id.nfc_prompt_cancel_button);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nfcDialog.dismiss();
            }
        });

        nfcDialog.show();
    }

    public void cancelNFCPrompt(final Context context){
        if(nfcDialog != null)
            nfcDialog.dismiss();
    }

    public void showPasswordPrompt(final Context context, final int action) {
        passwordDialog = new Dialog(context);
        passwordDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        passwordDialog.setContentView(R.layout.password_prompt);

        Button cancelButton = (Button) passwordDialog.findViewById(R.id.password_prompt_cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                passwordDialog.dismiss();
            }
        });

        final Button okayButton = (Button) passwordDialog.findViewById(R.id.password_prompt_encrypt_button);
        final EditText passwordInput = (EditText) passwordDialog.findViewById(R.id.password_prompt_field);

        okayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String password = passwordInput.getText().toString();
                if (WalletUtils.checkPassword(password, prefs)) {

                    passwordDialog.dismiss();

                    ((PasswordProvidedListener) context).onPasswordProvided(password, action);
                } else {
                    Toast.makeText(context, getString(R.string.invalid_password_string), Toast.LENGTH_LONG).show();
                }
            }
        });

        CheckBox passwordCheckbox = (CheckBox) passwordDialog.findViewById(R.id.password_checkbox);
        passwordCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    passwordInput.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
                } else {
                    passwordInput.setInputType(129);
                }
            }
        });

        passwordDialog.show();
    }

    public void initializeShamirSecretSharing(Context context) {

        if (!prefs.contains(Constants.SHAMIR_LOCAL_KEY)) {
            SecureRandom r = new SecureRandom();
            BigInteger secret = new BigInteger(256, r);

            BigInteger prime = BigInteger.probablePrime(256 + 64, r);

            SecretShare.PublicInfo publicInfo = new SecretShare.PublicInfo(Constants.SHAMIR_N, Constants.SHAMIR_K, prime, null);
            SecretShare secretShare = new SecretShare(publicInfo);

            SecretShare.SplitSecretOutput splitSecretOutput = secretShare.split(secret, r);
            List<SecretShare.ShareInfo> infos = splitSecretOutput.getShareInfos();

            if (infos != null) {
                SecretShare.ShareInfo shareInfo1 = infos.get(0);
                SecretShare.ShareInfo shareInfo2 = infos.get(1);
                SecretShare.ShareInfo shareInfo3 = infos.get(2);

                if (shareInfo1 != null && shareInfo2 != null && shareInfo3 != null) {

                    prefs.edit().putString(Constants.SHAMIR_LOCAL_KEY, shareInfo1.getX() + ":" + shareInfo1.getShare().toString()).commit();
                    prefs.edit().putString(Constants.SHAMIR_ENCRYPTED_KEY, shareInfo2.getX() + ":" + shareInfo2.getShare().toString()).commit();
                    prefs.edit().putString(Constants.SHAMIR_EXPORTED_KEY, shareInfo3.getX() + ":" + shareInfo3.getShare().toString()).commit();
                    prefs.edit().putString(Constants.SHAMIR_EXPORTED_KEY_SHA256, WalletUtils.convertToSha256(shareInfo3.getX() + ":" + shareInfo3.getShare().toString())).commit();

                }
            }
        }
        Log.d(TAG, "About to send shamir");

        if(prefs.contains(Constants.SHAMIR_EXPORTED_KEY)){
            SendShamirValueTask sendShamirValueTask = new SendShamirValueTask(context, prefs.getString(Constants.SHAMIR_EXPORTED_KEY, null), this);
            sendShamirValueTask.execute();
        }
    }

    public SharedPreferences getPrefs() {
        return prefs;
    }

    public void setKeyCache(KeyCache keyCache) {
        this.keyCache = keyCache;
    }

    public KeyCache getKeyCache() {
        return this.keyCache;
    }

    public void setBackupFileUri(Uri uri){
        this.backupFileUri = uri;
    }

    public Uri getBackupFileUri(){
        return this.backupFileUri;
    }

    public ArrayList<Map<String, String>> getPeopleList(){
        return this.peopleList;
    };

}
