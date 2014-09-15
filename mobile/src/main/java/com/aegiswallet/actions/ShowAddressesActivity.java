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

package com.aegiswallet.actions;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.aegiswallet.PayBitsApplication;
import com.aegiswallet.R;
import com.aegiswallet.adapters.AddressListAdapter;
import com.aegiswallet.listeners.PasswordProvidedListener;
import com.aegiswallet.tasks.AddWalletAddressTask;
import com.aegiswallet.utils.Constants;
import com.aegiswallet.utils.NfcUtils;
import com.aegiswallet.utils.WalletUtils;
import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.script.Script;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bsimic on 3/13/14.
 */
public class ShowAddressesActivity extends Activity implements PasswordProvidedListener {

    private String TAG = this.getClass().getName();

    Wallet wallet;
    PayBitsApplication application;
    Button addAddressButton;
    Button setDefaultButton;
    private Context context = this;
    private AddressListAdapter addressListAdapter;
    private SharedPreferences prefs;
    private boolean nfcEnabled;
    private boolean addAddress;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.address_view);

        application = (PayBitsApplication) getApplication();
        wallet = application.getWallet();

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getActionBar().setCustomView(R.layout.aegis_send_actionbar);

        TextView titleTextView = (TextView) findViewById(R.id.action_bar_title_text);
        titleTextView.setText(getString(R.string.addresses_activity_label));


        ImageButton backButton = (ImageButton) findViewById(R.id.action_bar_icon_back);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent openMainActivity = new Intent(context, MainActivity.class);
                openMainActivity.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                context.startActivity(openMainActivity);
                finish();
            }
        });

        ArrayList<Address> addressArrayList = new ArrayList<Address>();
        List<ECKey> keys = wallet.getKeys();

        for (ECKey key : keys) {
            Address a = key.toAddress(Constants.NETWORK_PARAMETERS);
            addressArrayList.add(a);
        }


        List<Script> watchedScripts = wallet.getWatchedScripts();

        for (Script s : watchedScripts){
            addressArrayList.add(s.getToAddress(Constants.NETWORK_PARAMETERS));
        }

        addressListAdapter = new AddressListAdapter(this,
                R.layout.address_detail_row,
                addressArrayList,
                wallet,
                PreferenceManager.getDefaultSharedPreferences(this));

        ListView transactionListView = (ListView) findViewById(R.id.address_list_view);
        transactionListView.setAdapter(addressListAdapter);

        determineNFCEnabled();

        handleButtons();
    }

    private void handleButtons() {
        addAddressButton = (Button) findViewById(R.id.add_address_button);

        if (wallet.isEncrypted()) {

            addAddressButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if (application.getKeyCache() != null) {
                        AddWalletAddressTask addWalletAddressTask = new AddWalletAddressTask(context, wallet, null, application, addressListAdapter, application.getKeyCache());
                        addWalletAddressTask.execute();
                    } else if (nfcEnabled) {
                        application.showNFCPrompt(context);
                        addAddress = true;
                    } else {
                        application.showPasswordPrompt(context, Constants.ACTION_DECRYPT);
                    }
                }
            });

        } else {
            addAddressButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    AddWalletAddressTask addWalletAddressTask = new AddWalletAddressTask(context, wallet, null, application, addressListAdapter, application.getKeyCache());
                    addWalletAddressTask.execute();
                }
            });
        }


    }

    @Override
    public void onPasswordProvided(String password, int action) {
        AddWalletAddressTask addWalletAddressTask = new AddWalletAddressTask(context, wallet, password, application, addressListAdapter, application.getKeyCache());
        addWalletAddressTask.execute();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        byte[] result = NfcUtils.getData(intent);

        Vibrator v = (Vibrator) this.context.getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(500);

        String resultString = null;

        if (result != null)
            resultString = new String(result);

        String shamirX2Hashed = application.getPrefs().getString(Constants.SHAMIR_X2_HASHED, null);

        if (resultString != null && shamirX2Hashed.equals(WalletUtils.convertToSha256(resultString))) {

            if (resultString != null && addAddress) {
                application.cancelNFCPrompt(context);
                AddWalletAddressTask addWalletAddressTask = new AddWalletAddressTask(context, wallet, resultString, application, addressListAdapter, application.getKeyCache());
                addWalletAddressTask.execute();
                addAddress = false;
            }
        } else {
            Toast.makeText(context, getString(R.string.nfc_tag_invalid_string), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        determineNFCEnabled();
        NfcUtils.listen(this, getClass());
    }

    private void determineNFCEnabled() {
        nfcEnabled = prefs.contains(Constants.SHAMIR_ENCRYPTED_KEY) ? false : true;
    }
}
