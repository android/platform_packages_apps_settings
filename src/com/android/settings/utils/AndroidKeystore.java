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
import android.security.keystore.KeyProperties;
import android.security.keystore2.AndroidKeyStoreLoadStoreParameter;
import android.util.Log;
import android.util.Pair;

import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;

/**
 * This class provides a portable and unified way to load the content of AndroidKeyStore through
 * public API.
 * @hide
 */
public class AndroidKeystore {
    private static final String TAG = "SettingsKeystoreUtils";

    /**
     * This function loads all aliases of asymmetric keys and certificates in the AndroidKeyStore
     * within the given namespace.
     * Viable namespaces are {@link KeyProperties#NAMESPACE_WIFI},
     * {@link KeyProperties#NAMESPACE_APPLICATION}, or null. The latter two are equivalent in
     * that they will load the keystore content of the app's own namespace. In case of settings,
     * this is the namespace of the AID_SYSTEM.
     *
     * @param namespace {@link KeyProperties#NAMESPACE_WIFI},
     *                  {@link KeyProperties#NAMESPACE_APPLICATION}, or null
     * @return A pair of collections of strings. The first collection comprises all aliases
     *         of private keys, the second comprises all aliases of public certificates.
     *         The first is a subset of the second, because each private key has a public
     *         certificate, but not every certificate entry necessarily has a private key
     *         associated with it.
     * @hide
     */
    public static Pair<Collection<String>, Collection<String>> loadKeyAndCertificateAliases(
            Integer namespace) {
        List<String> keyAliases = new ArrayList<>();
        List<String> certAliases = new ArrayList<>();
        KeyStore keyStore = null;
        Enumeration<String> aliases = null;
        try {
            if (namespace != null && namespace != KeyProperties.NAMESPACE_APPLICATION) {
                if (AndroidKeyStoreProvider.isKeystore2Enabled()) {
                    keyStore = KeyStore.getInstance("AndroidKeyStore");
                    keyStore.load(new AndroidKeyStoreLoadStoreParameter(namespace));
                } else {
                    // In the legacy case we pass in the WIFI UID because that is the only
                    // possible special namespace that existed as of this writing,
                    // and new namespaces must only be added using the new mechanism.
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
                    if (key instanceof PrivateKey) {
                        // If this is a private key entry it has both a key an a cert.
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
