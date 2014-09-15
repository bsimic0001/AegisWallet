package com.aegiswallet.helpers;

import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.aegiswallet.R;
import com.aegiswallet.actions.MainActivity;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * Created by bsimic on 5/7/14.
 */
public class CustomSharer {

    ShareAdapter adapter;
    Button button;
    Intent emailIntent = new Intent(Intent.ACTION_SEND);
    Activity context;
    String fileName;

    public CustomSharer(Activity context, String fileName) {
        this.context = context;
        this.fileName = fileName;
    }

    public void showCustomChooser() {

        final Dialog dialog = new Dialog(context);

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        WindowManager.LayoutParams layoutParams = dialog.getWindow().getAttributes();

        layoutParams.gravity = Gravity.CENTER;
        dialog.getWindow().setAttributes(layoutParams);
        dialog.getWindow().setBackgroundDrawable(
                new ColorDrawable(android.graphics.Color.TRANSPARENT));

        dialog.setCanceledOnTouchOutside(true);
        dialog.setContentView(R.layout.custom_chooser_view);

        dialog.setCancelable(true);

        ListView listView = (ListView) dialog.findViewById(R.id.custom_chooser_list_view);

        PackageManager packageManager = context.getPackageManager();

        emailIntent.setType("message/rfc822");
        emailIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        emailIntent.putExtra(Intent.EXTRA_EMAIL, "");
        emailIntent.putExtra(Intent.EXTRA_SUBJECT,
                "Aegis Wallet Backup");
        emailIntent.putExtra(Intent.EXTRA_TEXT, context.getString(R.string.wallet_backup_text));
        emailIntent.setType("x-bitcoin/aegis-backup");

        if(fileName != null && emailIntent != null)
            emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(fileName)));

        List<ResolveInfo> launchables = packageManager.queryIntentActivities(emailIntent, 0);

        Collections.sort(launchables, new ResolveInfo.DisplayNameComparator(packageManager));

        adapter = new ShareAdapter(packageManager, launchables);

        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                ResolveInfo launchable = adapter.getItem(i);
                ActivityInfo activityInfo = launchable.activityInfo;

                emailIntent.setType("plain/text");
                ComponentName componentName = new ComponentName(activityInfo.applicationInfo.packageName, activityInfo.name);
                emailIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                emailIntent.setComponent(componentName);
                context.startActivity(emailIntent);

                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private class ShareAdapter extends ArrayAdapter<ResolveInfo> {

        private PackageManager packageManager;

        public ShareAdapter(PackageManager packageManager, List<ResolveInfo> apps) {
            super(context, R.layout.custom_sharer_row, apps);
            this.packageManager = packageManager;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            convertView = context.getLayoutInflater().inflate(R.layout.custom_sharer_row, parent, false);

            TextView label = (TextView) convertView.findViewById(R.id.custom_sharer_label);
            label.setText(getItem(position).loadLabel(packageManager));

            ImageView icon = (ImageView) convertView.findViewById(R.id.custom_sharer_icon);
            icon.setImageDrawable(getItem(position).loadIcon(packageManager));

            return convertView;

        }
    }

}
