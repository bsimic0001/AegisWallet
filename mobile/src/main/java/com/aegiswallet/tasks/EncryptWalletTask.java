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

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.aegiswallet.PayBitsApplication;
import com.aegiswallet.R;
import com.aegiswallet.listeners.WalletEncryptedListener;
import com.aegiswallet.objects.KeyCache;
import com.aegiswallet.utils.Constants;
import com.aegiswallet.utils.WalletUtils;
import com.aegiswallet.widgets.AegisProgressDialog;
import com.aegiswallet.widgets.HoloCircularProgressBar;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.crypto.KeyCrypter;
import com.google.bitcoin.crypto.KeyCrypterScrypt;

import org.spongycastle.crypto.params.KeyParameter;

import java.math.BigInteger;
import java.util.Objects;

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

        pd = new AegisProgressDialog(context, 0, context.getString(R.string.encrypt_dialog_spinner_text));

    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        pd.show();
    }

    @Override
    protected Object doInBackground(Object[] objects) {
        if(nfc)
            super.encryptWalletShamirNFC(wallet, passOrX2, application);
        else
            super.encryptWalletWithShamir(wallet, passOrX2, application);

        return null;
    }

    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);

        pd.dismiss();
        ((WalletEncryptedListener) context).onWalletEncrypted();
    }
}
