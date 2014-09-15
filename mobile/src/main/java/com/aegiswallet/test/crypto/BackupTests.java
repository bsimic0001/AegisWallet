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

package com.aegiswallet.test.crypto;

import android.test.InstrumentationTestCase;
import android.util.Base64;
import android.util.Log;

import com.aegiswallet.utils.Constants;
import com.aegiswallet.utils.WalletUtils;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.DumpedPrivateKey;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.Wallet;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by bsimic on 5/6/14.
 */
public class BackupTests extends InstrumentationTestCase {

    private String TAG = this.getClass().getName();

    public void testWalletDecryption() {
        String fileName = "AegisWalletBackup-testnet-2014-05-06-23_24_55";
        String password = "000000";

        File file = new File(fileName);
        Wallet w = new Wallet(Constants.NETWORK_PARAMETERS);


        try {
            final List<ECKey> keys = new LinkedList<ECKey>();

            InputStream stream;
            stream = getInstrumentation().getTargetContext().getResources().getAssets().open(fileName);

            final BufferedReader in = new BufferedReader(new InputStreamReader(stream, Constants.UTF_8));

            String x1 = null;
            String x2Encrypted = null;
            String x2Decrypted = null;

            while (true) {
                final String line = in.readLine();
                if (line == null)
                    break; // eof

                if(line.startsWith("# "))
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
                    x2Encrypted = new String(Base64.decode(x2Base64.getBytes(), Base64.DEFAULT));
                    x2Decrypted = WalletUtils.decryptString(x2Encrypted, password);
                    x2Decrypted = x2Decrypted.split(":")[1];
                    continue;
                }

                BigInteger secret = WalletUtils.generateSecretFromStrings("1:" + x1, "2:" + x2Decrypted, null);
                String secretString = WalletUtils.convertToSha256(secret.toString());
                String encryptedKey = new String(Base64.decode(line.getBytes(), Base64.DEFAULT));
                String plainKey = WalletUtils.decryptString(encryptedKey, secretString);
                ECKey key = new DumpedPrivateKey(Constants.NETWORK_PARAMETERS, plainKey).getKey();

                if(!w.hasKey(key))
                    keys.add(key);
            }

            w.addKeys(keys);
            w.getBalance();

            assertNotNull(keys);
            assertNotNull(w);

        } catch (final AddressFormatException x) {
        } catch (IOException e) {

        }


    }
}
