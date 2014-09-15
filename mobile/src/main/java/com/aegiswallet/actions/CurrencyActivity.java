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
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.aegiswallet.R;
import com.aegiswallet.adapters.CurrencyAdapter;
import com.aegiswallet.objects.CurrencyPojo;
import com.aegiswallet.utils.BasicUtils;
import com.aegiswallet.utils.Constants;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by bsimic on 3/17/14.
 */
public class CurrencyActivity extends Activity {

    private SharedPreferences prefs;
    private Context context = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.currency_view);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getActionBar().setCustomView(R.layout.aegis_send_actionbar);

        TextView titleTextView = (TextView) findViewById(R.id.action_bar_title_text);
        titleTextView.setText(getString(R.string.currencies_activity_label));


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


        ArrayList<CurrencyPojo> currencies = new ArrayList<CurrencyPojo>();

        JSONObject jsonObject = BasicUtils.parseJSONData(getApplicationContext(), Constants.BLOCKCHAIN_CURRENCY_FILE_NAME);

        Iterator i = jsonObject.keys();
        while(i.hasNext()) {

            try {
                String currency = (String) i.next();
                JSONObject detailObj = jsonObject.getJSONObject(currency);
                CurrencyPojo newPojo = new CurrencyPojo(currency, detailObj.getDouble("last"), detailObj.getString("symbol"));

                currencies.add(newPojo);
            } catch (JSONException e) {
                Log.e("Currency Activity", "JSON Exception " + e.getMessage());
            }
        }

        String currentCurrency = prefs.getString(Constants.CURRENCY_PREF_KEY, null);
        CurrencyPojo firstPojo = currencies.get(0);


        for(int j = 0; j < currencies.size(); j++){
            CurrencyPojo pojo = currencies.get(j);
            if(pojo.getCurrency().equals(currentCurrency)){
                currencies.set(0, pojo);
                currencies.set(j, firstPojo);
            }
        }

        CurrencyAdapter currencyAdapter = new CurrencyAdapter(this, R.layout.currency_list_item, currencies, prefs);

        ListView transactionListView = (ListView) findViewById(R.id.currency_list);
        transactionListView.setAdapter(currencyAdapter);


    }
}
