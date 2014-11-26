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

import android.content.Context;
import android.util.Log;

import com.aegiswallet.PayBitsApplication;
import com.aegiswallet.R;
import com.aegiswallet.listeners.WalletEncryptedListener;
import com.aegiswallet.widgets.AegisProgressDialog;
import com.google.bitcoin.core.Wallet;

/**
 * Created by bsimic on 3/19/14.
 */
public class EncryptWalletTask extends BaseTask {

    private String TAG = this.getClass().getName();

    private Context context;
    private Wallet wallet;
    private String passOrX2;
    private AegisProgressDialog pd;
    private PayBitsApplication application;
    private boolean nfc;

    public EncryptWalletTask(Context context, Wallet wallet, String passOrX2, PayBitsApplication application, boolean nfc){
        this.context = context;
        this.wallet = wallet;
        this.passOrX2 = passOrX2;
        this.application = application;
        this.nfc = nfc;

        Log.d(TAG, "Encrypting wallet... inside constructor");
        pd = new AegisProgressDialog(context, 0, context.getString(R.string.encrypt_dialog_spinner_text));

    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        Log.d(TAG, "Inside on Pre execute for encrypt...");
        pd.show();
    }



    @Override
    protected Object doInBackground(Object[] objects) {
        Log.d(TAG, "Encrypting wallet with NFC Tag... inside doInBackground");

        if(nfc) {
            super.encryptWalletShamirNFC(wallet, passOrX2, application);
            Log.d(TAG, "Encrypting wallet with NFC Tag...");
        }
        else {
            Log.d(TAG, "Encrypting wallet with SSS...");
            System.out.println("Encrypting wallet with SSS...");
            super.encryptWalletWithShamir(wallet, passOrX2, application);
        }

        return null;
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        Log.d(TAG, "Encrypting wallet cancelled...");
    }

    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);

        pd.dismiss();
        ((WalletEncryptedListener) context).onWalletEncrypted();
    }
}
