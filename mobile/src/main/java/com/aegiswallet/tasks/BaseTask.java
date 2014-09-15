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

package com.aegiswallet.tasks;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.aegiswallet.PayBitsApplication;
import com.aegiswallet.objects.KeyCache;
import com.aegiswallet.utils.Constants;
import com.aegiswallet.utils.WalletUtils;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.crypto.KeyCrypter;
import com.google.bitcoin.crypto.KeyCrypterException;
import com.google.bitcoin.crypto.KeyCrypterScrypt;

import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.crypto.params.KeyParameter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.text.DateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by bsimic on 5/6/14.
 */
public class BaseTask extends AsyncTask {

    private String TAG = this.getClass().getName();

    @Override
    protected Object doInBackground(Object[] objects) {
        return null;
    }

    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);
    }

    public void encryptWalletShamirNFC(Wallet wallet, String x2, PayBitsApplication application) {
        String x1 = application.getPrefs().getString(Constants.SHAMIR_LOCAL_KEY, null);

        BigInteger result = WalletUtils.generateSecretFromStrings(x1, x2, null);
        String resultHashed = WalletUtils.convertToSha256(result.toString());

        KeyCrypter keyCrypter = wallet.getKeyCrypter();

        if (keyCrypter == null)
            keyCrypter = new KeyCrypterScrypt();

        KeyParameter aesKey = keyCrypter.deriveKey(resultHashed);
        wallet.encrypt(keyCrypter, aesKey);

        application.setKeyCache(new KeyCache(aesKey, keyCrypter, null));
    }

    public void encryptWalletWithShamir(Wallet wallet, String passOrNfc, PayBitsApplication application) {
        String x1 = application.getPrefs().getString(Constants.SHAMIR_LOCAL_KEY, null);
        String x2;

        if (application.getPrefs().contains(Constants.SHAMIR_ENCRYPTED_KEY)) {
            x2 = application.getPrefs().getString(Constants.SHAMIR_ENCRYPTED_KEY, null);
        } else {
            x2 = passOrNfc;
        }

        BigInteger result = WalletUtils.generateSecretFromStrings(x1, x2, null);

        if (result != null) {

            String resultHashed = WalletUtils.convertToSha256(result.toString());

            KeyCrypter keyCrypter = wallet.getKeyCrypter();
            if (keyCrypter == null)
                keyCrypter = new KeyCrypterScrypt();
            KeyParameter aesKey = keyCrypter.deriveKey(resultHashed);

            application.setKeyCache(new KeyCache(aesKey, keyCrypter, passOrNfc));
            wallet.encrypt(keyCrypter, aesKey);

            //only add the encrypted shamir key if nfc is not enabled....
            if (application.getPrefs().contains(Constants.SHAMIR_ENCRYPTED_KEY)) {
                String encryptedX2 = WalletUtils.encryptString(x2, passOrNfc);

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(application.getApplicationContext());
                SharedPreferences.Editor editor= prefs.edit();
                editor.putString(Constants.SHAMIR_ENCRYPTED_KEY, encryptedX2);
                editor.commit();
            }
        }
    }

    public void decryptWallet(PayBitsApplication application, Wallet wallet, String passOrNFC) {

        String x2;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(application.getApplicationContext());
        SharedPreferences.Editor editor = prefs.edit();

        if (application.getKeyCache() != null) {
            try {
                wallet.decrypt(application.getKeyCache().getAesKey());

                if (application.getPrefs().contains(Constants.SHAMIR_ENCRYPTED_KEY)) {
                    x2 = WalletUtils.decryptString(application.getPrefs().getString(Constants.SHAMIR_ENCRYPTED_KEY, null), application.getKeyCache().getPassword());
                    editor.putString(Constants.SHAMIR_ENCRYPTED_KEY, x2);
                    editor.commit();
                }
            } catch (Exception e) {
                Log.e(TAG, "unable to decrypt: " + e.getMessage());
            }
        } else {

            if (application.getPrefs().contains(Constants.SHAMIR_ENCRYPTED_KEY)) {
                x2 = WalletUtils.decryptString(application.getPrefs().getString(Constants.SHAMIR_ENCRYPTED_KEY, null), passOrNFC);
                editor.putString(Constants.SHAMIR_ENCRYPTED_KEY, x2);

            } else {
                x2 = passOrNFC;
            }

            String x1 = application.getPrefs().getString(Constants.SHAMIR_LOCAL_KEY, null);

            BigInteger result = WalletUtils.generateSecretFromStrings(x1, x2, null);
            String resultHashed = WalletUtils.convertToSha256(result.toString());

            try {
                KeyParameter aesKey = wallet.getKeyCrypter().deriveKey(resultHashed);
                application.setKeyCache(new KeyCache(aesKey, wallet.getKeyCrypter(), passOrNFC));

                wallet.decrypt(aesKey);
                editor.commit();
            } catch (KeyCrypterException e) {
                Log.e(TAG, e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    public void updateEncryptedX2(PayBitsApplication application, KeyCache keyCache) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(application.getApplicationContext());
        SharedPreferences.Editor editor= prefs.edit();

        //ONly update encrypted x2 if we are NOT using NFC
        if (application.getPrefs().contains(Constants.SHAMIR_ENCRYPTED_KEY)) {
            String x2 = application.getPrefs().getString(Constants.SHAMIR_ENCRYPTED_KEY, null);
            String encryptedX2 = WalletUtils.encryptString(x2, keyCache.getPassword());

            editor.putString(Constants.SHAMIR_ENCRYPTED_KEY, encryptedX2);
            editor.commit();
        }
    }

    public String doWalletBackup(Wallet wallet, boolean justDecrypted, PayBitsApplication application, String passOrNFC) {
        if (!wallet.isEncrypted()) {

            KeyCache keyCache = application.getKeyCache();

            try {
                Constants.WALLET_BACKUP_DIRECTORY.mkdirs();

                DateFormat dateFormat = Constants.backupDateFormat;
                dateFormat.setTimeZone(TimeZone.getDefault());
                String date = dateFormat.format(new Date());
                File file = new File(Constants.WALLET_BACKUP_DIRECTORY, Constants.WALLET_BACKUP_FILENAME + "-"
                        + date);

                List<ECKey> keys = new LinkedList<ECKey>();
                for (ECKey key : wallet.getKeys())
                    if (!wallet.isKeyRotating(key))
                        keys.add(key);

                Writer cipherOut = new OutputStreamWriter(new FileOutputStream(file), Constants.UTF_8);
                WalletUtils.writeEncryptedKeys(cipherOut, keys, application.getPrefs(), passOrNFC);
                cipherOut.close();

                if (justDecrypted) {
                    if (keyCache != null) {
                        wallet.encrypt(keyCache.getKeyCrypter(), keyCache.getAesKey());
                        updateEncryptedX2(application, keyCache);
                        application.setKeyCache(keyCache);
                    } else {
                        encryptWalletWithShamir(wallet, passOrNFC, application);
                    }
                }

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(application.getApplicationContext());
                SharedPreferences.Editor editor= prefs.edit();

                editor.putString(Constants.LAST_BACKUP_DATE, date).commit();
                editor.putInt(Constants.LAST_BACKUP_NUM_ADDRESSES, wallet.getKeychainSize()).commit();

                return file.getAbsolutePath();

            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }
        return null;
    }
}
