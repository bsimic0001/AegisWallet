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

package com.aegiswallet.services;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.aegiswallet.PayBitsApplication;
import com.aegiswallet.R;
import com.aegiswallet.actions.MainActivity;
import com.aegiswallet.utils.Constants;
import com.aegiswallet.utils.WalletUtils;
import com.google.bitcoin.core.AbstractPeerEventListener;
import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.BlockChain;
import com.google.bitcoin.core.CheckpointManager;
import com.google.bitcoin.core.InsufficientMoneyException;
import com.google.bitcoin.core.Peer;
import com.google.bitcoin.core.PeerEventListener;
import com.google.bitcoin.core.PeerGroup;
import com.google.bitcoin.core.StoredBlock;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.crypto.KeyCrypterException;
import com.google.bitcoin.net.discovery.DnsDiscovery;
import com.google.bitcoin.net.discovery.PeerDiscovery;
import com.google.bitcoin.net.discovery.PeerDiscoveryException;
import com.google.bitcoin.store.BlockStore;
import com.google.bitcoin.store.BlockStoreException;
import com.google.bitcoin.store.SPVBlockStore;
import com.google.common.util.concurrent.ListenableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.CheckForNull;

/**
 * Created by bsimic on 2/13/14.
 */
public class PeerBlockchainService extends android.app.Service {

    private PayBitsApplication application;
    private SharedPreferences prefs;
    private NotificationManager nm;
    private File blockChainFile;
    private BlockStore blockStore;
    private BlockChain blockChain;

    @CheckForNull
    private PeerGroup peerGroup;

    private PeerConnectivityListener peerConnectivityListener;

    private final Handler handler = new Handler();

    public static final String PREFS_KEY_CONNECTIVITY_NOTIFICATION = "connectivity_notification";
    public static final String ACTION_PEER_STATE = R.class.getPackage().getName() + ".peer_state";
    public static final String ACTION_PEER_STATE_NUM_PEERS = "num_peers";

    private final Handler delayHandler = new Handler();
    private int bestChainHeightEver;
    private AtomicInteger transactionsReceived = new AtomicInteger();
    private int notificationCount = 0;
    private BigInteger notificationAccumulatedAmount = BigInteger.ZERO;
    private final List<Address> notificationAddresses = new LinkedList<Address>();
    private boolean resetBlockchainOnShutdown = false;

    public static final String ACTION_BLOCKCHAIN_STATE = R.class.getPackage().getName() + ".blockchain_state";
    public static final String ACTION_BLOCKCHAIN_STATE_BEST_CHAIN_DATE = "best_chain_date";
    public static final String ACTION_BLOCKCHAIN_STATE_BEST_CHAIN_HEIGHT = "best_chain_height";
    public static final String ACTION_BLOCKCHAIN_STATE_REPLAYING = "replaying";
    public static final String ACTION_BLOCKCHAIN_STATE_DOWNLOAD = "download";
    public static final int ACTION_BLOCKCHAIN_STATE_DOWNLOAD_OK = 0;
    public static final int ACTION_BLOCKCHAIN_STATE_DOWNLOAD_STORAGE_PROBLEM = 1;
    public static final int ACTION_BLOCKCHAIN_STATE_DOWNLOAD_NETWORK_PROBLEM = 2;
    private static final int IDLE_BLOCK_TIMEOUT_MIN = 2;
    private static final int IDLE_TRANSACTION_TIMEOUT_MIN = 9;
    private static final int MAX_HISTORY_SIZE = Math.max(IDLE_TRANSACTION_TIMEOUT_MIN, IDLE_BLOCK_TIMEOUT_MIN);
    private static final int MIN_COLLECT_HISTORY = 2;
    public static final String ACTION_CANCEL_COINS_RECEIVED = R.class.getPackage().getName() + ".cancel_coins_received";
    private static final int NOTIFICATION_ID_CONNECTED = 0;
    private static final int NOTIFICATION_ID_COINS_RECEIVED = 1;
    public static final String ACTION_RESET_BLOCKCHAIN = R.class.getPackage().getName() + ".reset_blockchain";
    public static final String ACTION_BROADCAST_TRANSACTION = R.class.getPackage().getName() + ".broadcast_transaction";

    private static final Logger log = LoggerFactory.getLogger(PeerBlockchainService.class);
    private final IBinder mBinder = new LocalBinder();

    private SharedPreferences tagPrefs;

    private static final String TAG = PeerBlockchainService.class.getName();

    @Override
    public void onCreate() {
        super.onCreate();

        nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        application = (PayBitsApplication) getApplication();
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final Wallet wallet = application.getWallet();

        blockChainFile = new File(getDir("blockstore", Context.MODE_PRIVATE), Constants.BLOCKCHAIN_FILENAME);

        final boolean blockChainFileExists = blockChainFile.exists();

        if (!blockChainFileExists) {
            wallet.clearTransactions(0);
            wallet.setLastBlockSeenHeight(-1);
            wallet.setLastBlockSeenHash(null);
        }

        try {
            blockStore = new SPVBlockStore(Constants.NETWORK_PARAMETERS, blockChainFile);
            blockStore.getChainHead(); // detect corruptions as early as possible

            long earliestKeyCreationTime = wallet.getEarliestKeyCreationTime();

            if (earliestKeyCreationTime == 0)
                earliestKeyCreationTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7);


            if (!blockChainFileExists && earliestKeyCreationTime > 0) {
                Log.d(TAG, "creating blockchain from checkpoint. attmpting to at least...");
                try {
                    final InputStream checkpointsInputStream = getAssets().open(Constants.CHECKPOINTS_FILENAME);
                    CheckpointManager.checkpoint(Constants.NETWORK_PARAMETERS, checkpointsInputStream, blockStore, earliestKeyCreationTime);
                } catch (final IOException x) {
                    Log.e(TAG, "problem reading checkpoint file..." + x.getMessage());
                }
            }

        } catch (final BlockStoreException x) {
            blockChainFile.delete();

            final String msg = "blockstore cannot be created";
            throw new Error(msg, x);
        }

        try {
            blockChain = new BlockChain(Constants.NETWORK_PARAMETERS, wallet, blockStore);
        } catch (final BlockStoreException x) {
            throw new Error("blockchain cannot be created", x);
        }

        bestChainHeightEver = prefs.getInt(Constants.PREFS_KEY_BEST_CHAIN_HEIGHT_EVER, 0);

        peerConnectivityListener = new PeerConnectivityListener();
        sendBroadcastPeerState(0);

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        intentFilter.addAction(Intent.ACTION_DEVICE_STORAGE_LOW);
        intentFilter.addAction(Intent.ACTION_DEVICE_STORAGE_OK);

        registerReceiver(connectivityReceiver, intentFilter);
        registerReceiver(tickReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
        maybeRotateKeys();

        tagPrefs = application.getSharedPreferences(
                getString(R.string.tag_pref_filename), Context.MODE_PRIVATE);

    }

    @Override
    public void onDestroy() {
        unregisterReceiver(tickReceiver);

        if (peerGroup != null) {
            peerGroup.removeEventListener(peerConnectivityListener);
            peerGroup.removeWallet(application.getWallet());
            peerGroup.stopAndWait();
        }

        peerConnectivityListener.stop();

        unregisterReceiver(connectivityReceiver);
        removeBroadcastPeerState();
        removeBroadcastBlockchainState();

        prefs.edit().putInt(Constants.PREFS_KEY_BEST_CHAIN_HEIGHT_EVER, bestChainHeightEver).commit();
        delayHandler.removeCallbacksAndMessages(null);

        try {
            blockStore.close();
        } catch (final BlockStoreException x) {
            throw new RuntimeException(x);
        }

        //Removing blockchain.
        if (resetBlockchainOnShutdown) {
            Log.d(TAG, "STOPPING SERVICE, DELETING BC");
            blockChainFile.delete();
        }

        super.onDestroy();
    }

    public class LocalBinder extends Binder {
        public PeerBlockchainService getService() {
            return PeerBlockchainService.this;
        }
    }

    @Override
    public IBinder onBind(final Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(final Intent intent) {
        return super.onUnbind(intent);
    }

    private final class PeerConnectivityListener extends AbstractPeerEventListener implements SharedPreferences.OnSharedPreferenceChangeListener {
        private int peerCount;
        private AtomicBoolean stopped = new AtomicBoolean(false);

        public PeerConnectivityListener() {
            prefs.registerOnSharedPreferenceChangeListener(this);
        }

        public void stop() {
            stopped.set(true);

            prefs.unregisterOnSharedPreferenceChangeListener(this);
            nm.cancel(NOTIFICATION_ID_CONNECTED);
        }

        @Override
        public void onPeerConnected(final Peer peer, final int peerCount) {
            this.peerCount = peerCount;
            changed(peerCount);
        }

        @Override
        public void onPeerDisconnected(final Peer peer, final int peerCount) {
            this.peerCount = peerCount;
            changed(peerCount);
        }

        @Override
        public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {
            if (PREFS_KEY_CONNECTIVITY_NOTIFICATION.equals(key))
                changed(peerCount);
        }

        private void changed(final int numPeers) {
            if (stopped.get())
                return;

            handler.post(new Runnable() {
                @Override
                public void run() {

                    if (numPeers == 0) {
                        nm.cancel(NOTIFICATION_ID_CONNECTED);
                    } else {
                        final NotificationCompat.Builder notification = new NotificationCompat.Builder(PeerBlockchainService.this);
                        notification.setSmallIcon(R.drawable.icon, numPeers > 4 ? 4 : numPeers);
                        notification.setContentTitle(getString(R.string.app_name));
                        notification.setContentText(getString(R.string.connected_to_string) + " " + numPeers + " " + getString(R.string.peers_string));

                        notification.setContentIntent(PendingIntent.getActivity(PeerBlockchainService.this, 0, new Intent(PeerBlockchainService.this,
                                MainActivity.class), 0));
                        notification.setOngoing(false);
                        nm.notify(NOTIFICATION_ID_CONNECTED, notification.getNotification());
                    }

                    // send broadcast
                    sendBroadcastPeerState(numPeers);
                }
            });
        }
    }

    private final BroadcastReceiver connectivityReceiver = new BroadcastReceiver() {
        private boolean hasConnectivity;
        private boolean hasStorage = true;

        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();

            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
                hasConnectivity = !intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
                log.info("network is " + (hasConnectivity ? "up" : "down"));

                check();
            } else if (Intent.ACTION_DEVICE_STORAGE_LOW.equals(action)) {
                hasStorage = false;
                check();
            } else if (Intent.ACTION_DEVICE_STORAGE_OK.equals(action)) {
                hasStorage = true;
                check();
            }
        }

        @SuppressLint("Wakelock")
        private void check() {
            final Wallet wallet = application.getWallet();
            final boolean hasEverything = hasConnectivity && hasStorage;

            if (hasEverything && peerGroup == null) {
                peerGroup = new PeerGroup(Constants.NETWORK_PARAMETERS, blockChain);
                peerGroup.addWallet(wallet);
                peerGroup.setUserAgent("Aegis Wallet", "1.0");
                peerGroup.addEventListener(peerConnectivityListener);

                final int maxConnectedPeers = 10;

                peerGroup.setMaxConnections(maxConnectedPeers);

                peerGroup.addPeerDiscovery(new PeerDiscovery() {
                    private final PeerDiscovery normalPeerDiscovery = new DnsDiscovery(Constants.NETWORK_PARAMETERS);

                    @Override
                    public InetSocketAddress[] getPeers(final long timeoutValue, final TimeUnit timeoutUnit) throws PeerDiscoveryException {
                        final List<InetSocketAddress> peers = new LinkedList<InetSocketAddress>();


                        boolean needsTrimPeersWorkaround = false;

                        //TODO: remove this...using for tesnet connection to peers issue
                        //InetSocketAddress customPeer = new InetSocketAddress("54.243.211.176",18333);
                        //peers.add(customPeer);
                        //End todo

                        peers.addAll(Arrays.asList(normalPeerDiscovery.getPeers(timeoutValue, timeoutUnit)));

                        if (needsTrimPeersWorkaround)
                            while (peers.size() >= maxConnectedPeers)
                                peers.remove(peers.size() - 1);

                        return peers.toArray(new InetSocketAddress[0]);
                    }

                    @Override
                    public void shutdown() {
                        normalPeerDiscovery.shutdown();
                    }
                });

                peerGroup.start();
                peerGroup.startBlockChainDownload(blockchainDownloadListener);
            } else if (!hasEverything && peerGroup != null) {
                peerGroup.removeEventListener(peerConnectivityListener);
                peerGroup.removeWallet(wallet);
                peerGroup.stop();
                peerGroup = null;
            }

            final int download = (hasConnectivity ? 0 : ACTION_BLOCKCHAIN_STATE_DOWNLOAD_NETWORK_PROBLEM)
                    | (hasStorage ? 0 : ACTION_BLOCKCHAIN_STATE_DOWNLOAD_STORAGE_PROBLEM);

            sendBroadcastBlockchainState(download);
        }
    };

    private final PeerEventListener blockchainDownloadListener = new AbstractPeerEventListener() {
        private final AtomicLong lastMessageTime = new AtomicLong(0);

        @Override
        public void onBlocksDownloaded(final Peer peer, final Block block, final int blocksLeft) {
            bestChainHeightEver = Math.max(bestChainHeightEver, blockChain.getChainHead().getHeight());

            delayHandler.removeCallbacksAndMessages(null);

            final long now = System.currentTimeMillis();

            if (now - lastMessageTime.get() > Constants.BLOCKCHAIN_STATE_BROADCAST_THROTTLE_MS)
                delayHandler.post(runnable);
            else
                delayHandler.postDelayed(runnable, Constants.BLOCKCHAIN_STATE_BROADCAST_THROTTLE_MS);
        }

        private final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                lastMessageTime.set(System.currentTimeMillis());
                sendBroadcastBlockchainState(ACTION_BLOCKCHAIN_STATE_DOWNLOAD_OK);
            }
        };
    };

    private void removeBroadcastPeerState() {
        removeStickyBroadcast(new Intent(ACTION_PEER_STATE));
    }

    private void removeBroadcastBlockchainState() {
        removeStickyBroadcast(new Intent(ACTION_BLOCKCHAIN_STATE));
    }

    private void sendBroadcastBlockchainState(final int download) {
        final StoredBlock chainHead = blockChain.getChainHead();

        final Intent broadcast = new Intent(ACTION_BLOCKCHAIN_STATE);
        broadcast.setPackage(getPackageName());
        broadcast.putExtra(ACTION_BLOCKCHAIN_STATE_BEST_CHAIN_DATE, chainHead.getHeader().getTime());
        broadcast.putExtra(ACTION_BLOCKCHAIN_STATE_BEST_CHAIN_HEIGHT, chainHead.getHeight());
        broadcast.putExtra(ACTION_BLOCKCHAIN_STATE_REPLAYING, chainHead.getHeight() < bestChainHeightEver);
        broadcast.putExtra(ACTION_BLOCKCHAIN_STATE_DOWNLOAD, download);

        sendStickyBroadcast(broadcast);
    }

    private final static class ActivityHistoryEntry {
        public final int numTransactionsReceived;
        public final int numBlocksDownloaded;

        public ActivityHistoryEntry(final int numTransactionsReceived, final int numBlocksDownloaded) {
            this.numTransactionsReceived = numTransactionsReceived;
            this.numBlocksDownloaded = numBlocksDownloaded;
        }

        @Override
        public String toString() {
            return numTransactionsReceived + "/" + numBlocksDownloaded;
        }
    }

    private final BroadcastReceiver tickReceiver = new BroadcastReceiver() {
        private int lastChainHeight = 0;
        private final List<ActivityHistoryEntry> activityHistory = new LinkedList<ActivityHistoryEntry>();

        @Override
        public void onReceive(final Context context, final Intent intent) {
            final int chainHeight = blockChain.getBestChainHeight();

            if (lastChainHeight > 0) {
                final int numBlocksDownloaded = chainHeight - lastChainHeight;
                final int numTransactionsReceived = transactionsReceived.getAndSet(0);

                // push history
                activityHistory.add(0, new ActivityHistoryEntry(numTransactionsReceived, numBlocksDownloaded));

                // trim
                while (activityHistory.size() > MAX_HISTORY_SIZE)
                    activityHistory.remove(activityHistory.size() - 1);

                // print
                final StringBuilder builder = new StringBuilder();
                for (final ActivityHistoryEntry entry : activityHistory) {
                    if (builder.length() > 0)
                        builder.append(", ");
                    builder.append(entry);
                }

                // determine if block and transaction activity is idling
                boolean isIdle = false;
                if (activityHistory.size() >= MIN_COLLECT_HISTORY) {
                    isIdle = true;
                    for (int i = 0; i < activityHistory.size(); i++) {
                        final ActivityHistoryEntry entry = activityHistory.get(i);
                        final boolean blocksActive = entry.numBlocksDownloaded > 0 && i <= IDLE_BLOCK_TIMEOUT_MIN;
                        final boolean transactionsActive = entry.numTransactionsReceived > 0 && i <= IDLE_TRANSACTION_TIMEOUT_MIN;

                        if (blocksActive || transactionsActive) {
                            isIdle = false;
                            break;
                        }
                    }
                }

                if (isIdle) {
                    stopSelf();
                }
            }

            lastChainHeight = chainHeight;
        }
    };

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        if(intent == null)
            return START_NOT_STICKY;

        final String action = intent.getAction();

        if (PeerBlockchainService.ACTION_CANCEL_COINS_RECEIVED.equals(action)) {
            notificationCount = 0;
            notificationAccumulatedAmount = BigInteger.ZERO;
            notificationAddresses.clear();

            nm.cancel(NOTIFICATION_ID_COINS_RECEIVED);
        } else if (PeerBlockchainService.ACTION_RESET_BLOCKCHAIN.equals(action)) {
            resetBlockchainOnShutdown = true;
            stopSelf();
        } else if (PeerBlockchainService.ACTION_BROADCAST_TRANSACTION.equals(action)) {
            String addressExtra = intent.getStringExtra("address");
            String amountExtra = intent.getStringExtra("amount");
            boolean justDecrypted = intent.getBooleanExtra("justDecrypted", false);
            String tagExtra = intent.getStringExtra("tagText");
            BigInteger amountBigInt = new BigInteger(amountExtra);
            final Wallet wallet = application.getWallet();

            try {
                Address address = new Address(Constants.NETWORK_PARAMETERS, addressExtra);
                Wallet.SendRequest sendRequest = Wallet.SendRequest.to(address, amountBigInt);

                //Adding the tag to the shared prefs
                tagPrefs = application.getSharedPreferences(
                        getString(R.string.tag_pref_filename), Context.MODE_PRIVATE);

                tagPrefs.edit().putString(sendRequest.tx.getHashAsString(), tagExtra).commit();

                sendRequest.ensureMinRequiredFee = false;
                Transaction transaction = wallet.sendCoinsOffline(sendRequest);

                if (transaction != null && peerGroup != null) {
                    ListenableFuture<Transaction> future = peerGroup.broadcastTransaction(transaction);
                    //TODO: Maybe doe something with future?
                }

            } catch (AddressFormatException e) {
                Log.e(TAG, "Address format exception " + e.getMessage());
            } catch (InsufficientMoneyException e) {
                Log.e(TAG, "Insufficient Money Exception " + e.getMessage());
            } catch (NullPointerException e) {
                Log.e(TAG, "null pointer exception: " + e.getMessage());
            }
            catch (IllegalArgumentException e){
                Log.e(TAG, "illegal argument exception: " + e.getMessage());
            }
            catch (IllegalStateException e){
                Log.e(TAG, "illegal state exception: " + e.getMessage());
            }
            catch (KeyCrypterException e){
                Log.e(TAG, "key crypter exception: " + e.getMessage());
            }
            finally {

                if (justDecrypted) {
                    if (application.getKeyCache() != null) {
                        wallet.encrypt(application.getKeyCache().getKeyCrypter(), application.getKeyCache().getAesKey());

                        String x2 = prefs.getString(Constants.SHAMIR_ENCRYPTED_KEY, null);

                        if (x2 != null) {
                            String encryptedX2 = WalletUtils.encryptString(x2, application.getKeyCache().getPassword());
                            prefs.edit().putString(Constants.SHAMIR_ENCRYPTED_KEY, encryptedX2).commit();
                        }

                    }
                }

            }
        }

        return START_NOT_STICKY;
    }

    public List<StoredBlock> getRecentBlocks(final int maxBlocks) {
        final List<StoredBlock> blocks = new ArrayList<StoredBlock>(maxBlocks);

        try {
            StoredBlock block = blockChain.getChainHead();

            while (block != null) {
                blocks.add(block);

                if (blocks.size() >= maxBlocks)
                    break;

                block = block.getPrev(blockStore);
            }
        } catch (final BlockStoreException x) {
            Log.i(TAG, x.getMessage());
            // swallow
        }

        return blocks;
    }

    private void maybeRotateKeys() {
        final Wallet wallet = application.getWallet();
        wallet.setKeyRotationEnabled(false);

        final StoredBlock chainHead = blockChain.getChainHead();

        new Thread() {
            @Override
            public void run() {
                final boolean replaying = chainHead.getHeight() < bestChainHeightEver; // checking again

                wallet.setKeyRotationEnabled(!replaying);
            }
        }.start();
    }

    private void sendBroadcastPeerState(final int numPeers) {
        final Intent broadcast = new Intent(ACTION_PEER_STATE);
        broadcast.setPackage(getPackageName());
        broadcast.putExtra(ACTION_PEER_STATE_NUM_PEERS, numPeers);
        sendStickyBroadcast(broadcast);
    }
}
