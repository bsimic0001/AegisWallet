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
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.aegiswallet.PayBitsApplication;
import com.aegiswallet.R;
import com.aegiswallet.adapters.AddressListAdapter;
import com.aegiswallet.objects.KeyCache;
import com.aegiswallet.utils.Constants;
import com.aegiswallet.utils.WalletUtils;
import com.aegiswallet.widgets.AegisProgressDialog;
import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.crypto.KeyCrypter;
import com.google.bitcoin.crypto.KeyCrypterException;
import com.google.bitcoin.crypto.KeyCrypterScrypt;

import org.spongycastle.crypto.params.KeyParameter;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by bsimic on 3/14/14.
 */
public class AddWalletAddressTask extends BaseTask {

    private String TAG = this.getClass().getName();

    Wallet wallet;
    String passOrNfc;
    PayBitsApplication application;
    AddressListAdapter addressListAdapter;
    private SharedPreferences prefs;
    private AegisProgressDialog pd;
    private KeyCache keyCache;
    private boolean justDecrypted = false;

    public AddWalletAddressTask(Context context,
                                Wallet wallet,
                                String passOrNfc,
                                PayBitsApplication application,
                                AddressListAdapter addressListAdapter,
                                KeyCache keyCache) {

        this.wallet = wallet;
        this.passOrNfc = passOrNfc;
        this.application = application;
        this.addressListAdapter = addressListAdapter;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(application.getBaseContext());
        this.keyCache = keyCache;

        pd = new AegisProgressDialog(context, 0, context.getString(R.string.adding_address_spinner_text));
    }

    @Override
    protected void onPreExecute() {
        pd.show();
    }

    @Override
    protected Object doInBackground(Object[] objects) {

        if(wallet.isEncrypted()){
            super.decryptWallet(application, wallet, passOrNfc);
            justDecrypted = true;
        }
        return null;
    }



    @Override
    protected void onPostExecute(Object o) {
        ECKey newKey = new ECKey();

        if (!wallet.isEncrypted()) {
            wallet.addKey(newKey);

            if (justDecrypted) {
                if (keyCache != null) {
                    wallet.encrypt(keyCache.getKeyCrypter(), keyCache.getAesKey());
                    super.updateEncryptedX2(application, keyCache);
                    application.setKeyCache(keyCache);
                } else {
                    super.encryptWalletWithShamir(wallet, passOrNfc, application);
                }
            }

            pd.dismiss();
            Address address = newKey.toAddress(Constants.NETWORK_PARAMETERS);
            //Make new address the default
            prefs.edit().putString(Constants.PREFS_KEY_SELECTED_ADDRESS, address.toString()).commit();
            addressListAdapter.updateView();
        }
        else{
            pd.dismiss();
        }
    }

}
