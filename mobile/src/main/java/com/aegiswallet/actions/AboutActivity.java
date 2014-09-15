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
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.method.LinkMovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.aegiswallet.PayBitsApplication;
import com.aegiswallet.R;

/**
 * Created by bsimic on 5/12/14.
 */
public class AboutActivity extends Activity {

    private String TAG = this.getClass().getName();

    Context context = this;
    SharedPreferences prefs;

    Button donateButton;
    Button licenseButton;
    Button faqButton;

    TextView aboutView;
    TextView licenseView;

    PayBitsApplication application;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getActionBar().setCustomView(R.layout.aegis_send_actionbar);

        TextView titleTextView = (TextView) findViewById(R.id.action_bar_title_text);
        titleTextView.setText(getString(R.string.settings_about));

        ImageButton backButton = (ImageButton) findViewById(R.id.action_bar_icon_back);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent openSettingsActivity = new Intent(context, SettingsActivity.class);
                openSettingsActivity.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                context.startActivity(openSettingsActivity);
                finish();
            }
        });

        application = (PayBitsApplication) getApplication();
        initButtons();
    }

    private void initButtons() {
        donateButton = (Button) findViewById(R.id.settings_donate_button);
        licenseButton = (Button) findViewById(R.id.settings_license_button);
        faqButton = (Button) findViewById(R.id.settings_faq_button);

        aboutView = (TextView) findViewById(R.id.about_textview);
        licenseView = (TextView) findViewById(R.id.legal_textview);

        aboutView.setMovementMethod(new ScrollingMovementMethod());
        aboutView.setMovementMethod(LinkMovementMethod.getInstance());

        licenseView.setMovementMethod(new ScrollingMovementMethod());
        licenseView.setMovementMethod(LinkMovementMethod.getInstance());

        faqButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri uriUrl = Uri.parse(getString(R.string.faq_link));
                Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uriUrl);
                startActivity(launchBrowser);
            }
        });
    }

    public void initDonation(View view) {
        Intent donateIntent = new Intent(this, AddressScanActivity.class);
        donateIntent.putExtra("address", getResources().getString(R.string.settings_donate_address));
        startActivity(donateIntent);
    }

    public void toggleLegal(View view) {
        TextView legalView = (TextView) findViewById(R.id.legal_textview);
        if (legalView.getVisibility() == View.VISIBLE) {
            legalView.setVisibility(View.GONE);
            licenseButton.setCompoundDrawablesWithIntrinsicBounds(null, null, getResources().getDrawable(R.drawable.right_arrow), null);
        } else {
            legalView.setVisibility(View.VISIBLE);
            licenseButton.setCompoundDrawablesWithIntrinsicBounds(null, null, getResources().getDrawable(R.drawable.down_arrow), null);
        }
    }
}
