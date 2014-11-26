package com.aegiswallet.helpers;

import android.content.Context;

import com.aegiswallet.PayBitsApplication;
import com.aegiswallet.R;
import com.aegiswallet.listeners.WalletDecryptedListener;
import com.aegiswallet.widgets.AegisProgressDialog;
import com.google.bitcoin.core.Wallet;

/**
 * Created by HyperCorp on 11/10/14.
 */
public class DecryptHelper extends EncryptionHelperBase {

    private String TAG = this.getClass().getName();

    private Context context;
    private Wallet wallet;
    private String passOrNFC;
    private AegisProgressDialog pd;
    private PayBitsApplication application;

    public DecryptHelper(Context context, Wallet wallet, String passOrNFC, PayBitsApplication application){
        this.context = context;
        this.wallet = wallet;
        this.passOrNFC = passOrNFC;
        this.application = application;

        pd = new AegisProgressDialog(context, 0, context.getString(R.string.decrypt_dialog_spinner_text));
    }

    public void doDecryption(){
        pd = new AegisProgressDialog(context, 0, context.getString(R.string.decrypt_dialog_spinner_text));
        pd.show();
        super.decryptWallet(application, wallet, passOrNFC);

        pd.dismiss();

        ((WalletDecryptedListener) context).onWalletDecrypted(passOrNFC);

    }
}
