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

package com.aegiswallet.utils;

import android.os.Environment;
import android.text.format.DateUtils;

import com.google.bitcoin.core.Base58;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.params.TestNet3Params;

import java.io.File;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.regex.Pattern;

/**
 * Created by bsimic on 2/15/14.
 */
public class Constants {

    public static final Pattern PATTERN_BITCOIN_ADDRESS = Pattern.compile("[" + new String(Base58.ALPHABET) + "]{20,40}");
    public static final Pattern PATTERN_PRIVATE_KEY = Pattern.compile("5[" + new String(Base58.ALPHABET) + "]{50,51}");
    public static final Pattern PATTERN_TRANSACTION = Pattern.compile("[0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ$\\*\\+\\-\\.\\/\\:]{100,}");

    public static final String CURRENCY_PLUS_SIGN = "+ ";
    public static final String CURRENCY_MINUS_SIGN = "- ";

    public static final long MIN_AMOUNT_IN_SATOSHI = 10000;
    public static final BigInteger MIN_AMOUN_BIG_INT = BigInteger.valueOf(10000);
    public static final BigInteger MAX_DUST_ALLOWED = BigInteger.valueOf(5460);

    //public static final NetworkParameters NETWORK_PARAMETERS = TestNet3Params.get();
    public static final NetworkParameters NETWORK_PARAMETERS = MainNetParams.get();

    public static final String BLOCKCHAIN_FILENAME = "blockchain.dat";
    private static final String FILENAME_NETWORK_SUFFIX = NETWORK_PARAMETERS.getId().equals(NetworkParameters.ID_MAINNET) ? "" : "-testnet";

    public static final long BLOCKCHAIN_STATE_BROADCAST_THROTTLE_MS = DateUtils.SECOND_IN_MILLIS;
    public static final long BLOCKCHAIN_UPTODATE_THRESHOLD_MS = DateUtils.HOUR_IN_MILLIS;

    public static final String PREFS_KEY_BEST_CHAIN_HEIGHT_EVER = "best_chain_height_ever";
    public static final String CHECKPOINTS_FILENAME = "checkpoints" + FILENAME_NETWORK_SUFFIX;
    public static final String PREFS_KEY_SELECTED_ADDRESS = "selected_address";
    public static final String PREFS_KEY_REMIND_BACKUP = "remind_backup";
    public static final String WALLET_KEY_BACKUP_BASE58 = "key-backup-base58" + FILENAME_NETWORK_SUFFIX;
    public static final Charset UTF_8 = Charset.forName("UTF-8");
    public static final String WALLET_FILENAME_PROTOBUF = "wallet-protobuf" + FILENAME_NETWORK_SUFFIX;
    public static final String WALLET_FILENAME = "wallet" + FILENAME_NETWORK_SUFFIX;

    public static final int WALLET_UPDATE_COINS_RECEIVED = 1;
    public static final int WALLET_UPDATE_COINS_SENT = 2;
    public static final int WALLET_UPDATE_REORGANIZED = 3;
    public static final int WALLET_UPDATE_TRANS_CONFIDENCE = 4;
    public static final int WALLET_UPDATE_CHANGED = 5;
    public static final int WALLET_UPDATE_KEYS_ADDED = 6;

    public static final int WALLET_PASSWORD_MIN_LENGTH = 8;
    public static final int WALLET_PASSWORD_STRONG_LENGTH = 14;
    public static final int WALLET_PASSWORD_VERY_STRONG_LENGTH = 20;


    public static final int BTC_MAX_PRECISION = 8;
    public static final int BTC_MIN_PRECISION = 4;

    public static final String CURRENCY_PREF_KEY = "currency";
    public static final String BLOCKCHAIN_CURRENCY_FILE_NAME = "tickerfile.txt";
    public static final String BLOCKCHAIN_CURRENCY_CALL = "https://blockchain.info/ticker";
    public static final String BLOCKCHAIN_TX_URL = NETWORK_PARAMETERS.getId().equals(NetworkParameters.ID_MAINNET) ? "https://blockchain.info/tx/" : "http://blockexplorer.com/testnet/tx/";
    public static final String BLOCKCHAIN_ADDRESS_URL = NETWORK_PARAMETERS.getId().equals(NetworkParameters.ID_MAINNET) ? "https://blockchain.info/address/" : "http://blockexplorer.com/testnet/address/";

    public static final int ACTION_ENCRYPT = 1;
    public static final int ACTION_DECRYPT = 2;
    public static final int ACTION_BACKUP = 3;
    public static final int ACTION_RESTORE_BACKUP = 4;
    public static final int ACTION_SWITCH_TO_NFC = 5;
    public static final int ACTION_IMPORT_KEY = 6;

    public static final DateFormat backupDateFormat = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");
    public static final File WALLET_BACKUP_DIRECTORY = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    public static final String WALLET_BACKUP_FILENAME = "AegisWalletBackup" + FILENAME_NETWORK_SUFFIX;

    public static final int SHAMIR_N = 3;
    public static final int SHAMIR_K = 2;
    public static final String SHAMIR_LOCAL_KEY = "shamir_local_key";
    public static final String SHAMIR_ENCRYPTED_KEY = "shamir_encrypted_key";
    public static final String SHAMIR_EXPORTED_KEY = "shamir_exported_key";
    public static final String SHAMIR_EXPORTED_KEY_SHA256 = "shamir_exported_key_sha256";

    public static final String LAST_BACKUP_DATE = "last_backup_date";
    public static final String LAST_BACKUP_NUM_ADDRESSES = "last_backup_num_addresses";

    public static final String PASSWORD_HASH = "password_hash";
    public static final String PASSWORD_SALT = "password_salt";
    public static final String APP_INIT_COMPLETE = "app_init_complete";

    //The password minimum score - should be 3 or 4....
    public static final int MINIMUM_PASSWORD_SCORE = 3;

    public static final String NFC_VALUE = "com.aegis.wallet";
    public static final String NFC_TAG_FORMATTED = "tag_formatted";
    public static final String NFC_CANT_CONNECT = "cant_connect_nfc_adapter";
    public static final String NFC_READ_ONLY = "failed_read_only";
    public static final String NFC_TAG_TOO_SMALL = "too_small_tag";
    public static final String NFC_CANT_FORMAT = "cant_format";
    public static final String NFC_TAG_WRITTEN = "tag_written";
    public static final String NFC_WRITE_EXCEPTION = "io_exception_format";
    public static final String NFC_TAG_NOT_SUPPORTED = "tag_not_supported";

    public static final String SHAMIR_X2_HASHED = "shamir_x2_hashed";

    public static final String AEGIS_SITE = "https://bitcoinsecurityproject.org/api/aegis";

    public static final int SMS_STATUS_INIT = 0;
    public static final int SMS_STATUS_REC = 1;
    public static final int SMS_STATUS_COMP = 2;
}
