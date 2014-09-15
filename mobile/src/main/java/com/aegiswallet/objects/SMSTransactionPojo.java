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

package com.aegiswallet.objects;

import android.util.Base64;
import android.util.Log;

import com.aegiswallet.utils.BasicUtils;
import com.aegiswallet.utils.Constants;
import com.aegiswallet.utils.NfcUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by bsimic on 7/3/14.
 */
public class SMSTransactionPojo {

    private String TAG = this.getClass().getName();

    private String phoneNumber;
    private String name;
    private BigInteger amount;
    private String btcAddress;
    private long timestamp;
    private int status;
    private String tag;

    public SMSTransactionPojo(String phoneNumber, String name, BigInteger amount, String btcAddress, int status, String tag) {

        if (phoneNumber == null)
            phoneNumber = "";
        if (name == null)
            name = "";
        if (btcAddress == null)
            btcAddress = "";
        if(tag == null)
            tag = "";

        this.phoneNumber = phoneNumber;
        this.name = name;
        this.amount = amount;
        this.btcAddress = btcAddress;
        this.timestamp = Calendar.getInstance().getTimeInMillis();
        this.status = status;
        this.tag = tag;
    }

    public SMSTransactionPojo(String base64EncodedJSONString) {
        byte[] decoded = Base64.decode(base64EncodedJSONString.getBytes(), Base64.NO_WRAP);
        String jsonString = new String(decoded);

        try {
            JSONObject object = new JSONObject(jsonString);

            this.phoneNumber = object.getString("number");
            this.name = object.getString("name");
            this.amount = new BigInteger(object.getString("amount"));
            this.btcAddress = object.getString("address");
            this.timestamp = new Long(object.getString("timestamp")).longValue();
            this.status = new Integer(object.getString("status")).intValue();
            this.tag = object.getString("tag");

        } catch (JSONException e) {
            Log.d(TAG, e.getMessage());
        }
    }

    public String getPhoneNumber() {
        return this.phoneNumber;
    }

    public String getName() {
        return this.name;
    }

    public BigInteger getAmount(){ return this.amount; }

    public String getBtcAddress() {
        return this.btcAddress;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public int getStatus() {
        return this.status;
    }

    public String getTag(){ return this.tag; }

    public String toJSON() {

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("number", getPhoneNumber());
            jsonObject.put("name", getName());
            jsonObject.put("amount", getAmount());
            jsonObject.put("address", getBtcAddress());
            jsonObject.put("timestamp", getTimestamp());
            jsonObject.put("status", getStatus());
            jsonObject.put("tag", getTag());

            return jsonObject.toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return "";
        }

    }

    public String getJSONBase64() {
        byte[] encoded = Base64.encode(toJSON().getBytes(), Base64.NO_WRAP);
        return new String(encoded);
    }

    public void setBtcAddress(String address){
        this.btcAddress = address;
    }

    public void setStatus(int status){
        this.status = status;
    }
}
