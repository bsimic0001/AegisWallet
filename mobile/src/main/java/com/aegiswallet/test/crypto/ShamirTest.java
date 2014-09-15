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

package com.aegiswallet.test.crypto;

import android.test.InstrumentationTestCase;
import android.util.Log;

import com.aegiswallet.helpers.secretshare.SecretShare;
import com.aegiswallet.utils.WalletUtils;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by bsimic on 5/5/14.
 */

public class ShamirTest extends InstrumentationTestCase {

    private String TAG = this.getClass().getName();
    final int n = 3;
    final int k = 2;
    BigInteger prime = null;

    public void test() throws Exception {

        SecureRandom r = new SecureRandom();
        BigInteger secret = new BigInteger(256, r);

        prime = SecretShare.createAppropriateModulusForSecret(secret);
        SecretShare.PublicInfo publicInfo = new SecretShare.PublicInfo(n, k, prime, "test one");
        SecretShare secretShare = new SecretShare(publicInfo);

        SecretShare.SplitSecretOutput splitSecretOutput = secretShare.split(secret, r);
        BigInteger recon = subtestReconstruction(splitSecretOutput.getShareInfos());

        assertEquals(secret, recon);

    }

    public void test10Iterations() {

        for (int i = 0; i <= 10; i++) {
            SecureRandom r = new SecureRandom();
            BigInteger secret = new BigInteger(256, r);

            BigInteger primePad = new BigInteger(128, r);

            //BigInteger newPrime = primePad.add(secret);

            BigInteger newPrime = BigInteger.probablePrime(256 + 64, r);

            SecretShare.PublicInfo publicInfo = new SecretShare.PublicInfo(n, k, newPrime, "test " + i);
            SecretShare secretShare = new SecretShare(publicInfo);

            SecretShare.SplitSecretOutput splitSecretOutput = secretShare.split(secret, r);


            Log.d(TAG, i + " " + splitSecretOutput.debugDump());
            Log.d(TAG, "Solving... " + i);

            BigInteger recon = subtestReconstruction(splitSecretOutput.getShareInfos());
            assertEquals(secret, recon);

        }

    }

    public void testReconWithTwo() {
        SecureRandom r = new SecureRandom();
        BigInteger secret = new BigInteger(256, r);

        prime = SecretShare.createAppropriateModulusForSecret(secret);

        SecretShare.PublicInfo publicInfo = new SecretShare.PublicInfo(n, k, prime, "test one");
        SecretShare secretShare = new SecretShare(publicInfo);

        SecretShare.SplitSecretOutput splitSecretOutput = secretShare.split(secret, r);

        Log.d(TAG, splitSecretOutput.debugDump());

        Log.d(TAG, "Solving using shares...");

        //Removing 2 of the shares to show that they the result fails.
        List<SecretShare.ShareInfo> infos = splitSecretOutput.getShareInfos();

        List<SecretShare.ShareInfo> newShares = new ArrayList<SecretShare.ShareInfo>();

        SecretShare.PublicInfo publicInfo1 = new SecretShare.PublicInfo(3, 2, null, null);
        SecretShare.PublicInfo publicInfo2 = new SecretShare.PublicInfo(3, 2, null, null);

        SecretShare.ShareInfo shareInfoOne = new SecretShare.ShareInfo(1, infos.get(0).getShare(), publicInfo1);
        SecretShare.ShareInfo shareInfoThree = new SecretShare.ShareInfo(3, infos.get(2).getShare(), publicInfo2);

        newShares.add(shareInfoOne);
        newShares.add(shareInfoThree);

        BigInteger recon = subtestReconstruction(newShares);

        assertEquals(secret, recon);
    }

    public void testSecretRecovery() {
        String k1 = "1:7777330194881803291642355287239194720526881768387740038069905695006411010950";
        String k2 = "2:7777330194881803291642355287239194720571518620826621934932528030228401280839";
        String k3 = "3:7777330194881803291642355287239194720616155473265503831795150365450391550728";
        String original = "7777330194881803291642355287239194720482244915948858141207283359784420741061";

        BigInteger secret = WalletUtils.generateSecretFromStrings(k1, k2, k3);

        Log.d(TAG, "Secret is: " + secret);
        assertEquals(secret.toString(), original);
    }

    public void testSecretRecovery2Keys() {
        String k1 = "1:7777330194881803291642355287239194720526881768387740038069905695006411010950";
        String k3 = "3:7777330194881803291642355287239194720616155473265503831795150365450391550728";
        String original = "7777330194881803291642355287239194720482244915948858141207283359784420741061";

        BigInteger secret = WalletUtils.generateSecretFromStrings(k1, null, k3);

        Log.d(TAG, "Secret is: " + secret);
        assertEquals(secret.toString(), original);
    }

    public void testSecretRecovery2KeysAlternate() {
        String k1 = "1:7777330194881803291642355287239194720526881768387740038069905695006411010950";
        String k2 = "2:7777330194881803291642355287239194720571518620826621934932528030228401280839";
        String original = "7777330194881803291642355287239194720482244915948858141207283359784420741061";

        BigInteger secret = WalletUtils.generateSecretFromStrings(k1, k2, null);

        Log.d(TAG, "Secret is: " + secret);
        assertEquals(secret.toString(), original);
    }

    public void testSecretRecovery2KeysAlternateAgain() {
        String k2 = "2:7777330194881803291642355287239194720571518620826621934932528030228401280839";
        String k3 = "3:7777330194881803291642355287239194720616155473265503831795150365450391550728";
        String original = "7777330194881803291642355287239194720482244915948858141207283359784420741061";

        BigInteger secret = WalletUtils.generateSecretFromStrings(null, k2, k3);

        Log.d(TAG, "Secret is: " + secret);
        assertEquals(secret.toString(), original);
    }

    private BigInteger subtestReconstruction(List<SecretShare.ShareInfo> shares) {
        if (true) {
            subtestAllCombinations(shares);
        }

        // pick the first share's public info:
        SecretShare.PublicInfo publicInfo = shares.get(0).getPublicInfo();

        // create a new solver from just the public info:
        SecretShare solver = new SecretShare(publicInfo);

        // pick some of the shares
        List<SecretShare.ShareInfo> usetheseshares =
                new ArrayList<SecretShare.ShareInfo>();
        for (int i = 0, max = publicInfo.getK(); i < max; i++) {
            usetheseshares.add(shares.get(i));
        }

        SecretShare.CombineOutput solved = solver.combine(usetheseshares);
        return solved.getSecret();
    }

    private void subtestAllCombinations(List<SecretShare.ShareInfo> shares) {
        // pick the first share's public info:
        SecretShare.PublicInfo publicInfo = shares.get(0).getPublicInfo();

        // create a new solver from just the public info:
        SecretShare solver = new SecretShare(publicInfo);

        BigInteger secret = solver.combineParanoid(shares);
        assertNotNull(secret);
    }


}