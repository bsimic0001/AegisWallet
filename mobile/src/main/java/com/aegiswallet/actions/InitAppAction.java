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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.aegiswallet.PayBitsApplication;
import com.aegiswallet.R;
import com.aegiswallet.helpers.PasswordCheckHelper;
import com.aegiswallet.listeners.WalletEncryptedListener;
import com.aegiswallet.tasks.EncryptWalletTask;
import com.aegiswallet.utils.BasicUtils;
import com.aegiswallet.utils.Constants;
import com.aegiswallet.utils.NfcUtils;
import com.aegiswallet.utils.WalletUtils;

/**
 * Created by bsimic on 5/9/14.
 */
public class InitAppAction extends Activity implements WalletEncryptedListener {

    private String TAG = this.getClass().getName();

    private EditText passwordInput;
    private EditText passwordConfirmInput;
    private TextView passwordStrength;
    private Context context = this;
    private PayBitsApplication application;
    private PasswordCheckHelper passwordCheckHelper;
    private String x2Value;
    private SharedPreferences prefs;
    private EncryptWalletTask encryptWalletTask;

    private TextView nfcInfoTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        setContentView(R.layout.activity_init_app);

        application = (PayBitsApplication) getApplication();

        passwordInput = (EditText) findViewById(R.id.init_password_field);
        passwordConfirmInput = (EditText) findViewById(R.id.init_password_confirm_field);

        passwordStrength = (TextView) findViewById(R.id.init_password_strength_textbox);

        passwordCheckHelper = new PasswordCheckHelper(context, null, passwordStrength, passwordInput);
        passwordCheckHelper.setupProgressMeter();

        nfcInfoTextView = (TextView) findViewById(R.id.nfc_info_text_view);

        x2Value = prefs.getString(Constants.SHAMIR_ENCRYPTED_KEY, null);

        initButtons();

        if (NfcUtils.nfcIsOff(this)) {
            Toast.makeText(context, getString(R.string.nfc_off_string), Toast.LENGTH_LONG).show();
        }

        if (NfcUtils.nfcIsntAvailable(this)) {
            Toast.makeText(context, getString(R.string.nfc_unavailable), Toast.LENGTH_LONG).show();
        }
    }

    private void initButtons() {
        final Button continueButton = (Button) findViewById(R.id.init_continue_button);

        continueButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                String password = passwordInput.getText().toString();
                String passwordConfirm = passwordConfirmInput.getText().toString();

                if (password.equals(passwordConfirm) && passwordCheckHelper.getScore(password) >= Constants.MINIMUM_PASSWORD_SCORE) {

                    continueButton.setText(getString(R.string.please_wait_string));

                    if (!BasicUtils.isPasswordInDictionary(context, password)) {
                        Toast.makeText(context, getString(R.string.password_guessable), Toast.LENGTH_LONG).show();
                        continueButton.setText(getString(R.string.continue_string));
                    } else {
                        System.out.println("Password is good, about to generateSecureKey");
                        Log.d(TAG, "Password is good, about to generateSecureKey");

                        String passwordSalt = BasicUtils.generateSecureKey();
                        String passwordHash = passwordSalt + WalletUtils.convertToSha256(passwordSalt + password);

                        application.getPrefs().edit().putString(Constants.PASSWORD_HASH, passwordHash).commit();
                        application.getPrefs().edit().putString(Constants.PASSWORD_SALT, passwordSalt).commit();
                        application.getPrefs().edit().putBoolean(Constants.APP_INIT_COMPLETE, true).commit();
                        System.out.println("Starting Encrypt Wallet Tastk...");
                        Log.d(TAG, "Starting Encrypt Wallet Tastk...");

                        encryptWalletTask = new EncryptWalletTask(context, application.getWallet(), password, application, false);
                        //encryptWalletTask.execute();
                        encryptWalletTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
                    }
                } else if (!password.equals(passwordConfirm)) {
                    Toast.makeText(context, getString(R.string.settings_change_password_mismatch), Toast.LENGTH_LONG).show();
                } else if (passwordCheckHelper.getScore(password) < Constants.MINIMUM_PASSWORD_SCORE) {
                    Toast.makeText(context, getString(R.string.invalid_password_strength), Toast.LENGTH_LONG).show();
                }
            }
        });

        if (NfcUtils.nfcIsntAvailable(this)) {
            nfcInfoTextView.setText(R.string.nfc_not_available_string);
        } else if (!NfcUtils.nfcIsntAvailable(this) && NfcUtils.nfcIsOff(this)) {
            nfcInfoTextView.setText(R.string.nfc_is_off);
        }
    }

    @Override
    public void onWalletEncrypted() {
        startMainActivity();
    }

    private void startMainActivity() {
        Intent openMainActivity = new Intent(context, MainActivity.class);
        openMainActivity.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        context.startActivity(openMainActivity);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (encryptWalletTask != null && encryptWalletTask.getStatus().equals(AsyncTask.Status.RUNNING))
            return;

        if (application.getPrefs().getBoolean(Constants.APP_INIT_COMPLETE, false)) {
            startMainActivity();
        }

        NfcUtils.listen(this, getClass());

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        String result = NfcUtils.write(intent, x2Value.getBytes());

        Vibrator v = (Vibrator) this.context.getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(500);

        if (result.equals(Constants.NFC_TAG_WRITTEN)) {
            application.getPrefs().edit().putBoolean(Constants.APP_INIT_COMPLETE, true).commit();

            encryptWalletTask = new EncryptWalletTask(context,
                    application.getWallet(),
                    prefs.getString(Constants.SHAMIR_ENCRYPTED_KEY, null),
                    application,
                    true);

            prefs.edit().putString(Constants.SHAMIR_X2_HASHED, WalletUtils.convertToSha256(x2Value)).commit();
            prefs.edit().remove(Constants.SHAMIR_ENCRYPTED_KEY).commit();

            //encryptWalletTask.execute();
            encryptWalletTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);

        }

        String resultMessage = result;

        if(result.equals(Constants.NFC_READ_ONLY))
            resultMessage = getString(R.string.tag_tag_is_read_only_string);
        else if(result.equals(Constants.NFC_TAG_TOO_SMALL))
            resultMessage = getString(R.string.tag_too_small_string);
        else if(result.equals(Constants.NFC_TAG_FORMATTED))
            resultMessage = getString(R.string.tag_formatted_string);
        else if(result.equals(Constants.NFC_CANT_FORMAT))
            resultMessage = getString(R.string.tag_error_formatting_tag_string);
        else if(result.equals(Constants.NFC_WRITE_EXCEPTION))
            resultMessage = getString(R.string.tag_nfc_write_exception_string);
        else if(result.equals(Constants.NFC_CANT_CONNECT))
            resultMessage = getString(R.string.tag_cant_connect_nfc_adapter_string);

        Toast.makeText(context, resultMessage, Toast.LENGTH_LONG).show();
    }
}
