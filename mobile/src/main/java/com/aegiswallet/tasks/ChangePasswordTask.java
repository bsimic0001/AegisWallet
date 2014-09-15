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
import com.aegiswallet.actions.MainActivity;
import com.aegiswallet.listeners.WalletDecryptedListener;
import com.aegiswallet.listeners.WalletEncryptedListener;
import com.aegiswallet.utils.WalletUtils;
import com.aegiswallet.widgets.AegisProgressDialog;
import com.google.bitcoin.core.Wallet;

/**
 * Created by bsimic on 3/19/14.
 */
public class ChangePasswordTask extends BaseTask {

    private String TAG = this.getClass().getName();

    private Context context;
    private Wallet wallet;
    private AegisProgressDialog pd;
    private PayBitsApplication application;

    private String oldPassword;
    private String newPassword;

    public ChangePasswordTask(Context context, Wallet wallet, PayBitsApplication application, String oldPassword, String newPassword){
        this.context = context;
        this.wallet = wallet;
        this.application = application;

        this.oldPassword = oldPassword;
        this.newPassword = newPassword;

        pd = new AegisProgressDialog(context, 0, context.getString(R.string.settings_change_password_dialog_text));
    }

    @Override
    protected void onPreExecute() {
        pd.show();
    }

    @Override
    protected Object doInBackground(Object[] objects) {
        super.decryptWallet(application, wallet, oldPassword);
        return null;
    }

    @Override
    protected void onPostExecute(Object o) {
        WalletUtils.changePassword(newPassword, application.getPrefs());
        //super.encryptWalletWithShamir(application.getWallet(), newPassword, application);

        pd.dismiss();
        ((WalletDecryptedListener) context).onWalletDecrypted(newPassword);
    }
}
