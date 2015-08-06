package com.aegiswallet.helpers;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by HyperCorp on 2/26/15.
 */
public class CurrencyExchangeHelper {


    private SharedPreferences prefs;
    private Context context;

    public CurrencyExchangeHelper(SharedPreferences prefs, Context context){

        this.prefs = prefs;
        this.context = context;
    }

}
