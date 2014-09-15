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

package com.aegiswallet.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.format.DateUtils;
import android.util.Base64;
import android.util.Log;

import com.aegiswallet.helpers.Crypto;
import com.aegiswallet.helpers.secretshare.SecretShare;
import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.DumpedPrivateKey;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.ScriptException;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.Wallet;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.crypto.SecretKey;

/**
 * Created by bsimic on 3/10/14.
 */
public class WalletUtils {

    private static final String TAG = WalletUtils.TAG;

    private static int passwordIterations = 10000;
    private static int keySize = 256;

    public static final BigInteger ONE_BTC = new BigInteger("100000000", 10);
    public static final BigInteger ONE_MBTC = new BigInteger("100000", 10);

    public static void writeEncryptedKeys(@Nonnull final Writer out, @Nonnull final List<ECKey> keys, SharedPreferences prefs, String passOrNFC) throws IOException {

        boolean nfcEnabled = prefs.contains(Constants.SHAMIR_ENCRYPTED_KEY) ? false : true;

        String x1 = prefs.getString(Constants.SHAMIR_LOCAL_KEY, null);
        String x2 = null;
        String encodedEncryptedX2 = null;

        if (!nfcEnabled) {
            x2 = prefs.getString(Constants.SHAMIR_ENCRYPTED_KEY, null);
            String encryptedX2 = encryptString(x2, passOrNFC);
            encodedEncryptedX2 = Base64.encodeToString(encryptedX2.getBytes("UTF-8"), Base64.NO_WRAP);
        }

        out.write("# PRIVATE KEYS ARE ENCRYPTED WITH SHAMIR SECRET SHARING\n");
        out.write("# TO DECRYPT - Import this backup and provide your password or NFC token\n");
        out.write("# If password/NFC token are lost, contact Bitcoin Security Project. We may be able to help.\n");
        out.write("#" + x1);
        out.write("\n");

        if (!nfcEnabled && encodedEncryptedX2 != null) {
            out.write("#X2:" + encodedEncryptedX2);
            out.write("\n");
            out.write("#ENCTYPE:PASSWORD");
        }
        //Means NFC is enabled and we're using that for encryption
        else if (nfcEnabled) {
            out.write("#ENCTYPE:NFC");
        }

        out.write("\n");

        BigInteger mainKey = null;
        if (nfcEnabled) {
            mainKey = generateSecretFromStrings(x1, passOrNFC, null);
        } else if (x2 != null) {
            mainKey = generateSecretFromStrings(x1, x2, null);
        }

        String mainKeyHash = convertToSha256(mainKey.toString());

        for (final ECKey key : keys) {
            String encodedKey = key.getPrivateKeyEncoded(Constants.NETWORK_PARAMETERS).toString();
            String encryptedKey = encryptString(encodedKey, mainKeyHash);

            out.write(Base64.encodeToString(encryptedKey.getBytes(), Base64.NO_WRAP));
            out.write('\n');
        }
    }

    public static boolean checkFileBackupNFCEncrypted(String fileName) {
        boolean result = false;

        try {
            if (Constants.WALLET_BACKUP_DIRECTORY.exists() && Constants.WALLET_BACKUP_DIRECTORY.isDirectory()) {

                File file = new File(Constants.WALLET_BACKUP_DIRECTORY, fileName);

                FileInputStream fileInputStream = new FileInputStream(file);
                final BufferedReader in = new BufferedReader(new InputStreamReader(fileInputStream, Constants.UTF_8));

                while (true) {
                    final String line = in.readLine();
                    if (line == null)
                        break; // eof

                    if (line.startsWith("#ENCTYPE:NFC")) {
                        return true;
                    }
                }
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, "IO Exception: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "some other exception: " + e.getMessage());
        }

        return result;
    }

    /**
     * This file will check the password provided when importing a backup file. If the password is correct,
     * it will return true, if not, false.
     *
     * @param fileName
     * @param password
     * @return
     */
    public static boolean checkPasswordForBackupFile(String fileName, String password) {
        boolean result = false;

        try {
            if (Constants.WALLET_BACKUP_DIRECTORY.exists() && Constants.WALLET_BACKUP_DIRECTORY.isDirectory()) {

                File file = new File(Constants.WALLET_BACKUP_DIRECTORY, fileName);

                FileInputStream fileInputStream = new FileInputStream(file);
                final BufferedReader in = new BufferedReader(new InputStreamReader(fileInputStream, Constants.UTF_8));

                while (true) {
                    final String line = in.readLine();
                    if (line == null)
                        break; // eof

                    if (line.startsWith("#X2:")) {

                        String[] splitStr = line.split(":");
                        String x2Base64 = splitStr[1];

                        String x2Encrypted = new String(Base64.decode(x2Base64.getBytes(), Base64.NO_WRAP));
                        String x2Decrypted = WalletUtils.decryptString(x2Encrypted, password);
                        x2Decrypted = x2Decrypted.split(":")[1];

                        if (x2Decrypted != null) {
                            return true;
                        }
                    }
                }
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, "IO Exception: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "some other exception: " + e.getMessage());
        }

        return result;
    }

    public static List<ECKey> restoreWalletFromBackupFile(String fileName, String passOrNFC, Wallet wallet, boolean shouldAddKeys) {
        final List<ECKey> keys = new LinkedList<ECKey>();
        boolean nfcEncrypted;

        try {

            if (Constants.WALLET_BACKUP_DIRECTORY.exists() && Constants.WALLET_BACKUP_DIRECTORY.isDirectory()) {

                File file = new File(Constants.WALLET_BACKUP_DIRECTORY, fileName);

                FileInputStream fileInputStream = new FileInputStream(file);
                final BufferedReader in = new BufferedReader(new InputStreamReader(fileInputStream, Constants.UTF_8));

                String x1 = null;
                String x2Encrypted = null;
                String x2Decrypted = null;
                String secretString = null;

                while (true) {
                    final String line = in.readLine();
                    if (line == null)
                        break; // eof

                    if (line.startsWith("# "))
                        continue;

                    if (line.trim().isEmpty())
                        continue;

                    if (line.startsWith("#1:")) {
                        String[] splitStr = line.split(":");
                        x1 = splitStr[1];
                        continue;
                    }

                    if (line.startsWith("#X2:")) {

                        String[] splitStr = line.split(":");
                        String x2Base64 = splitStr[1];

                        x2Encrypted = new String(Base64.decode(x2Base64.getBytes(), Base64.NO_WRAP));
                        x2Decrypted = WalletUtils.decryptString(x2Encrypted, passOrNFC);
                        x2Decrypted = x2Decrypted.split(":")[1];
                        BigInteger secret = WalletUtils.generateSecretFromStrings("1:" + x1, "2:" + x2Decrypted, null);
                        secretString = WalletUtils.convertToSha256(secret.toString());

                        continue;
                    }

                    if (line.startsWith("#ENCTYPE:NFC")) {
                        x2Decrypted = passOrNFC;
                        BigInteger secret = WalletUtils.generateSecretFromStrings("1:" + x1, x2Decrypted, null);
                        secretString = WalletUtils.convertToSha256(secret.toString());
                        continue;
                    }

                    if (line.startsWith("#ENCTYPE:PASSWORD"))
                        continue;

                    String encryptedKey = new String(Base64.decode(line.getBytes(), Base64.NO_WRAP));
                    String plainKey = WalletUtils.decryptString(encryptedKey, secretString);
                    ECKey key = new DumpedPrivateKey(Constants.NETWORK_PARAMETERS, plainKey).getKey();

                    if (!wallet.hasKey(key))
                        keys.add(key);
                }

                //Only add keys if the parameter says so. This is because the wallet may be still encrypted.
                //We dont want to add keys to an encrypted wallet. That's bad.
                if (shouldAddKeys)
                    wallet.addKeys(keys);

            }


        } catch (final AddressFormatException x) {
            Log.e(TAG, "exception caught: " + x.getMessage());
        } catch (IOException e) {
            Log.e(TAG, "exception caught: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "exception caught: " + e.getMessage());
        }

        return keys;

    }

    public static List<ECKey> readKeys(@Nonnull final BufferedReader in) throws IOException {
        try {
            final DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            final List<ECKey> keys = new LinkedList<ECKey>();

            while (true) {
                final String line = in.readLine();
                if (line == null)
                    break; // eof
                if (line.trim().isEmpty() || line.charAt(0) == '#')
                    continue; // skip comment

                final String[] parts = line.split(" ");

                final ECKey key = new DumpedPrivateKey(Constants.NETWORK_PARAMETERS, parts[0]).getKey();
                key.setCreationTimeSeconds(parts.length >= 2 ? format.parse(parts[1]).getTime() / DateUtils.SECOND_IN_MILLIS : 0);

                keys.add(key);
            }

            return keys;
        } catch (final AddressFormatException x) {
            throw new IOException("cannot read keys", x);
        } catch (final ParseException x) {
            throw new IOException("cannot read keys", x);
        }
    }

    public static Address getFirstFromAddress(@Nonnull final Transaction transaction) {
        if (transaction.isCoinBase())
            return null;

        try {
            for (final TransactionInput input : transaction.getInputs()) {
                return input.getFromAddress();
            }

            throw new IllegalStateException();
        } catch (final ScriptException x) {
            return null;
        }
    }

    @CheckForNull
    public static Address getFirstToAddress(@Nonnull final Transaction transaction) {
        try {
            for (final TransactionOutput output : transaction.getOutputs()) {
                return output.getScriptPubKey().getToAddress(Constants.NETWORK_PARAMETERS);
            }

            throw new IllegalStateException();
        } catch (final ScriptException x) {
            return null;
        }
    }

    public static String getBTCCurrencryValue(Context context, SharedPreferences prefs, BigDecimal amount) {
        String result = "";

        File file = context.getApplicationContext().getFileStreamPath(Constants.BLOCKCHAIN_CURRENCY_FILE_NAME);
        if (file.exists()) {
            JSONObject jsonObject = BasicUtils.parseJSONData(context, Constants.BLOCKCHAIN_CURRENCY_FILE_NAME);
            try {

                if (jsonObject != null) {

                    JSONObject newObject = jsonObject.getJSONObject(prefs.getString(Constants.CURRENCY_PREF_KEY, null));
                    Double doubleVal = newObject.getDouble("last");
                    BigDecimal decimal = BigDecimal.valueOf(doubleVal);

                    result = newObject.getString("symbol") + decimal.multiply(amount).setScale(2, RoundingMode.HALF_EVEN).toString();
                }
            } catch (JSONException e) {
                Log.e("Wallet Utils", "JSON Exception " + e.getMessage());
            }
        }


        return result;
    }


    public static String getWalletCurrencyValue(Context context, SharedPreferences prefs, BigInteger balance) {
        String result = "";

        File file = context.getApplicationContext().getFileStreamPath(Constants.BLOCKCHAIN_CURRENCY_FILE_NAME);
        if (file.exists()) {
            JSONObject jsonObject = BasicUtils.parseJSONData(context, Constants.BLOCKCHAIN_CURRENCY_FILE_NAME);
            try {

                String balanceInBTC = balance.toString();

                if (balance.longValue() > 0)
                    balanceInBTC = BasicUtils.formatValue(balance, Constants.BTC_MAX_PRECISION, 0);
                BigDecimal formattedBalance = new BigDecimal(balanceInBTC);

                if (jsonObject != null) {

                    JSONObject newObject = jsonObject.getJSONObject(prefs.getString(Constants.CURRENCY_PREF_KEY, null));
                    Double doubleVal = newObject.getDouble("last");
                    BigDecimal decimal = BigDecimal.valueOf(doubleVal);

                    result = newObject.getString("symbol") + decimal.multiply(formattedBalance).setScale(2, RoundingMode.HALF_EVEN).toString();
                }
            } catch (JSONException e) {
                Log.e("Wallet Utils", "JSON Exception " + e.getMessage());
            }
        }


        return result;
    }

    public static BigDecimal getExchangeRate(Context context, SharedPreferences prefs) {
        File file = context.getApplicationContext().getFileStreamPath(Constants.BLOCKCHAIN_CURRENCY_FILE_NAME);
        if (file.exists()) {
            JSONObject jsonObject = BasicUtils.parseJSONData(context, Constants.BLOCKCHAIN_CURRENCY_FILE_NAME);
            try {
                if (jsonObject != null) {

                    JSONObject newObject = jsonObject.getJSONObject(prefs.getString(Constants.CURRENCY_PREF_KEY, null));
                    double doubleValue = newObject.getDouble("last");

                    BigDecimal bigDecimal = BigDecimal.valueOf(doubleValue);
                    return bigDecimal;
                }
            } catch (JSONException e) {
                Log.e("Wallet Utils", "JSON Exception " + e.getMessage());
            }
        }

        return null;
    }

    public static BigInteger btcValue(Context context, SharedPreferences prefs, @Nonnull final BigInteger localValue) {
        BigInteger result = null;

        BigDecimal exchangeRate = getExchangeRate(context, prefs);
        if (exchangeRate != null)
            result = localValue.multiply(ONE_BTC).divide(exchangeRate.toBigInteger());

        return result;
    }

    public static String encryptString(String plainText, String password) {
        byte[] salt = Crypto.generateSalt();
        SecretKey key = Crypto.deriveKeyPbkdf2(salt, password);
        return Crypto.encrypt(plainText, key, salt);
    }

    public static String decryptString(String ciphertext, String password) {
        return Crypto.decryptPbkdf2(ciphertext, password);
    }

    private static String getRawKey(SecretKey key) {
        if (key == null) {
            return null;
        }

        return Crypto.toHex(key.getEncoded());
    }

    public static String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte bytes[] = new byte[keySize / 8];
        random.nextBytes(bytes);
        String s = new String(bytes);
        return s;
    }

    public static String convertToSha256(String plainText) {

        String result = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(plainText.getBytes());

            byte byteData[] = md.digest();

            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < byteData.length; i++) {
                sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
            }

            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < byteData.length; i++) {
                String hex = Integer.toHexString(0xff & byteData[i]);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            result = hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, e.getMessage());
        } finally {
            return result;
        }
    }

    public static BigInteger generateSecretFromStrings(String x1String, String x2String, String x3String) {

        List<SecretShare.ShareInfo> newShares = new ArrayList<SecretShare.ShareInfo>();

        SecretShare.PublicInfo publicInfo1 = new SecretShare.PublicInfo(3, 2, null, null);
        SecretShare.PublicInfo publicInfo2 = new SecretShare.PublicInfo(3, 2, null, null);
        SecretShare.PublicInfo publicInfo3 = new SecretShare.PublicInfo(3, 2, null, null);

        SecretShare.ShareInfo shareInfoOne;
        SecretShare.ShareInfo shareInfoTwo;
        SecretShare.ShareInfo shareInfoThree;

        if (x1String != null) {
            BigInteger one = new BigInteger(getShareOrX(x1String, true));
            int x1 = new Integer(getShareOrX(x1String, false)).intValue();
            shareInfoOne = new SecretShare.ShareInfo(x1, one, publicInfo1);
            newShares.add(shareInfoOne);
        }

        if (x2String != null) {
            BigInteger two = new BigInteger(getShareOrX(x2String, true));
            int x2 = new Integer(getShareOrX(x2String, false)).intValue();
            shareInfoTwo = new SecretShare.ShareInfo(x2, two, publicInfo2);
            newShares.add(shareInfoTwo);
        }

        if (x3String != null) {
            BigInteger three = new BigInteger(getShareOrX(x3String, true));
            int x3 = new Integer(getShareOrX(x3String, false)).intValue();
            shareInfoThree = new SecretShare.ShareInfo(x3, three, publicInfo3);
            newShares.add(shareInfoThree);
        }

        if (newShares.size() >= 2) {
            SecretShare.PublicInfo publicInfo = newShares.get(0).getPublicInfo();
            SecretShare solver = new SecretShare(publicInfo);
            SecretShare.CombineOutput solved = solver.combine(newShares);
            return solved.getSecret();
        }

        return null;
    }

    public static String getShareOrX(String x, boolean getShare) {
        if (x != null) {
            if (!getShare && x.contains(":"))
                return x.split(":")[0];
            else if (x.contains(":"))
                return x.split(":")[1];
        }

        return null;
    }

    public static boolean checkPassword(String password, SharedPreferences prefs) {
        boolean result = false;

        String passwordSalt = prefs.getString(Constants.PASSWORD_SALT, "");
        String passwordHash = passwordSalt + convertToSha256(passwordSalt + password);

        String storedHash = prefs.getString(Constants.PASSWORD_HASH, "");

        if (passwordHash.equals(storedHash))
            result = true;

        return result;
    }

    public static void changePassword(String password, SharedPreferences prefs) {

        String passwordSalt = BasicUtils.generateSecureKey();
        String passwordHash = passwordSalt + WalletUtils.convertToSha256(passwordSalt + password);

        prefs.edit().putString(Constants.PASSWORD_HASH, passwordHash).commit();
        prefs.edit().putString(Constants.PASSWORD_SALT, passwordSalt).commit();
        prefs.edit().putBoolean(Constants.APP_INIT_COMPLETE, true).commit();
    }

    public static boolean isTransactionRelevant(Transaction tx, Wallet wallet) throws ScriptException {

        return tx.getValueSentFromMe(wallet).compareTo(BigInteger.ZERO) > 0 ||
                tx.getValueSentToMe(wallet).compareTo(BigInteger.ZERO) > 0 || tx.isPending();

    }

    public static ArrayList<Transaction> getRelevantTransactions(ArrayList<Transaction> currentTxs, Wallet wallet){
        ArrayList<Transaction> newList = new ArrayList<Transaction>();

        if(currentTxs != null){
            for(Transaction transaction : currentTxs){
                if(WalletUtils.isTransactionRelevant(transaction, wallet)){
                    newList.add(transaction);
                }
            }
        }

        return newList;
    }

    public static boolean isAddressMine(Wallet w, Address a){
        List<ECKey> keys = w.getKeys();

        for (ECKey key : keys) {
            Address address = key.toAddress(Constants.NETWORK_PARAMETERS);
            if(a.toString().equals(address.toString())){
                return true;
            }
        }

        return false;
    }
}
