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
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.aegiswallet.R;
import com.aegiswallet.objects.SMSTransactionPojo;
import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.NetworkParameters;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nonnull;

import edu.vt.middleware.dictionary.ArrayWordList;
import edu.vt.middleware.dictionary.WordListDictionary;
import edu.vt.middleware.dictionary.WordLists;
import edu.vt.middleware.dictionary.sort.ArraysSort;
import edu.vt.middleware.password.DictionarySubstringRule;
import edu.vt.middleware.password.Password;
import edu.vt.middleware.password.PasswordData;
import edu.vt.middleware.password.PasswordValidator;
import edu.vt.middleware.password.Rule;
import edu.vt.middleware.password.RuleResult;


public class BasicUtils {

    private static SecureRandom random = new SecureRandom();

    public static final String TAG = "BasicUtils";
    public static final BigInteger ONE_BTC = new BigInteger("100000000", 10);
    public static final BigInteger ONE_MBTC = new BigInteger("100000", 10);
    private static final int ONE_BTC_INT = ONE_BTC.intValue();
    private static final int ONE_MBTC_INT = ONE_MBTC.intValue();
    private final static QRCodeWriter qrCodeWriter = new QRCodeWriter();

    public static String satoshiToBTC(BigInteger value) {
        return formatValue(value, "", "-", Constants.BTC_MIN_PRECISION, 0);
    }

    public static String formatValue(@Nonnull final BigInteger value, final int precision, final int shift) {
        return formatValue(value, "", "-", precision, shift);
    }

    public static String formatValue(@Nonnull final BigInteger value, @Nonnull final String plusSign, @Nonnull final String minusSign,
                                     final int precision, final int shift) {
        long longValue = value.longValue();

        final String sign = longValue < 0 ? minusSign : plusSign;

        if (shift == 0) {
            if (precision == 2)
                longValue = longValue - longValue % 1000000 + longValue % 1000000 / 500000 * 1000000;
            else if (precision == 4)
                longValue = longValue - longValue % 10000 + longValue % 10000 / 5000 * 10000;
            else if (precision == 6)
                longValue = longValue - longValue % 100 + longValue % 100 / 50 * 100;
            else if (precision == 8)
                ;
            else
                throw new IllegalArgumentException("cannot handle precision/shift: " + precision + "/" + shift);

            final long absValue = Math.abs(longValue);
            final long coins = absValue / ONE_BTC_INT;
            final int satoshis = (int) (absValue % ONE_BTC_INT);

            if (satoshis % 1000000 == 0)
                return String.format(Locale.US, "%s%d.%02d", sign, coins, satoshis / 1000000);
            else if (satoshis % 10000 == 0)
                return String.format(Locale.US, "%s%d.%04d", sign, coins, satoshis / 10000);
            else if (satoshis % 100 == 0)
                return String.format(Locale.US, "%s%d.%06d", sign, coins, satoshis / 100);
            else
                return String.format(Locale.US, "%s%d.%08d", sign, coins, satoshis);
        } else if (shift == 3) {
            if (precision == 2)
                longValue = longValue - longValue % 1000 + longValue % 1000 / 500 * 1000;
            else if (precision == 4)
                longValue = longValue - longValue % 10 + longValue % 10 / 5 * 10;
            else if (precision == 5)
                ;
            else
                throw new IllegalArgumentException("cannot handle precision/shift: " + precision + "/" + shift);

            final long absValue = Math.abs(longValue);
            final long coins = absValue / ONE_MBTC_INT;
            final int satoshis = (int) (absValue % ONE_MBTC_INT);

            if (satoshis % 1000 == 0)
                return String.format(Locale.US, "%s%d.%02d", sign, coins, satoshis / 1000);
            else if (satoshis % 10 == 0)
                return String.format(Locale.US, "%s%d.%04d", sign, coins, satoshis / 10);
            else
                return String.format(Locale.US, "%s%d.%05d", sign, coins, satoshis);
        } else {
            throw new IllegalArgumentException("cannot handle shift: " + shift);
        }
    }

    public static BigInteger toNanoCoins(final String value, final int shift) {

        try {
            final BigInteger nanoCoins = new BigDecimal(value).movePointRight(8 - shift).toBigIntegerExact();

            if (nanoCoins.signum() < 0)
                throw new IllegalArgumentException("negative amount: " + value);
            if (nanoCoins.compareTo(NetworkParameters.MAX_MONEY) > 0)
                Log.e(TAG, "AMOUNT TOO LARGE");

            return nanoCoins;
        } catch (ArithmeticException e) {
            Log.d(TAG, e.getMessage());
            return BigInteger.ZERO;
        }
    }

    public static Bitmap encodeAsBitmap(String contents, BarcodeFormat format, int dimension) {

        Bitmap bitmap = null;

        try {
            final Hashtable<EncodeHintType, Object> hints = new Hashtable<EncodeHintType, Object>();
            //hints.put(EncodeHintType.MARGIN, 0);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            final BitMatrix result = qrCodeWriter.encode(contents, BarcodeFormat.QR_CODE, dimension, dimension, hints);

            final int width = result.getWidth();
            final int height = result.getHeight();
            final int[] pixels = new int[width * height];

            for (int y = 0; y < height; y++) {
                final int offset = y * width;
                for (int x = 0; x < width; x++) {
                    pixels[offset + x] = result.get(x, y) ? Color.BLACK : Color.WHITE;
                }
            }

            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            return bitmap;

        } catch (WriterException e) {
            Log.e("Basic Utils", "cannot write to bitmap " + e.getMessage());
        }
        return bitmap;
    }

    private static String guessAppropriateEncoding(CharSequence contents) {
        for (int i = 0; i < contents.length(); i++) {
            if (contents.charAt(i) > 0xFF) {
                return "UTF-8";
            }
        }
        return null;
    }

    public static JSONObject parseJSONData(Context context, String fileName) {
        String JSONString = null;
        JSONObject JSONObject = null;
        try {

            //open the inputStream to the file
            InputStream inputStream = context.openFileInput(fileName);

            int sizeOfJSONFile = inputStream.available();

            //array that will store all the data
            byte[] bytes = new byte[sizeOfJSONFile];

            //reading data into the array from the file
            inputStream.read(bytes);

            //close the input stream
            inputStream.close();

            JSONString = new String(bytes, "UTF-8");
            JSONObject = new JSONObject(JSONString);

        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        } catch (JSONException x) {
            return null;
        }
        return JSONObject;
    }

    public static String generateSecureKey() {
        return new BigInteger(512, random).toString(32);
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static Typeface getCustomTypeFace(Context context) {
        Typeface tf = Typeface.createFromAsset(
                context.getAssets(), "fonts/regular.otf");

        return tf;
    }

    public static String[] loadFileList() {

        String[] fileList = null;
        File filesPath = Constants.WALLET_BACKUP_DIRECTORY;

        if (Constants.WALLET_BACKUP_DIRECTORY.exists() && Constants.WALLET_BACKUP_DIRECTORY.isDirectory()) {
            final String filePrefix = "AegisWalletBackup";
            try {
                filesPath.mkdirs();
            } catch (SecurityException e) {
                Log.e(TAG, "Could not write to SD card - " + e.toString());
            }
            if (filesPath.exists()) {
                FilenameFilter filter = new FilenameFilter() {
                    public boolean accept(File dir, String filename) {
                        File sel = new File(dir, filename);
                        return filename.contains(filePrefix) || sel.isDirectory();
                    }
                };
                fileList = filesPath.list(filter);
            } else {
                fileList = new String[0];
            }
        }

        return fileList;
    }

    public static String getNFCErrorMessage(Context context, String writeMessage) {
        String result = null;

        Resources resources = context.getResources();

        if (writeMessage.equals(Constants.NFC_TAG_TOO_SMALL)) {
            result = resources.getString(R.string.nfc_error_tag_too_small);
        } else if (writeMessage.equals(Constants.NFC_READ_ONLY)) {
            result = resources.getString(R.string.nfc_error_tag_read_only);
        } else if (writeMessage.equals(Constants.NFC_CANT_CONNECT)) {
            result = resources.getString(R.string.nfc_error_tag_cant_connect);
        } else if (writeMessage.equals(Constants.NFC_CANT_FORMAT)) {
            result = resources.getString(R.string.nfc_error_tag_cant_format);
        } else if (writeMessage.equals(Constants.NFC_TAG_NOT_SUPPORTED)) {
            result = resources.getString(R.string.nfc_error_tag_not_supported);
        } else if (writeMessage.equals(Constants.NFC_WRITE_EXCEPTION)) {
            result = resources.getString(R.string.nfc_error_tag_write_exception);
        }

        return result;
    }

    public static boolean isPasswordInDictionary(Context context, String password) {
        boolean resultBool = false;

        if(password == null)
            return false;

        try {


            AssetFileDescriptor descriptor = context.getAssets().openFd("commonpasswords.xmf");
            FileReader reader = new FileReader(descriptor.getFileDescriptor());

            // create a case sensitive word list and sort it
            ArrayWordList awl = WordLists.createFromReader(
                    new FileReader[]{reader},
                    true,
                    new ArraysSort());

            WordListDictionary dict = new WordListDictionary(awl);

            DictionarySubstringRule dictRule = new DictionarySubstringRule(dict);
            dictRule.setWordLength(6); // size of words to check in the password
            dictRule.setMatchBackwards(true); // match dictionary words backwards

            List<Rule> ruleList = new ArrayList<Rule>();
            ruleList.add(dictRule);

            PasswordValidator validator = new PasswordValidator(ruleList);
            PasswordData passwordData = new PasswordData(new Password(password.toLowerCase()));

            RuleResult result = validator.validate(passwordData);
            if (result.isValid()) {
                Log.d(TAG, "Valid password");
                resultBool = true;
            } else {
                Log.d(TAG, "Invalid password");
            }
        } catch (FileNotFoundException e) {
            Log.d(TAG, e.getMessage());
        }
        catch (IOException e){
            Log.d(TAG, e.getMessage());
        }

        return resultBool;
    }

    public static String getAddressFromMessage(String message){
        String address = null;

        String[] splitString = message.split(" ");
        for(String s : splitString){
            try{
                Address a = new Address(Constants.NETWORK_PARAMETERS, s);
                if(a != null){
                    return a.toString();
                }
            }
            catch (AddressFormatException e){
                continue;
            }

        }

        return address;
    }

    public static String findKeyInPrefs(SharedPreferences prefs, String value) {
        for (Map.Entry<String, ?> entry: prefs.getAll().entrySet()) {
            if (value.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null; // not found
    }

    public static ArrayList<SMSTransactionPojo> getAllPendingTransactions(SharedPreferences prefs){
        ArrayList<SMSTransactionPojo> list = new ArrayList<SMSTransactionPojo>();

        for (Map.Entry<String, ?> entry: prefs.getAll().entrySet()) {
            SMSTransactionPojo pojo = new SMSTransactionPojo(entry.getValue() + "");
            list.add(pojo);
        }
        return list;
    }

    public static ArrayList<SMSTransactionPojo> getAllRespondedSMSTransactions(SharedPreferences prefs){
        ArrayList<SMSTransactionPojo> list = new ArrayList<SMSTransactionPojo>();

        for (Map.Entry<String, ?> entry: prefs.getAll().entrySet()) {
            SMSTransactionPojo pojo = new SMSTransactionPojo(entry.getValue() + "");
            if(pojo.getStatus() == Constants.SMS_STATUS_REC){
                list.add(pojo);
            }
        }
        return list;
    }

    public static ArrayList<SMSTransactionPojo> getAllNotRespondedSMSTransactions(SharedPreferences prefs){
        ArrayList<SMSTransactionPojo> list = new ArrayList<SMSTransactionPojo>();

        for (Map.Entry<String, ?> entry: prefs.getAll().entrySet()) {
            SMSTransactionPojo pojo = new SMSTransactionPojo(entry.getValue() + "");
            if(pojo.getStatus() == Constants.SMS_STATUS_INIT){
                list.add(pojo);
            }
        }
        return list;
    }
}

