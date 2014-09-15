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

import android.telephony.SmsManager;

/**
 * Created by bsimic on 7/3/14.
 */
public class MessagingUtils {

    public static void sendSMS(String phoneNumber, String message) {
        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNumber, null, message, null, null);
    }

    /**
     *
     * @param ascci to be encoded to gsm7 bit
     * @return encoded ascci to gsm 7 bit
     */
    public static String encode(String ascci) {
        StringBuilder sb = new StringBuilder();
        int length = ascci.length();
        int gsm7Length = GSM7CHARS.length;
        for (int i = length; i > 0; i--) {

            char c = ascci.charAt((i - 1));
            for (int j = 0; j < gsm7Length; j++) {

                if ((char) GSM7CHARS[j] == c) {
                    int num = GSM7CHARS[j];
                    sb.append(makeSevenBits(Integer.toBinaryString(num)));
                }
            }
        }
        String encoded = from7BitBinaryToHexReversed(sb.toString());
        return encoded;
    }

    /**
     *
     * @param hex to be decoded
     * @return decoded gsm 7 bit to ascci
     */
    public static String decode(String hex) {
        StringBuilder binary = new StringBuilder();
        StringBuilder decoded = new StringBuilder();
        int length = hex.length();
        for (int i = length; i >= 2; i -= 2) {
            String twos = hex.substring((i - 2), i);//2 characters at a time on reverse direction
            binary.append(fromHexTo8BitBinary(twos));//Convert to 8 bit binary
        }
        int gsm7Length = GSM7CHARS.length;
        for (int i = binary.length(); i >= 7; i -= 7) {
            String seven = binary.substring((i - 7), i);//Chop into 7 bits binary in reverse direction
            int decimalOfSeven = Integer.parseInt(seven, 2);
            for (int j = 0; j < gsm7Length; j++) {
                if (GSM7CHARS[j] == decimalOfSeven) {
                    decoded.append("").append((char) GSM7CHARS[j]);//Do translation
                }
            }
        }
        return decoded.toString();
    }

    /**
     *
     * @param hex hex be converted to 8 bit binary
     * @return 8 bit binary
     */
    private static String fromHexTo8BitBinary(String hex) {
        StringBuilder ret = new StringBuilder();
        String binary = Integer.toBinaryString(Integer.parseInt(hex, 16));//Convert hex to binary
        int length = 8 - binary.length();
        for (int i = 0; i < length; i++) {
            ret.append("0");//Append missing 0's
        }
        ret.append(binary);
        return ret.toString();
    }

    /**
     *
     * @param binary to be converted to hex
     * @return hex string
     */
    private static String from7BitBinaryToHexReversed(String binary) {
        String ret = "";
        String temp = "";
        int length = binary.length();
        int rem = length % 8;
        int missingLenth = 8 - rem;
        StringBuilder zeros = new StringBuilder();
        for(int i = 0; i < missingLenth; i++) {
            zeros.append("0");
        }
        binary = zeros.toString() + binary;
        length = binary.length();
        for (int i = length; i >= 8; i -= 8) {//read in reverse direction
            temp = binary.substring((i - 8), i);//chop into 8 bits
            int val = Integer.parseInt(temp, 2);//get decimal value of binary
            String code = Integer.toHexString(val);
            if (code.length() < 2) {
                code = "0" + code;
            }
            ret += code;
        }

        return ret.toUpperCase();
    }

    /**
     *
     * @param binaryStr to be made 7 bits bianary
     * @return 7 bit binary
     */
    private static String makeSevenBits(String binaryStr) {
        String ret = "";
        StringBuilder zeros = new StringBuilder();
        int length = binaryStr.length();
        int appends = 7 - length;
        for (int i = 0; i < appends; i++) {
            zeros.append("0");//Append missing 0's to make 7 bits
        }
        ret = zeros + binaryStr;
        return ret;
    }

    /**
     * GSM 7 bit table
     */
     static final int[] GSM7CHARS = {
            0x0040, 0x00A3, 0x0024, 0x00A5, 0x00E8, 0x00E9, 0x00F9, 0x00EC,
            0x00F2, 0x00E7, 0x000A, 0x00D8, 0x00F8, 0x000D, 0x00C5, 0x00E5,
            0x0394, 0x005F, 0x03A6, 0x0393, 0x039B, 0x03A9, 0x03A0, 0x03A8,
            0x03A3, 0x0398, 0x039E, 0x00A0, 0x00C6, 0x00E6, 0x00DF, 0x00C9,
            0x0020, 0x0021, 0x0022, 0x0023, 0x00A4, 0x0025, 0x0026, 0x0027,
            0x0028, 0x0029, 0x002A, 0x002B, 0x002C, 0x002D, 0x002E, 0x002F,
            0x0030, 0x0031, 0x0032, 0x0033, 0x0034, 0x0035, 0x0036, 0x0037,
            0x0038, 0x0039, 0x003A, 0x003B, 0x003C, 0x003D, 0x003E, 0x003F,
            0x00A1, 0x0041, 0x0042, 0x0043, 0x0044, 0x0045, 0x0046, 0x0047,
            0x0048, 0x0049, 0x004A, 0x004B, 0x004C, 0x004D, 0x004E, 0x004F,
            0x0050, 0x0051, 0x0052, 0x0053, 0x0054, 0x0055, 0x0056, 0x0057,
            0x0058, 0x0059, 0x005A, 0x00C4, 0x00D6, 0x00D1, 0x00DC, 0x00A7,
            0x00BF, 0x0061, 0x0062, 0x0063, 0x0064, 0x0065, 0x0066, 0x0067,
            0x0068, 0x0069, 0x006A, 0x006B, 0x006C, 0x006D, 0x006E, 0x006F,
            0x0070, 0x0071, 0x0072, 0x0073, 0x0074, 0x0075, 0x0076, 0x0077,
            0x0078, 0x0079, 0x007A, 0x00E4, 0x00F6, 0x00F1, 0x00FC, 0x00E0,
            -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1,};

    static final int[] GSM7CHARS_DEFAULT = {
            -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, 0x000C, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, 0x005E, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1,
            0x007B, 0x007D, -1, -1, -1, -1, -1, 0x005C,
            -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, 0x005B, 0x007E, 0x005D, -1,
            0x007C, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, 0x20AC, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1,
    };
}
