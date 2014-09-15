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

package com.aegiswallet.helpers;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.aegiswallet.R;
import com.aegiswallet.utils.Constants;


public class PasswordCheckHelper {

    ProgressBar progressBar;
    EditText password;
    TextView passwordStrength;
    Context context;
    int i = 1;

    public PasswordCheckHelper(Context context, ProgressBar progressBar, TextView passwordStrength, EditText password) {
        this.context = context;
        this.progressBar = progressBar;
        this.passwordStrength = passwordStrength;
        this.password = password;
    }

    public void setupProgressMeter() {
        password.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
                if (password.getText().toString().length() == 0) {
                    //no password provided yet...
                    if(progressBar != null)
                        progressBar.setProgress(0);
                    else if(passwordStrength != null)
                        passwordStrength.setText("");
                } else {
                    calculateStrength(password.getText().toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }
        });
    }

    public int getScore(String password){
        int currentScore = 0;
        boolean sawUpper = false;
        boolean sawLower = false;
        boolean sawDigit = false;
        boolean sawSpecial = false;

        if(password.length() >= Constants.WALLET_PASSWORD_VERY_STRONG_LENGTH){
            currentScore += 4;
        }
        else if(password.length() >= Constants.WALLET_PASSWORD_STRONG_LENGTH){
            currentScore += 3;
        }
        else if (password.length() >= Constants.WALLET_PASSWORD_MIN_LENGTH){
            currentScore += 1;
        }
        else{
            currentScore -= 1;
        }

        // Do this as efficiently as possible.
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (!sawSpecial && !Character.isLetterOrDigit(c)) {
                currentScore += 1;
                sawSpecial = true;
            } else {
                if (!sawDigit && Character.isDigit(c)) {
                    currentScore += 1;
                    sawDigit = true;
                } else {
                    if (!sawUpper || !sawLower) {
                        if (Character.isUpperCase(c))
                            sawUpper = true;
                        else
                            sawLower = true;
                        if (sawUpper && sawLower)
                            currentScore += 1;
                    }
                }
            }
        }

        return currentScore;
    }

    private void calculateStrength(String password) {

        int currentScore = getScore(password);

        if (progressBar != null) {

            switch (currentScore) {
                case 0:
                    progressBar.setProgress(20);
                    break;
                case 1:
                    progressBar.setProgress(40);
                    break;
                case 2:
                    progressBar.setProgress(60);
                    break;
                case 3:
                    progressBar.setProgress(80);
                    break;
                case 4:
                    progressBar.setProgress(100);
                    break;
                default:
            }
        } else if(passwordStrength != null) {
            switch (currentScore) {
                case -1:
                    passwordStrength.setText(context.getString(R.string.password_very_weak));
                    passwordStrength.setTextColor(context.getResources().getColor(R.color.custom_red));
                case 0:
                    passwordStrength.setText(context.getString(R.string.password_very_weak));
                    passwordStrength.setTextColor(context.getResources().getColor(R.color.custom_red));
                    break;
                case 1:
                    passwordStrength.setText(context.getString(R.string.password_weak));
                    passwordStrength.setTextColor(context.getResources().getColor(R.color.custom_red));
                    break;
                case 2:
                    passwordStrength.setText(context.getString(R.string.password_medium));
                    passwordStrength.setTextColor(context.getResources().getColor(R.color.custom_blue));
                    break;
                case 3:
                    passwordStrength.setText(context.getString(R.string.password_strong));
                    passwordStrength.setTextColor(context.getResources().getColor(R.color.custom_green));
                    break;
                case 4:
                    passwordStrength.setText(context.getString(R.string.password_very_strong));
                    passwordStrength.setTextColor(context.getResources().getColor(R.color.custom_green));
                    break;
                case 5:
                    passwordStrength.setText(context.getString(R.string.password_very_strong));
                    passwordStrength.setTextColor(context.getResources().getColor(R.color.custom_green));
                    break;
                case 6:
                    passwordStrength.setText(context.getString(R.string.password_very_strong));
                    passwordStrength.setTextColor(context.getResources().getColor(R.color.custom_green));
                    break;
                case 7:
                    passwordStrength.setText(context.getString(R.string.password_very_strong));
                    passwordStrength.setTextColor(context.getResources().getColor(R.color.custom_green));
                    break;
                default:
            }
        }
    }


}



