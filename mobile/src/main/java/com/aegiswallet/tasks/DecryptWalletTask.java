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

import com.aegiswallet.PayBitsApplication;
import com.aegiswallet.R;
import com.aegiswallet.listeners.WalletDecryptedListener;
import com.aegiswallet.listeners.WalletEncryptedListener;
import com.aegiswallet.widgets.AegisProgressDialog;
import com.google.bitcoin.core.Wallet;

/**
 * Created by bsimic on 3/19/14.
 */
public class DecryptWalletTask extends BaseTask {

    private String TAG = this.getClass().getName();

    private Context context;
    private Wallet wallet;
    private String passOrNFC;
    private AegisProgressDialog pd;
    private PayBitsApplication application;

    public DecryptWalletTask(Context context, Wallet wallet, String passOrNFC, PayBitsApplication application){
        this.context = context;
        this.wallet = wallet;
        this.passOrNFC = passOrNFC;
        this.application = application;

        pd = new AegisProgressDialog(context, 0, context.getString(R.string.decrypt_dialog_spinner_text));
    }

    @Override
    protected void onPreExecute() {
        pd.show();
    }

    @Override
    protected Object doInBackground(Object[] objects) {
        super.decryptWallet(application, wallet, passOrNFC);
        return null;
    }

    @Override
    protected void onPostExecute(Object o) {
        pd.dismiss();
        ((WalletDecryptedListener) context).onWalletDecrypted(passOrNFC);
    }
}
