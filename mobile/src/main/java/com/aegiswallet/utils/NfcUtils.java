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

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Parcelable;

import java.io.IOException;
import java.nio.charset.Charset;

public class NfcUtils {
    private static final String LOG_TAG = NfcUtils.class.getName();

    //checks if there is nfc adapter
    public static boolean nfcIsntAvailable(Activity a) {
        return NfcAdapter.getDefaultAdapter(a) == null;
    }

    // checks if nfc is on
    public static boolean nfcIsOff(Activity a) {
        NfcAdapter n = NfcAdapter.getDefaultAdapter(a);
        return n != null && !n.isEnabled();
    }

    //need to be called inside onResume. like that "listen(this,getClass());"
    public static void listen(Activity activity, Class c) {
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(activity);
        if (nfcAdapter != null && nfcAdapter.isEnabled()) {
            PendingIntent pi = PendingIntent.getActivity(activity, 0,
                    new Intent(activity, c)
                            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0
            );
            IntentFilter tagDetected = new IntentFilter(
                    NfcAdapter.ACTION_TAG_DISCOVERED);
            IntentFilter[] filters = new IntentFilter[]{tagDetected};
            nfcAdapter.enableForegroundDispatch(activity, pi, filters, null);
        }
    }

    //to get data from nfc
    //you can use it by overriding the function "onNewIntent" and then with the intent recieved call getData
    public static byte[] getData(Intent intent) {
        Parcelable raw[] = intent.getParcelableArrayExtra(
                NfcAdapter.EXTRA_NDEF_MESSAGES);
        if (raw == null)
            return null;
        NdefMessage msg = (NdefMessage) raw[0];
        NdefRecord pvk = msg.getRecords()[0];

        return pvk.getPayload();
    }

    // to format and write on it
    //you can use it by overriding the function "onNewIntent" and
    //then with the intent recieved call write with the data to write
    public static String write(Intent i, byte[] data) {
        Tag tag = i.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag == null)
            return "NULL";
        //return -1;

        NdefRecord appRecord = NdefRecord
                .createApplicationRecord(Constants.NFC_VALUE);
        byte[] mimeBytes = ("application/" + Constants.NFC_VALUE)
                .getBytes(Charset.forName("US-ASCII"));

        NdefRecord cardRecord = new NdefRecord(NdefRecord.TNF_MIME_MEDIA,
                mimeBytes, new byte[0], data);
        NdefMessage message = new NdefMessage(new NdefRecord[]{cardRecord,
                appRecord});
        Ndef ndef = Ndef.get(tag);
        if (ndef == null) {
            String a = format(tag);
            if (!a.equals(Constants.NFC_TAG_FORMATTED))
                return a;
        }
        ndef = Ndef.get(tag);
        if (ndef == null)
            return Constants.NFC_CANT_CONNECT;
        try {
            ndef.connect();
            if (!ndef.isWritable()) {
                ndef.close();
                return Constants.NFC_READ_ONLY;
            }
            int size = message.toByteArray().length;
            if (ndef.getMaxSize() < size) {
                ndef.close();
                return Constants.NFC_TAG_TOO_SMALL;
            }
            try {
                ndef.writeNdefMessage(message);
            } catch (FormatException e) {
                e.printStackTrace();
                return Constants.NFC_CANT_FORMAT;
            }
            return Constants.NFC_TAG_WRITTEN;
        } catch (IOException e) {
            e.printStackTrace();
            return Constants.NFC_WRITE_EXCEPTION;
        }
    }

    private static String format(Tag tag) {
        NdefFormatable format = NdefFormatable.get(tag);
        if (format == null)
            return Constants.NFC_TAG_NOT_SUPPORTED;
        try {
            format.connect();
            format.format(null);
            return Constants.NFC_TAG_FORMATTED;
        } catch (Exception ignore) {
            ignore.printStackTrace();
        }
        return Constants.NFC_CANT_FORMAT;
    }


}
