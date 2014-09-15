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

import com.aegiswallet.objects.SMSTransactionPojo;
import com.aegiswallet.utils.BasicUtils;
import com.aegiswallet.utils.Constants;
import com.aegiswallet.utils.WalletUtils;

import java.math.BigInteger;

/**
 * Created by bsimic on 5/5/14.
 */
public class WalletUtilsTest extends InstrumentationTestCase {

    private String TAG = this.getClass().getName();

    public void testStringEncryption(){
        String plainText = "encrypt this string";
        String password = "password";
        String cipherText = WalletUtils.encryptString(plainText, password);
        assertNotNull(cipherText);
    }

    public void testDecryptString(){
        String result = WalletUtils.decryptString("dwnaWJiRma6c+ZD01IcvW4GyN2j9Ot9Jsl3Ub2IMAZg=]xIYJI2lNfW48NMVVSqivoQ==]dUtFYh1qt0w+p/jtKcGflrJH5000VePbS3W49K54kmc=", "password");

        assertEquals("encrypt this string", result);
    }

    public void testSatoshiConversion(){
        BigInteger value = BasicUtils.toNanoCoins(".0001", 0);

        Log.d(TAG, "satoshi amount: " + value.toString());

        assertNotNull(value);

    }

    public void testPasswordDictionary(){
        boolean result = BasicUtils.isPasswordInDictionary(getInstrumentation().getContext(), "Password1");
        assertFalse(result);
    }

    public void testSMSPojoConversion(){
        SMSTransactionPojo smsTransactionPojo = new SMSTransactionPojo("6146253134", "Nakeishia Simic", BigInteger.valueOf(10000), "", Constants.SMS_STATUS_INIT, "");
        String jsonBase64 = smsTransactionPojo.getJSONBase64();
        SMSTransactionPojo pojo = new SMSTransactionPojo(jsonBase64);

        Log.d(TAG, "POJO amount: " + pojo.getAmount());
        Log.d(TAG, "POJO name: " + pojo.getName());


        assertNotNull(pojo.getAmount());
    }
}
