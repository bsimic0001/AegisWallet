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

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.aegiswallet.R;
import com.aegiswallet.actions.MainActivity;
import com.aegiswallet.objects.CurrencyPojo;
import com.aegiswallet.utils.Constants;

import java.util.ArrayList;

/**
 * Created by bsimic on 3/17/14.
 */
public class CurrencyAdapter extends ArrayAdapter<CurrencyPojo> {

    ArrayList<CurrencyPojo> currencies;
    SharedPreferences prefs;
    Context context;

    public CurrencyAdapter(Context context, int resourceId, ArrayList<CurrencyPojo> currencies, SharedPreferences prefs){
        super(context, resourceId, currencies);

        this.context = context;
        this.currencies = currencies;
        this.prefs = prefs;

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;

        LayoutInflater vi = (LayoutInflater) getContext().getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);

        v = vi.inflate(R.layout.currency_list_item, null);


        CurrencyPojo p = currencies.get(position);

        if(p != null){
            TextView denominationView = (TextView) v.findViewById(R.id.currency_item_denomination);
            TextView conversionView = (TextView) v.findViewById(R.id.currency_list_item_conversion);

            String currentPref = prefs.getString(Constants.CURRENCY_PREF_KEY, null);

            if(currentPref != null && currentPref.equals(p.getCurrency())){
                denominationView.setText(p.getCurrency() + " - Current Default");
                denominationView.setTextColor(context.getResources().getColor(R.color.custom_blue));
            }
            else
                denominationView.setText(p.getCurrency());

            conversionView.setText(" 1 BTC = " + p.getSymbol() + p.getLast().toString());
        }

        ChangeCurrencyListener changeCurrencyListener = new ChangeCurrencyListener(position);
        v.setOnClickListener(changeCurrencyListener);

        return v;

    }

    private class ChangeCurrencyListener implements View.OnClickListener{

        private int currPosition;

        public ChangeCurrencyListener(int currPosition){
            this.currPosition = currPosition;
        }

        @Override
        public void onClick(View view) {

            final Dialog dialog = new Dialog(view.getContext());
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.change_currency_prompt);

            TextView textView = (TextView) dialog.findViewById(R.id.change_currency_text);
            Button cancelButton = (Button) dialog.findViewById(R.id.change_currency_cancel_button);
            Button okButton = (Button) dialog.findViewById(R.id.change_currency_ok_button);

            String newCurrency = currencies.get(currPosition).getCurrency();
            textView.setText(context.getString(R.string.change_currency_prompt_message) + " " + newCurrency + "?");

            dialog.setCancelable(true);

            okButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    prefs.edit().putString(Constants.CURRENCY_PREF_KEY, currencies.get(currPosition).getCurrency()).commit();
                    Toast toast = Toast.makeText(context, context.getString(R.string.change_currency_toast) + " " + currencies.get(currPosition).getCurrency(), Toast.LENGTH_LONG);
                    toast.show();
                    context.startActivity(new Intent(context, MainActivity.class));
                }
            });

            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialog.dismiss();
                }
            });

            dialog.show();
        }
    }
}
