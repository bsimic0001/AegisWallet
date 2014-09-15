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

package com.aegiswallet.adapters;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.aegiswallet.R;
import com.aegiswallet.utils.BasicUtils;
import com.aegiswallet.utils.Constants;
import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.script.Script;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bsimic on 3/11/14.
 */
public class AddressListAdapter extends ArrayAdapter<Address> {

    private String TAG = this.getClass().getName();

    private ArrayList<Address> addresses;
    private Context context;
    private Wallet wallet;
    private View.OnClickListener onClickListener;
    SharedPreferences prefs;
    private String currentDefault;

    public AddressListAdapter(Context context,
                              int resourceId,
                              ArrayList<Address> addresses,
                              Wallet wallet,
                              SharedPreferences prefs) {

        super(context, resourceId, addresses);

        this.addresses = addresses;
        this.context = context;
        this.wallet = wallet;
        this.prefs = prefs;
        this.currentDefault = prefs.getString(Constants.PREFS_KEY_SELECTED_ADDRESS, null);
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        View v = null;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) getContext().getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.address_detail_row, parent, false);
        }

        Address a = null;
        if (addresses != null) {
            a = addresses.get(position);
        }

        if (a != null) {
            TextView addressView = (TextView) v.findViewById(R.id.address_string);
            Button copyToClipboardButton = (Button) v.findViewById(R.id.copy_to_clipboard_button);
            Button unWatchAddress = (Button) v.findViewById(R.id.unwatch_address_button);

            AddressOnClickListener listener = new AddressOnClickListener(position, a.toString());
            copyToClipboardButton.setOnClickListener(listener);

            Button exploreAddressButton = (Button) v.findViewById(R.id.explore_address_button);
            exploreAddressButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (BasicUtils.isNetworkAvailable(context)) {
                        String url = Constants.BLOCKCHAIN_ADDRESS_URL + addresses.get(position).toString();
                        Intent viewTxIntent = new Intent(Intent.ACTION_VIEW);
                        viewTxIntent.setData(Uri.parse(url));
                        context.startActivity(viewTxIntent);
                    } else {
                        Toast.makeText(context, context.getString(R.string.no_internet_connection_available_string), 500).show();
                    }
                }
            });

            Button setAsDefaultButton = (Button) v.findViewById(R.id.set_default_address_button);
            setAsDefaultButton.setOnClickListener(listener);

            if (a.toString().equals(currentDefault)) {
                setAsDefaultButton.setText(context.getString(R.string.current_default_string));
                setAsDefaultButton.setTextColor(context.getResources().getColor(R.color.custom_blue));
            }

            addressView.setText(a.toString());

            final Address copyAddress = a;

            if (wallet.isAddressWatched(a)) {
                setAsDefaultButton.setVisibility(View.GONE);

                UnWatchAddressOnClickListener unWatchAddressOnClickListener = new UnWatchAddressOnClickListener(position, a);
                unWatchAddress.setOnClickListener(unWatchAddressOnClickListener);

                setAsDefaultButton.setEnabled(false);
                setAsDefaultButton.setText(context.getString(R.string.wallet_watch_address));
                setAsDefaultButton.setTextColor(context.getResources().getColor(R.color.guava_color));

                unWatchAddress.setTextColor(context.getResources().getColor(R.color.guava_color));
                unWatchAddress.setVisibility(View.VISIBLE);
            }
        }

        return v;
    }

    public class AddressOnClickListener implements View.OnClickListener {

        private int position;
        private String address;

        public AddressOnClickListener(int position, String address) {
            this.position = position;
            this.address = address;
        }

        @Override
        public void onClick(View view) {

            Toast toast = null;
            if (view.getId() == R.id.copy_to_clipboard_button) {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("address", address);
                clipboard.setPrimaryClip(clip);
                toast = Toast.makeText(context, "Address Copied!", 250);
            } else if (view.getId() == R.id.set_default_address_button) {
                prefs.edit().putString(Constants.PREFS_KEY_SELECTED_ADDRESS, address.toString()).commit();
                updateView();
                toast = Toast.makeText(context, "Set As Default!", 250);
            }

            toast.show();
        }

    }

    public class UnWatchAddressOnClickListener implements View.OnClickListener {

        private int position;
        private Address address;

        public UnWatchAddressOnClickListener(int position, Address address) {
            this.position = position;
            this.address = address;
        }

        @Override
        public void onClick(View view) {

            Toast toast = null;

            try {

                Address addressToRemove = new Address(Constants.NETWORK_PARAMETERS, address.toString());

                boolean success = wallet.removeWatchedAddress(addressToRemove);
                if (success) {
                    toast = Toast.makeText(context, context.getString(R.string.watch_address_removed_success), 500);
                    updateView();
                } else {
                    toast = Toast.makeText(context, context.getString(R.string.watch_address_not_removed), 500);
                }

                toast.show();
            } catch (AddressFormatException e) {
                Log.e(TAG, e.getMessage());
            }
        }

    }

    @Override
    public Address getItem(int position) {
        return addresses.get(position);
    }

    public void setCurrentDefault(String currentDefault) {
        this.currentDefault = currentDefault;
    }

    public void updateView() {
        List<ECKey> keys = wallet.getKeys();

        ArrayList<Address> addressArrayList = new ArrayList<Address>();
        for (ECKey key : keys) {
            Address a = key.toAddress(Constants.NETWORK_PARAMETERS);
            addressArrayList.add(a);
        }

        List<Script> watchedScripts = wallet.getWatchedScripts();

        for (Script s : watchedScripts) {
            addressArrayList.add(s.getToAddress(Constants.NETWORK_PARAMETERS));
        }

        setCurrentDefault(prefs.getString(Constants.PREFS_KEY_SELECTED_ADDRESS, null));

        notifyDataSetInvalidated();
        clear();
        addAll(addressArrayList);
        notifyDataSetChanged();
    }
}
