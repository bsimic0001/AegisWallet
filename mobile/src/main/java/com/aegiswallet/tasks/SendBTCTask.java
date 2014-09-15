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
import android.util.Log;

import com.aegiswallet.PayBitsApplication;
import com.aegiswallet.R;
import com.aegiswallet.actions.MainActivity;
import com.aegiswallet.objects.KeyCache;
import com.aegiswallet.services.PeerBlockchainService;
import com.aegiswallet.widgets.AegisProgressDialog;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.crypto.KeyCrypter;

import org.spongycastle.crypto.params.KeyParameter;

import java.math.BigInteger;

/**
 * Created by bsimic on 3/14/14.
 */
public class SendBTCTask extends BaseTask {

    private String TAG = this.getClass().getName();
    private Context context;
    Wallet wallet;
    String passOrNfc;
    PayBitsApplication application;
    private AegisProgressDialog pd;

    private String toAddress;
    private BigInteger sendAmount;
    private KeyCrypter keyCrypter;
    private boolean justDecrypted;
    private String tagText;


    public SendBTCTask(Context context,
                       Wallet wallet,
                       String passOrNfc,
                       String toAddress,
                       BigInteger sendAmount,
                       PayBitsApplication application,
                       String tagText
    ) {

        this.context = context;
        this.wallet = wallet;
        this.passOrNfc = passOrNfc;
        this.toAddress = toAddress;
        this.sendAmount = sendAmount;
        this.application = application;
        this.tagText = tagText;

        if (wallet.isEncrypted())
            keyCrypter = wallet.getKeyCrypter();

        pd = new AegisProgressDialog(context, 0, context.getString(R.string.send_btc_spinner_text));
    }

    @Override
    protected void onPreExecute() {
        pd.show();
    }

    @Override
    protected Object doInBackground(Object[] objects) {

        if (wallet.isEncrypted()) {
            super.decryptWallet(application, wallet, passOrNfc);
            justDecrypted = true;
        }
        return null;
    }

    @Override
    protected void onPostExecute(Object o) {
        if (justDecrypted) {
            final Intent intent = new Intent(PeerBlockchainService.ACTION_BROADCAST_TRANSACTION, null, context, PeerBlockchainService.class);
            intent.putExtra("address", toAddress);
            intent.putExtra("amount", sendAmount.toString());
            intent.putExtra("password", passOrNfc);
            intent.putExtra("justDecrypted", true);
            intent.putExtra("tagText", tagText.trim());
            context.startService(intent);
        }
        else{
            final Intent intent = new Intent(PeerBlockchainService.ACTION_BROADCAST_TRANSACTION, null, context, PeerBlockchainService.class);
            intent.putExtra("address", toAddress);
            intent.putExtra("amount", sendAmount.toString());
            intent.putExtra("password", passOrNfc);
            intent.putExtra("justDecrypted", false);
            intent.putExtra("tagText", tagText.trim());

            context.startService(intent);
        }

        pd.dismiss();

        Intent openMainActivity = new Intent(context, MainActivity.class);
        openMainActivity.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        context.startActivity(openMainActivity);
    }
}
