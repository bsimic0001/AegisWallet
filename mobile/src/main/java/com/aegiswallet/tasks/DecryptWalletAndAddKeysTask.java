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
import android.content.Intent;

import com.aegiswallet.PayBitsApplication;
import com.aegiswallet.R;
import com.aegiswallet.listeners.ImportCompletedListener;
import com.aegiswallet.listeners.WalletEncryptedListener;
import com.aegiswallet.services.PeerBlockchainService;
import com.aegiswallet.utils.WalletUtils;
import com.aegiswallet.widgets.AegisProgressDialog;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.Wallet;

import java.util.List;

/**
 * Created by bsimic on 5/6/14.
 */
public class DecryptWalletAndAddKeysTask extends BaseTask{

    private AegisProgressDialog dialog;
    private PayBitsApplication application;
    private Wallet wallet;
    private Context context;
    private String password;
    private List<ECKey> keyList;

    public DecryptWalletAndAddKeysTask(PayBitsApplication application, Wallet wallet, Context context, String password, List<ECKey> keyList){
        this.application = application;
        this.wallet = wallet;
        this.context = context;
        this.password = password;
        this.keyList = keyList;

        dialog = new AegisProgressDialog(context, 0, context.getString(R.string.adding_keys_to_wallet_spinner_text));
    }

    @Override
    protected void onPreExecute() {
        dialog.show();
    }

    @Override
    protected Object doInBackground(Object[] objects) {
        if(wallet.isEncrypted())
            decryptWallet(application, wallet, password);
        return null;
    }

    @Override
    protected void onPostExecute(Object o) {
        dialog.dismiss();
        if(keyList != null && keyList.size() > 0){
            wallet.addKeys(keyList);
        }
        ((ImportCompletedListener) context).onImportCompleted(null, keyList);

    }
}
