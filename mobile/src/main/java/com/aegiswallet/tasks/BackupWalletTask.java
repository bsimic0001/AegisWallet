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
import com.aegiswallet.listeners.BackupCompletedListener;
import com.aegiswallet.objects.KeyCache;
import com.aegiswallet.widgets.AegisProgressDialog;
import com.google.bitcoin.core.Wallet;

/**
 * Created by bsimic on 5/5/14.
 */
public class BackupWalletTask extends BaseTask {

    private String TAG = this.getClass().getName();

    private PayBitsApplication application;
    private Context context;
    private Wallet wallet;
    private KeyCache keyCache;
    private AegisProgressDialog dialog;
    private String passOrNFC;
    private boolean justDecrypted;
    private String filePath;


    public BackupWalletTask(PayBitsApplication application, Context context, Wallet wallet, String passOrNFC) {
        this.application = application;
        this.context = context;
        this.wallet = wallet;
        this.passOrNFC = passOrNFC;
        this.keyCache = application.getKeyCache();

        dialog = new AegisProgressDialog(context, 0, context.getString(R.string.backup_spinner_text));
    }

    @Override
    protected void onPreExecute() {
        dialog.show();
    }

    @Override
    protected Object doInBackground(Object[] objects) {
        if (wallet.isEncrypted()) {
            super.decryptWallet(application, wallet, passOrNFC);
            justDecrypted = true;
        }
        else{
            filePath = super.doWalletBackup(wallet, justDecrypted, application, passOrNFC);
        }
        return null;
    }

    @Override
    protected void onPostExecute(Object object) {
        //Wallet is decrypted, now write keys to file.
        if(justDecrypted)
            filePath = super.doWalletBackup(wallet, justDecrypted, application, passOrNFC);

        dialog.dismiss();
        ((BackupCompletedListener) context).onBackupCompleted(filePath);
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
    }
}
