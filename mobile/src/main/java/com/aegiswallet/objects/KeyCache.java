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

import com.google.bitcoin.crypto.KeyCrypter;

import org.spongycastle.crypto.params.KeyParameter;

/**
 * Created by bsimic on 5/2/14.
 */
public class KeyCache {
    private KeyParameter aesKey;
    private KeyCrypter keyCrypter;
    private String password;

    public KeyCache(KeyParameter aesKey, KeyCrypter keyCrypter, String password){
        this.aesKey = aesKey;
        this.keyCrypter = keyCrypter;
        this.password = password;
    }

    public KeyParameter getAesKey(){
        return this.aesKey;
    }

    public KeyCrypter getKeyCrypter(){
        return this.keyCrypter;
    }

    public String getPassword() { return this.password; }
}
