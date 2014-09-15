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

import android.util.Log;

import com.aegiswallet.utils.Constants;
import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.DumpedPrivateKey;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.uri.BitcoinURI;
import com.google.bitcoin.uri.BitcoinURIParseException;

import java.math.BigInteger;

import javax.annotation.Nonnull;


public class PaymentProtocolHelper {

    private String TAG = this.getClass().getName();

    private String input;
    private Address address;
    private String addressLabel;
    private BigInteger amount;
    private BitcoinURI bitcoinUri;

    public PaymentProtocolHelper(@Nonnull String input) {

        input = input.replaceAll("\\s","");
        this.input = input;
        parse();
    }

    public void parse() {
        if (input.startsWith("bitcoin:")) {
            try {
                bitcoinUri = new BitcoinURI(null, input);
                address = bitcoinUri.getAddress();
                addressLabel = bitcoinUri.getLabel();
                amount = bitcoinUri.getAmount();

            } catch (final BitcoinURIParseException x) {
                Log.e(TAG, "Error parsing bitcoin URI. " + input);
            }
        } else if (Constants.PATTERN_BITCOIN_ADDRESS.matcher(input).matches()) {
            try {
                address = new Address(Constants.NETWORK_PARAMETERS, input);

            } catch (final AddressFormatException x) {
                Log.e(TAG, "invalid address");
            }
        } else if (Constants.PATTERN_PRIVATE_KEY.matcher(input).matches()) {
            try {
                final ECKey key = new DumpedPrivateKey(Constants.NETWORK_PARAMETERS, input).getKey();
                address = new Address(Constants.NETWORK_PARAMETERS, key.getPubKeyHash());
            } catch (final AddressFormatException x) {
                Log.e(TAG, "input parser invalid address since input matches private key");
            }
        }
        else {
            Log.e(TAG, "cannot classify error");
        }
    }

    public String getTAG() {
        return TAG;
    }

    public Address getAddress() {
        return address;
    }

    public String getAddressLabel() {
        return addressLabel;
    }

    public BigInteger getAmount() {
        return amount;
    }

    public BitcoinURI getBitcoinUri() {
        return bitcoinUri;
    }
}


