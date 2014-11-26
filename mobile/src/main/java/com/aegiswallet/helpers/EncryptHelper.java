package com.aegiswallet.helpers;

import android.content.Context;
import android.util.Log;

import com.aegiswallet.PayBitsApplication;
import com.aegiswallet.R;
import com.aegiswallet.listeners.WalletEncryptedListener;
import com.aegiswallet.widgets.AegisProgressDialog;
import com.google.bitcoin.core.Wallet;

/**
 * Created by HyperCorp on 11/10/14.
 */
public class EncryptHelper extends EncryptionHelperBase{

    private String TAG = this.getClass().getName();

    private Context context;
    private Wallet wallet;
    private String passOrX2;
    private AegisProgressDialog pd;
    private PayBitsApplication application;
    private boolean nfc;

    public EncryptHelper(Context context, Wallet wallet, String passOrX2, PayBitsApplication application, boolean nfc){
        this.context = context;
        this.wallet = wallet;
        this.passOrX2 = passOrX2;
        this.application = application;
        this.nfc = nfc;

        Log.d(TAG, "Encrypting wallet helper... inside constructor");

    }

    public void doEncryption(){
        Log.d(TAG, "Encrypting wallet with NFC Tag... inside doInBackground");

        pd = new AegisProgressDialog(context, 0, context.getString(R.string.decrypt_dialog_spinner_text));
        pd.show();


        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    doEncryptionFunction();

                    while(!wallet.isEncrypted()){
                        Log.d(TAG, "wallet is still not encrypted");
                        wait(1000);
                    }


                } catch (Exception e) {

                }
                pd.dismiss();
                ((WalletEncryptedListener) context).onWalletEncrypted();

            }
        }).start();

    }

    private void doEncryptionFunction(){
        if(nfc) {
            super.encryptWalletShamirNFC(wallet, passOrX2, application);
            Log.d(TAG, "Encrypting wallet with NFC Tag...");
        }
        else {
            Log.d(TAG, "Encrypting wallet with SSS...");
            System.out.println("Encrypting wallet with SSS...");
            super.encryptWalletWithShamir(wallet, passOrX2, application);
        }
    }


}
