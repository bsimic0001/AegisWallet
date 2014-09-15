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

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.aegiswallet.utils.Constants;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.core.WalletEventListener;
import com.google.bitcoin.script.Script;

import java.math.BigInteger;
import java.util.List;

/**
 * Created by bsimic on 3/12/14.
 */
public class WalletUpdateListener implements WalletEventListener {

    Handler handler;
    private int DELAY_TIME = 1000;

    public WalletUpdateListener(Handler handler){
        this.handler = handler;
    }

    @Override
    public void onCoinsReceived(Wallet wallet, Transaction transaction, BigInteger prevBalance, BigInteger newBalance) {

        Message message = new Message();
        Bundle data = new Bundle();

        data.putInt("status", Constants.WALLET_UPDATE_COINS_RECEIVED);
        data.putDouble("amount", newBalance.subtract(prevBalance).doubleValue());
        data.putDouble("balance", newBalance.doubleValue());
        data.putString("txHash", transaction.getHashAsString());
        message.setData(data);
        handler.sendMessageDelayed(message, DELAY_TIME);
    }

    @Override
    public void onCoinsSent(Wallet wallet, Transaction transaction, BigInteger prevBalance, BigInteger newBalance) {
        Message message = new Message();
        Bundle data = new Bundle();

        data.putInt("status", Constants.WALLET_UPDATE_COINS_SENT);
        data.putDouble("amount", prevBalance.subtract(newBalance).doubleValue());
        data.putDouble("balance", newBalance.doubleValue());
        data.putString("txHash", transaction.getHashAsString());
        message.setData(data);
        handler.sendMessageDelayed(message, DELAY_TIME);
    }

    @Override
    public void onReorganize(Wallet wallet) {
        //handler.sendEmptyMessage(Constants.WALLET_UPDATE_REORGANIZED);
    }

    @Override
    public void onTransactionConfidenceChanged(Wallet wallet, Transaction transaction) {
        Message message = new Message();
        Bundle data = new Bundle();

        data.putInt("status", Constants.WALLET_UPDATE_TRANS_CONFIDENCE);
        data.putString("confidenceString", transaction.getConfidence().toString());
        data.putString("confidenceType", transaction.getConfidence().getConfidenceType().toString());

        message.setData(data);
        handler.sendMessageDelayed(message, DELAY_TIME);
    }

    @Override
    public void onWalletChanged(Wallet wallet) {
        handler.sendEmptyMessageDelayed(DELAY_TIME, Constants.WALLET_UPDATE_CHANGED);
        //handler.sendEmptyMessage(Constants.WALLET_UPDATE_CHANGED);
    }

    @Override
    public void onKeysAdded(Wallet wallet, List<ECKey> ecKeys) {
        Message message = new Message();
        Bundle data = new Bundle();

        data.putInt("status", Constants.WALLET_UPDATE_KEYS_ADDED);
        data.putInt("numKeysAdded", ecKeys.size());
        message.setData(data);
        handler.sendMessageDelayed(message, DELAY_TIME);
    }

    @Override
    public void onScriptsAdded(Wallet wallet, List<Script> scripts) {

    }
}
