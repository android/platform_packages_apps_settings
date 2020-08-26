/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.security;

import android.content.Context;
import android.os.UserManager;

import com.android.settings.core.BasePreferenceController;

public class InstallCaCertificatePreferenceController extends
        BasePreferenceController {

    private static final String KEY_INSTALL_CA_CERTIFICATE = "install_ca_certificate";

    public InstallCaCertificatePreferenceController(Context context) {
        super(context, UserManager.DISALLOW_CONFIG_CREDENTIALS);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_INSTALL_CA_CERTIFICATE;
    }
}
