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
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.text.Spannable;
import android.text.SpannableString;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.aegiswallet.PayBitsApplication;
import com.aegiswallet.R;
import com.aegiswallet.adapters.PendingTransactionListAdapter;
import com.aegiswallet.adapters.TransactionListAdapter;
import com.aegiswallet.helpers.CustomSharer;
import com.aegiswallet.helpers.CustomTypefaceSpan;
import com.aegiswallet.listeners.BackupCompletedListener;
import com.aegiswallet.listeners.ImportCompletedListener;
import com.aegiswallet.listeners.PasswordProvidedListener;
import com.aegiswallet.listeners.SMSTaskCompletedListener;
import com.aegiswallet.listeners.WalletDecryptedListener;
import com.aegiswallet.listeners.WalletEncryptedListener;
import com.aegiswallet.objects.SMSTransactionPojo;
import com.aegiswallet.services.PeerBlockchainService;
import com.aegiswallet.tasks.BackupWalletTask;
import com.aegiswallet.tasks.DecryptWalletTask;
import com.aegiswallet.tasks.EncryptWalletTask;
import com.aegiswallet.tasks.GetCurrencyInfoTask;
import com.aegiswallet.tasks.HandleSMSResponsesTask;
import com.aegiswallet.utils.BasicUtils;
import com.aegiswallet.utils.Constants;
import com.aegiswallet.utils.WalletUtils;
import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.Wallet;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class MainActivity extends Activity implements PasswordProvidedListener,
        WalletEncryptedListener,
        BackupCompletedListener,
        ImportCompletedListener,
        WalletDecryptedListener,
        SMSTaskCompletedListener{

    private String TAG = this.getClass().getName();
    private ListView transactionListView;
    private ListView pendingTransactionListView;
    private PayBitsApplication application;
    private Wallet wallet;
    private SharedPreferences prefs;
    private Context context = this;

    private ImageButton requestBTCButton;
    private ImageButton sendBTCButton;
    private TextView balanceInCurrencyView;
    private TextView getBalanceInCurrencyViewType;
    private Button mainEncryptionButton;

    private TransactionListAdapter transactionListAdapter;
    private PendingTransactionListAdapter pendingTransactionListAdapter;
    private Handler balanceHandler;

    private TextView walletBalanceView;
    private TextView walletWatchAddressBalanceView;
    private LinearLayout watchedBalanceLinearLayout;
    private TextView blockchainStatus;
    private BroadcastReceiver receiver;
    private NotificationManager notificationManager;


    //Drawer stuff
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;

    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
    private String[] drawerOptions;
    private ArrayAdapter<String> arrayAdapter;
    ArrayList<Transaction> recentTransactions;
    ArrayList<SMSTransactionPojo> pendingTransactions;
    private List<ECKey> keyList;
    private AlertDialog qrCodeAlertDialog = null;
    private ImageButton openDrawerButton;
    private boolean nfcEnabled;
    private final AtomicLong lastViewUpdateTime = new AtomicLong(0);
    private PeerBlockchainService peerBlockchainService;
    private boolean isServiceBound = false;

    private SharedPreferences smsTxnsPrefs;

    private ServiceConnection blockchainServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            peerBlockchainService = ((PeerBlockchainService.LocalBinder) service).getService();
        }

        public void onServiceDisconnected(ComponentName className) {
            peerBlockchainService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        application = (PayBitsApplication) getApplication();
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        smsTxnsPrefs = application.getSharedPreferences(getString(R.string.sms_transaction_filename), Context.MODE_PRIVATE);
        //smsTxnsPrefs.edit().clear().commit();
        wallet = application.getWallet();
        application.initializeShamirSecretSharing(context);

        checkIfAppInitiated();

        setContentView(R.layout.activity_main);
        getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getActionBar().setCustomView(R.layout.aegis_actionbar);

        openDrawerButton = (ImageButton) findViewById(R.id.action_bar_icon);
        openDrawerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mDrawerLayout != null) {
                    if (mDrawerLayout.isDrawerOpen(Gravity.LEFT))
                        mDrawerLayout.closeDrawer(Gravity.LEFT);
                    else
                        mDrawerLayout.openDrawer(Gravity.LEFT);
                }
            }
        });

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        blockchainStatus = (TextView) findViewById(R.id.blockchain_status);
        blockchainStatus.setTypeface(BasicUtils.getCustomTypeFace(getBaseContext()));

        setupBlockchainBroadcastReceiver(blockchainStatus);

        GetCurrencyInfoTask currencyInfoTask = new GetCurrencyInfoTask(getApplicationContext());
        currencyInfoTask.execute();

        watchedBalanceLinearLayout = (LinearLayout) findViewById(R.id.watched_balance_view);

        walletBalanceView = (TextView) findViewById(R.id.wallet_balance);
        walletBalanceView.setTypeface(BasicUtils.getCustomTypeFace(getBaseContext()));

        if (!prefs.contains(Constants.CURRENCY_PREF_KEY)) {
            prefs.edit().putString(Constants.CURRENCY_PREF_KEY, "USD").commit();
        }

        walletWatchAddressBalanceView = (TextView) findViewById(R.id.watch_only_balance);

        updateMainViews();
        determineSelectedAddress();
        application.startBlockchainService(false);
        handleButtons();

        recentTransactions = (ArrayList<Transaction>) wallet.getRecentTransactions(50, true);
        transactionListAdapter = new TransactionListAdapter(this,
                R.layout.transaction_detail_row,
                recentTransactions,
                wallet);

        transactionListView = (ListView) findViewById(R.id.transaction_list);
        if (recentTransactions == null || recentTransactions.size() == 0)
            transactionListView.setEmptyView(findViewById(R.id.transaction_empty_view_main));

        transactionListView.setAdapter(transactionListAdapter);

        handlePendingTransactions();

        initiateHandlers(walletBalanceView);
        application.addWalletListener(balanceHandler);

        checkWalletEncryptionStatus();
        this.registerReceiver(receiver, new IntentFilter(PeerBlockchainService.ACTION_BLOCKCHAIN_STATE));

        doDrawerSetup();

        String message = getIntent().getStringExtra("message");
        if (message != null) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            getIntent().removeExtra("message");
        }

        nfcEnabled = prefs.contains(Constants.SHAMIR_ENCRYPTED_KEY) ? false : true;
        checkBackupDone();
        doBackupReminder();

        isServiceBound = this.bindService(new Intent(this, PeerBlockchainService.class), blockchainServiceConnection, Context.BIND_AUTO_CREATE);

        HandleSMSResponsesTask handleSMSResponsesTask = new HandleSMSResponsesTask(this);
        handleSMSResponsesTask.execute();

        sendMessagesToWear();

    }

    private void sendMessagesToWear(){
        application.sendMessage("MessageAddress", prefs.getString(Constants.PREFS_KEY_SELECTED_ADDRESS, null));

        if(wallet != null) {
            String currencyValue = WalletUtils.getWalletCurrencyValue(getApplicationContext(),
                    prefs, wallet.getBalance(Wallet.BalanceType.ESTIMATED));

            application.sendMessage("MessageBalance", currencyValue);

            String exchangeRate =  WalletUtils.getExchangeRateWithSymbol(this, prefs);
            if(exchangeRate != null)
                application.sendMessage("MessageBitcoinValue", exchangeRate);
        }
    }

    private void handlePendingTransactions(){

        SharedPreferences smsPrefs = application.getSharedPreferences(getString(R.string.sms_transaction_filename), Context.MODE_PRIVATE);

        if(pendingTransactions != null)
            pendingTransactions.clear();

        if(pendingTransactionListAdapter != null)
            pendingTransactionListAdapter.notifyDataSetInvalidated();

        pendingTransactions = BasicUtils.getAllPendingTransactions(smsPrefs);

        if (pendingTransactions.size() > 0) {
            pendingTransactionListView = (ListView) findViewById(R.id.pending_transaction_list);
            pendingTransactionListView.setVisibility(View.VISIBLE);
            pendingTransactionListAdapter = new PendingTransactionListAdapter(
                    this,
                    R.layout.pending_transaction_detail_row,
                    pendingTransactions);

            pendingTransactionListView.setAdapter(pendingTransactionListAdapter);
            pendingTransactionListAdapter.notifyDataSetChanged();
        }
    }

    private void checkIfAppInitiated() {
        if (!prefs.getBoolean(Constants.APP_INIT_COMPLETE, false)) {
            Intent openMainActivity = new Intent(this, InitAppAction.class);
            openMainActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(openMainActivity);
        }
    }

    private void doDrawerSetup() {
        mTitle = mDrawerTitle = getTitle();

        drawerOptions = getResources().getStringArray(R.array.drawer_items);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        if (wallet.isEncrypted()) {
            drawerOptions[3] = getString(R.string.drawer_wallet_decrypt);
        } else {
            drawerOptions[3] = getString(R.string.drawer_wallet_encrypt);
        }
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        arrayAdapter = new ArrayAdapter<String>(this, R.layout.drawer_list_item, drawerOptions);
        mDrawerList.setAdapter(arrayAdapter);

        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {

                switch (position) {
                    case 0:
                        mDrawerLayout.closeDrawer(mDrawerList);
                        launchShowTransactionsActivity();
                        break;
                    //show addresses
                    case 1:
                        mDrawerLayout.closeDrawer(mDrawerList);
                        launchShowAddressesActivity();
                        break;
                    //show currencies
                    case 2:
                        mDrawerLayout.closeDrawer(mDrawerList);
                        launchCurrencyActivity();
                        break;
                    case 3:
                        mDrawerLayout.closeDrawer(mDrawerList);
                        if (wallet.isEncrypted()) {
                            //do decrypt wallet
                            if (application.getKeyCache() != null) {
                                DecryptWalletTask decryptWalletTask = new DecryptWalletTask(context, wallet, null, application);
                                decryptWalletTask.execute();
                            }
                            //Means x2 is written to the tag.
                            else if (!prefs.contains(Constants.SHAMIR_ENCRYPTED_KEY)) {
                                Intent decryptIntent = new Intent(context, NFCActivity.class);
                                decryptIntent.putExtra("nfc_action", "decrypt");
                                startActivity(decryptIntent);
                            } else {
                                application.showPasswordPrompt(context, Constants.ACTION_DECRYPT);
                            }
                        } else {
                            if (nfcEnabled) {
                                Intent decryptIntent = new Intent(context, NFCActivity.class);
                                decryptIntent.putExtra("nfc_action", "encrypt");
                                startActivity(decryptIntent);
                            } else {
                                application.showPasswordPrompt(context, Constants.ACTION_ENCRYPT);
                            }
                        }
                        break;
                    //If doing backup.
                    case 4:
                        mDrawerLayout.closeDrawer(mDrawerList);

                        if (nfcEnabled) {
                            Intent intent = new Intent(context, NFCActivity.class);
                            intent.putExtra("nfc_action", "backup");
                            startActivity(intent);
                        } else
                            application.showPasswordPrompt(context, Constants.ACTION_BACKUP);

                        break;
                    case 5:
                        mDrawerLayout.closeDrawer(mDrawerList);
                        initiateSettingsActivity();
                        break;

                    default:
                        break;
                }
            }
        });

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                R.drawable.ic_drawer,  /* nav drawer image to replace 'Up' caret */
                R.string.drawer_open,  /* "open drawer" description for accessibility */
                R.string.drawer_close  /* "close drawer" description for accessibility */
        ) {
            public void onDrawerClosed(View view) {
                setTitle(mTitle);
                //getActionBar().setTitle(mTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
                //getActionBar().setTitle(mDrawerTitle);
                setTitle(mDrawerTitle);

                if (wallet.isEncrypted()) {
                    drawerOptions[3] = getString(R.string.drawer_wallet_decrypt);
                } else {

                    drawerOptions[3] = getString(R.string.drawer_wallet_encrypt);
                }
                arrayAdapter.notifyDataSetChanged();
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        mDrawerList.setItemChecked(0, true);
        setTitle(drawerOptions[0]);
        mDrawerLayout.closeDrawer(mDrawerList);


    }

    @Override
    public void setTitle(CharSequence title) {
        SpannableString s = new SpannableString(title);

        s.setSpan(new CustomTypefaceSpan(this, "regular.otf"), 0, s.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        //getActionBar().setTitle(s);
        mTitle = s;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
        //menu.findItem(R.id.action_websearch).setVisible(!drawerOpen);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    private void setupBlockchainBroadcastReceiver(final TextView blockchainStatus) {
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int download = intent.getIntExtra(PeerBlockchainService.ACTION_BLOCKCHAIN_STATE_DOWNLOAD, PeerBlockchainService.ACTION_BLOCKCHAIN_STATE_DOWNLOAD_OK);
                Date bestChainDate = (Date) intent.getSerializableExtra(PeerBlockchainService.ACTION_BLOCKCHAIN_STATE_BEST_CHAIN_DATE);

                long blockchainLag = System.currentTimeMillis() - bestChainDate.getTime();
                boolean blockchainUptodate = blockchainLag < Constants.BLOCKCHAIN_UPTODATE_THRESHOLD_MS;
                boolean downloadOk = download == PeerBlockchainService.ACTION_BLOCKCHAIN_STATE_DOWNLOAD_OK;

                String downloading = downloadOk ? getString(R.string.synchronizing_network)
                        : getString(R.string.sync_stalled);

                Date currentDate = new Date();
                long daysOutOfDate = TimeUnit.MILLISECONDS.toDays(currentDate.getTime() - bestChainDate.getTime()) + 1;

                if (!blockchainUptodate) {
                    blockchainStatus.setText(downloading + " " + daysOutOfDate + " " + getString(R.string.sync_days_behind));
                    blockchainStatus.setTextColor(getResources().getColor(R.color.custom_red));
                } else {
                    blockchainStatus.setText(getString(R.string.sync_completed));
                    blockchainStatus.setTextColor(getResources().getColor(R.color.custom_green));
                }

                updateMainViews();
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();

        mDrawerLayout.closeDrawer(Gravity.LEFT);

        checkIfAppInitiated();

        nfcEnabled = prefs.contains(Constants.SHAMIR_ENCRYPTED_KEY) ? false : true;

        updateMainViews();
        doDrawerSetup();
        registerReceiver(receiver, new IntentFilter(PeerBlockchainService.ACTION_BLOCKCHAIN_STATE));
        checkBackupDone();


        String message = getIntent().getStringExtra("message");
        if (message != null) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            getIntent().removeExtra("message");
        }

        isServiceBound = this.bindService(new Intent(this, PeerBlockchainService.class), blockchainServiceConnection, Context.BIND_AUTO_CREATE);
        handlePendingTransactions();
        sendMessagesToWear();
        doBackupReminder();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doServiceUnbind();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.finish();
    }

    private void checkBackupDone() {
        boolean backupDone = getIntent().getBooleanExtra("backup_done", false);
        if (backupDone) {
            String fileName = getIntent().getStringExtra("backup_file");
            if (fileName != null) {
                CustomSharer sharer = new CustomSharer(this, fileName);
                sharer.showCustomChooser();
                getIntent().removeExtra("backup_done");
                getIntent().removeExtra("backup_file");
            }

        }
    }

    @Override
    protected void onPause() {
        unregisterReceiver(receiver);
        doServiceUnbind();
        super.onPause();
    }

    private void doServiceUnbind() {
        if (blockchainServiceConnection != null && isServiceBound) {
            unbindService(blockchainServiceConnection);
            isServiceBound = false;
        }
    }

    private void updateMainViews() {
        walletBalanceView.setText(BasicUtils.satoshiToBTC(wallet.getBalance(Wallet.BalanceType.ESTIMATED)));
        balanceInCurrencyView = (TextView) findViewById(R.id.wallet_balance_in_currency);
        balanceInCurrencyView.setTypeface(BasicUtils.getCustomTypeFace(getBaseContext()));

        String currencyValue = WalletUtils.getWalletCurrencyValue(getApplicationContext(),
                prefs, wallet.getBalance(Wallet.BalanceType.ESTIMATED));

        getBalanceInCurrencyViewType = (TextView) findViewById(R.id.wallet_balance_currency_type);
        getBalanceInCurrencyViewType.setTypeface(BasicUtils.getCustomTypeFace(getBaseContext()));


        if (currencyValue.length() >= 10) {
            balanceInCurrencyView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32);
            getBalanceInCurrencyViewType.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        } else if (currencyValue.length() >= 6) {
            balanceInCurrencyView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 40);
            getBalanceInCurrencyViewType.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        }

        balanceInCurrencyView.setText(currencyValue);
        getBalanceInCurrencyViewType.setText(" " + prefs.getString(Constants.CURRENCY_PREF_KEY, null));

        BigInteger watchedBalance = wallet.getWatchedBalance();

        if (watchedBalance != null && watchedBalance.longValue() > 0) {
            watchedBalanceLinearLayout.setVisibility(View.VISIBLE);
            walletWatchAddressBalanceView.setText(getString(R.string.watched_balance_string) + " " +
                    BasicUtils.satoshiToBTC(wallet.getWatchedBalance()));
        } else {
            watchedBalanceLinearLayout.setVisibility(View.GONE);
        }

        checkWalletEncryptionStatus();

        recentTransactions = (ArrayList<Transaction>) wallet.getRecentTransactions(50, true);

        if (transactionListAdapter != null) {
            transactionListAdapter.clear();
            transactionListAdapter.notifyDataSetInvalidated();
            //transactionListAdapter.addAll(WalletUtils.getRelevantTransactions(recentTransactions, wallet));
            transactionListAdapter.addAll(recentTransactions);
            transactionListAdapter.notifyDataSetChanged();
        }

        //Update the last view update time here.
        lastViewUpdateTime.set(System.currentTimeMillis());
        sendMessagesToWear();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (mDrawerLayout != null) {
                if (mDrawerLayout.isDrawerOpen(Gravity.LEFT))
                    mDrawerLayout.closeDrawer(Gravity.LEFT);
                else
                    mDrawerLayout.openDrawer(Gravity.LEFT);
            }
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    private void checkWalletEncryptionStatus() {
        if (mainEncryptionButton == null)
            mainEncryptionButton = (Button) findViewById(R.id.wallet_main_encryption_button);

        if (wallet.isEncrypted()) {
            mainEncryptionButton.setText(R.string.wallet_encrypted_button_string);
            mainEncryptionButton.setTextColor(getResources().getColor(R.color.custom_green));
            mainEncryptionButton.setEnabled(false);
        } else {
            mainEncryptionButton.setText(R.string.wallet_not_encrypted_button_string);
            mainEncryptionButton.setTextColor(getResources().getColor(R.color.custom_red));
            mainEncryptionButton.setEnabled(true);
        }
    }

    private void initiateHandlers(final TextView wallet_balance) {
        balanceHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {

                final long now = System.currentTimeMillis();
                boolean shouldNotify = false;

                //If the last view update time is less than one second ago we don't do anything.
                //This is to stop the app from crashing.
                if (now - lastViewUpdateTime.get() < 1500) {
                    shouldNotify = false;
                } else
                    shouldNotify = true;

                Bundle data = msg.getData();

                int status = data.getInt("status");
                switch (status) {
                    case Constants.WALLET_UPDATE_COINS_RECEIVED:
                        Double amountReceived = data.getDouble("amount");
                        notifyCoinsReceivedOrSent(amountReceived, true);
                        if (shouldNotify) {
                            updateMainViews();
                        }
                        break;
                    case Constants.WALLET_UPDATE_COINS_SENT:
                        Double amountSent = data.getDouble("amount");
                        notifyCoinsReceivedOrSent(amountSent, false);
                        if (shouldNotify)
                            updateMainViews();
                        break;
                    case Constants.WALLET_UPDATE_REORGANIZED:
                        //Do something upon reorg?
                        //updateMainViews();
                        if (shouldNotify)
                            updateMainViews();
                        break;
                    case Constants.WALLET_UPDATE_TRANS_CONFIDENCE:
                        if (shouldNotify) {
                            updateMainViews();
                        }
                        break;
                    case Constants.WALLET_UPDATE_CHANGED:
                        //Do something upon change?
                        if (shouldNotify) {
                            checkWalletEncryptionStatus();
                            updateMainViews();
                        }
                        break;
                    case Constants.WALLET_UPDATE_KEYS_ADDED:
                        if (shouldNotify) {
                            updateMainViews();
                        }
                        break;
                    default:
                        break;
                }
            }
        };
    }

    private void handleButtons() {
        requestBTCButton = (ImageButton) findViewById(R.id.request_coins_button);
        requestBTCButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchRequestActivity();
            }
        });

        sendBTCButton = (ImageButton) findViewById(R.id.send_coins_button);
        sendBTCButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchScanActivity();
            }
        });

        mainEncryptionButton = (Button) findViewById(R.id.wallet_main_encryption_button);
        mainEncryptionButton.setTypeface(BasicUtils.getCustomTypeFace(getBaseContext()));
        if (wallet.isEncrypted())
            mainEncryptionButton.setText(getString(R.string.wallet_encrypted_button_string));
        else
            mainEncryptionButton.setText(getString(R.string.wallet_not_encrypted_button_string));

        mainEncryptionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!wallet.isEncrypted()) {
                    if (nfcEnabled) {
                        Intent decryptIntent = new Intent(context, NFCActivity.class);
                        decryptIntent.putExtra("nfc_action", "encrypt");
                        startActivity(decryptIntent);
                    } else {
                        application.showPasswordPrompt(context, Constants.ACTION_ENCRYPT);
                    }
                }

            }
        });
    }

    private void initiateSettingsActivity() {
        Intent settingsIntent = new Intent(this, SettingsActivity.class);
        startActivity(settingsIntent);
    }

    private void launchCurrencyActivity() {
        Intent viewCurrencyIntent = new Intent(this, CurrencyActivity.class);
        startActivity(viewCurrencyIntent);
    }

    private void launchShowAddressesActivity() {
        Intent showAddressesIntent = new Intent(this, ShowAddressesActivity.class);
        startActivity(showAddressesIntent);
    }

    private void launchShowTransactionsActivity() {
        Intent showTransactionListIntent = new Intent(this, ShowTransactionsActivity.class);
        startActivity(showTransactionListIntent);
    }

    private void launchRequestActivity() {
        Intent requestIntent = new Intent(this, RequestBitcoinActivity.class);
        startActivity(requestIntent);
    }

    private void launchScanActivity() {
        Intent sendIntent = new Intent(this, AddressScanActivity.class);
        startActivity(sendIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }

    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);


            return rootView;
        }
    }

    public Address determineSelectedAddress() {
        final String selectedAddress = prefs.getString(Constants.PREFS_KEY_SELECTED_ADDRESS, null);

        Address firstAddress = null;
        for (final ECKey key : wallet.getKeys()) {
            if (!wallet.isKeyRotating(key)) {
                final Address address = key.toAddress(Constants.NETWORK_PARAMETERS);

                if (address.toString().equals(selectedAddress)) {
                    return address;
                }

                if (firstAddress == null) {
                    firstAddress = address;
                }
            }
        }
        prefs.edit().putString(Constants.PREFS_KEY_SELECTED_ADDRESS, firstAddress.toString()).commit();
        return firstAddress;
    }

    private void encryptWallet(String password) {
        EncryptWalletTask encryptWalletTask = new EncryptWalletTask(context, wallet, password, application, false);
        encryptWalletTask.execute();
    }

    @Override
    public void onPasswordProvided(String password, int action) {
        if (action == Constants.ACTION_ENCRYPT) {
            encryptWallet(password);
        } else if (action == Constants.ACTION_DECRYPT) {
            DecryptWalletTask decryptWalletTask = new DecryptWalletTask(context, wallet, password, application);
            decryptWalletTask.execute();
        } else if (action == Constants.ACTION_BACKUP) {
            BackupWalletTask backupWalletTask = new BackupWalletTask(application, context, wallet, password);
            backupWalletTask.execute();
        }
    }

    @Override
    public void onWalletEncrypted() {
        updateMainViews();
    }

    @Override
    public void onWalletDecrypted(String password) {
        updateMainViews();
    }

    @Override
    public void onBackupCompleted(String filePath) {
        String fileName = filePath;

        CustomSharer sharer = new CustomSharer(this, fileName);
        sharer.showCustomChooser();
    }

    @Override
    public void onImportCompleted(String fileName, List<ECKey> keyList) {
        //Will NOT return a file name if the wallet was unencrypted.
        if (!wallet.isEncrypted()) {
            application.showImportCompletedPrompt(context, fileName);
        }
    }

    public void onClick(View v) {
    }

    public void notifyCoinsReceivedOrSent(Double amount, boolean received) {
        final NotificationCompat.Builder notification = new NotificationCompat.Builder(MainActivity.this);

        String eventTitle = "";
        String eventLocation = "";
        String eventDescription = "";
        int wearIcon = 0;

        BigDecimal decimal = BigDecimal.valueOf(amount);
        BigInteger amountBigInt = decimal.toBigInteger();

        if (received) {
            notification.setSmallIcon(R.drawable.aegis_receive_icon);
            notification.setContentText(getString(R.string.received_string) + " " + BasicUtils.satoshiToBTC(amountBigInt) + getString(R.string.btc_string));

            eventTitle = "Aegis Receive";
            eventLocation = "Aegis Wallet";
            eventDescription = getString(R.string.received_string) + " " + BasicUtils.satoshiToBTC(amountBigInt) + getString(R.string.btc_string);
            wearIcon = R.drawable.aegis_receive_icon;

        } else if (!received) {
            notification.setSmallIcon(R.drawable.aegis_send_icon);
            notification.setContentText(getString(R.string.sent_string) + " " + BasicUtils.satoshiToBTC(amountBigInt) + getString(R.string.btc_string));

            eventTitle = "Aegis Send";
            eventLocation = "Aegis Wallet";
            eventDescription = getString(R.string.sent_string) + " " + BasicUtils.satoshiToBTC(amountBigInt) + getString(R.string.btc_string);
            wearIcon = R.drawable.aegis_send_icon;
        }
        notification.setContentTitle(getString(R.string.app_name));

        PendingIntent pendingIntent = PendingIntent.getActivity(MainActivity.this, 0, new Intent(MainActivity.this,
                MainActivity.class), 0);

        notification.setContentIntent(pendingIntent);
        notification.setOngoing(false);
        notificationManager.notify(1, notification.getNotification());


        NotificationCompat.BigTextStyle bigStyle = new NotificationCompat.BigTextStyle();
        bigStyle.bigText(eventDescription);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.icon)
                        .setLargeIcon(BitmapFactory.decodeResource(
                                getResources(), R.drawable.initpageicon))
                        .setContentTitle(eventTitle)
                        .setContentText(eventLocation)
                        .setContentIntent(pendingIntent)
                        .setStyle(bigStyle);

    }

    private void doBackupReminder() {

        if (System.currentTimeMillis() - application.lastReminderTime < 60000)
            return;

        String lastBackupString = prefs.getString(Constants.LAST_BACKUP_DATE, null);
        int lastBackupNumAddresses = prefs.getInt(Constants.LAST_BACKUP_NUM_ADDRESSES, 0);

        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.backup_reminder_prompt);

        TextView backupText = (TextView) dialog.findViewById(R.id.backup_reminder_prompt_text);

        Button cancelButton = (Button) dialog.findViewById(R.id.backup_reminder_prompt_cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        final Button okayButton = (Button) dialog.findViewById(R.id.backup_reminder_prompt_ok_button);

        okayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();

                if (nfcEnabled) {
                    Intent intent = new Intent(context, NFCActivity.class);
                    intent.putExtra("nfc_action", "backup");
                    startActivity(intent);
                } else
                    application.showPasswordPrompt(context, Constants.ACTION_BACKUP);
            }
        });

        try {

            if (dialog.isShowing())
                return;

            if (lastBackupString != null) {
                Date lastBackupDate = Constants.backupDateFormat.parse(lastBackupString);
                long currentDate = System.currentTimeMillis();
                long difference = currentDate - lastBackupDate.getTime();

                long days = TimeUnit.MILLISECONDS.toDays(difference);
                int keyChainSize = wallet.getKeychainSize();

                if (days > 6) {
                    dialog.show();
                    application.lastReminderTime = System.currentTimeMillis();
                } else if (!prefs.contains(Constants.LAST_BACKUP_NUM_ADDRESSES)) {
                    dialog.show();
                    application.lastReminderTime = System.currentTimeMillis();

                } else if (keyChainSize > lastBackupNumAddresses) {
                    backupText.setText(getString(R.string.backup_reminder_new_address));
                    dialog.show();
                    application.lastReminderTime = System.currentTimeMillis();
                }

            } else {
                application.lastReminderTime = System.currentTimeMillis();
                dialog.show();
            }

        } catch (ParseException e) {
            Log.d(TAG, e.getMessage());
        }
    }

    public void onSMSTaskCompleted(){
        handlePendingTransactions();
    }
}
