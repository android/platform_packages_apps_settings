/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.utils;

import android.os.Process;
import android.security.keystore.AndroidKeyStoreProvider;
import android.security.keystore2.AndroidKeyStoreLoadStoreParameter;
import android.util.Log;
import android.util.Pair;

import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;

public class AndroidKeystore {
    private final static String TAG = "SettingsKeystoreUtils";

    static public Pair<Collection<String>, Collection<String>> loadKeyAndCertificateAliases(
            Integer namespace) {
        List<String> keyAliases = new ArrayList<>();
        List<String> certAliases = new ArrayList<>();
        KeyStore keyStore = null;
        Enumeration<String> aliases = null;
        try {
            if (namespace != null) {
                if (AndroidKeyStoreProvider.isKeystore2Enabled()) {
                    keyStore = KeyStore.getInstance("AndroidKeyStore");
                    keyStore.load(new AndroidKeyStoreLoadStoreParameter(namespace));
                } else {
                    keyStore = AndroidKeyStoreProvider.getKeyStoreForUid(Process.WIFI_UID);
                }
            } else {
                keyStore = KeyStore.getInstance("AndroidKeyStore");
                keyStore.load(null);
            }
            aliases = keyStore.aliases();
        } catch (Exception e) {
            Log.e(TAG, "Failed to open and retrieve aliases from Android Keystore.", e);
            // Return empty lists.
            return new Pair(keyAliases, certAliases);
        }
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            try {
                Key key = keyStore.getKey(alias, null);
                if (key != null) {
                    if (key instanceof PublicKey) {
                        // If this is a public key entry it has both a key an a cert.
                        keyAliases.add(alias);
                        certAliases.add(alias);
                    }
                } else {
                    if (keyStore.getCertificate(alias) != null) {
                        certAliases.add(alias);
                    }
                }
            } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
                Log.e(TAG, "Failed to load alias: "
                        + alias + " from Android Keystore. Ignoring.", e);
            }
        }
        return new Pair(keyAliases, certAliases);
    }

}
