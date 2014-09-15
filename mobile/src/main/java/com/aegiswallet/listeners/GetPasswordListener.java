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

package com.aegiswallet.listeners;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.aegiswallet.R;
import com.aegiswallet.utils.Constants;

/**
 * Created by bsimic on 3/14/14.
 */
public class GetPasswordListener implements View.OnClickListener{

    Context context;
    String password;
    int action;

    public GetPasswordListener(Context context, int action){
        this.context = context;
        this.action = action;
    }

    @Override
    public void onClick(View view) {
        LayoutInflater li = LayoutInflater.from(context);
        View passwordPromptView = li.inflate(R.layout.password_prompt, null);

        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
        alertBuilder.setView(passwordPromptView);

        final EditText passwordInput = (EditText) passwordPromptView.findViewById(R.id.password_prompt_field);

        alertBuilder.setCancelable(true);
        alertBuilder.setPositiveButton(context.getString(R.string.okay_button_string), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                password = passwordInput.getText().toString();
                if (password != null &&
                        password.length() >= Constants.WALLET_PASSWORD_MIN_LENGTH) {

                    ((PasswordProvidedListener) context).onPasswordProvided(password, action);

                }
            }
        });

        alertBuilder.setNegativeButton(context.getString(R.string.cancel_button_string), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });

        AlertDialog alertDialog = alertBuilder.create();
        alertDialog.show();

    }


}
