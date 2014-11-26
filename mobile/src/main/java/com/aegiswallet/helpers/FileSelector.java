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

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.aegiswallet.PayBitsApplication;
import com.aegiswallet.R;
import com.aegiswallet.actions.NFCActivity;
import com.aegiswallet.tasks.ImportWalletTask;
import com.aegiswallet.utils.WalletUtils;

/**
 * Created by bsimic on 5/7/14.
 */
public class FileSelector {

    private String TAG = this.getClass().getName();

    SelectorAdapter adapter;
    Context context;
    String[] files;
    PayBitsApplication application;
    String chosenFile;
    int chosenIndex = -1;
    String chosenFileName = null;
    ListView listView;
    EditText passwordField;
    TextView nfcTextView;
    String filePassword;


    public FileSelector(Context context, String[] files, PayBitsApplication application) {
        this.context = context;
        this.files = files;
        this.application = application;

    }

    public void showFileSelector() {

        final Dialog dialog = new Dialog(context);

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        WindowManager.LayoutParams layoutParams = dialog.getWindow().getAttributes();

        layoutParams.gravity = Gravity.CENTER;
        dialog.getWindow().setAttributes(layoutParams);
        dialog.getWindow().setBackgroundDrawable(
                new ColorDrawable(android.graphics.Color.TRANSPARENT));

        dialog.setCanceledOnTouchOutside(true);
        dialog.setContentView(R.layout.file_selector_view);

        dialog.setCancelable(true);
        passwordField = (EditText) dialog.findViewById(R.id.file_selector_password_field);
        nfcTextView = (TextView) dialog.findViewById(R.id.file_selector_nfc_notification);

        Button button = (Button) dialog.findViewById(R.id.file_selector_cancel_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        Button okButton = (Button) dialog.findViewById(R.id.file_selector_import_button);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String password = passwordField.getText().toString();

                if (WalletUtils.checkFileBackupNFCEncrypted(chosenFileName)) {
                    dialog.dismiss();
                    Intent intent = new Intent(context, NFCActivity.class);
                    intent.putExtra("nfc_action", "restore_backup");
                    intent.putExtra("fileNameForRestoreBackup", chosenFileName);
                    context.startActivity(intent);

                } else {

                    if (WalletUtils.checkPasswordForBackupFile(chosenFileName, password)) {
                        dialog.dismiss();
                        filePassword = password;
                        ImportWalletTask importWalletTask = new ImportWalletTask(application, application.getWallet(), context, password, chosenFileName);
                        //importWalletTask.execute();
                        importWalletTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);

                    } else if(chosenFileName != null) {
                        Toast.makeText(context, context.getString(R.string.invalid_password_string), Toast.LENGTH_SHORT).show();
                    }
                    else if(chosenFileName == null){
                        Toast.makeText(context, context.getString(R.string.choose_file_string), Toast.LENGTH_SHORT).show();
                    }
                }

            }
        });


        listView = (ListView) dialog.findViewById(R.id.file_selector_listview);

        adapter = new SelectorAdapter(files);

        listView.setAdapter(adapter);

        listView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (view.isSelected()) {
                    view.setBackgroundColor(context.getResources().getColor(R.color.custom_blue));
                } else {
                    view.setBackgroundColor(context.getResources().getColor(R.color.aegis_white));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                //Do nothing. This should never happen
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                chosenFile = adapter.getItem(i);
            }
        });

        dialog.show();
    }

    private class SelectorAdapter extends ArrayAdapter<String> {

        public SelectorAdapter(String[] files) {
            super(context, R.layout.custom_sharer_row, files);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.file_selector_row, parent, false);

            final RadioButton radioButton = (RadioButton) convertView.findViewById(R.id.file_selector_radio);

            radioButton.setText(getItem(position));
            radioButton.setChecked(position == chosenIndex);
            radioButton.setTag(position);


            radioButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (radioButton.isChecked()) {
                        chosenIndex = position;
                        chosenFileName = radioButton.getText().toString();
                        notifyDataSetInvalidated();
                        listView.setSelection(position);

                        if (passwordField != null) {
                            if (WalletUtils.checkFileBackupNFCEncrypted(chosenFileName)) {
                                passwordField.setVisibility(View.GONE);
                                nfcTextView.setVisibility(View.VISIBLE);
                            } else {
                                passwordField.setVisibility(View.VISIBLE);
                                nfcTextView.setVisibility(View.GONE);
                            }
                        }
                    }

                }
            });


            return convertView;

        }


    }

    public String getChosenFile() {
        return chosenFile;
    }
    public String getFilePassword(){ return filePassword; }
}
