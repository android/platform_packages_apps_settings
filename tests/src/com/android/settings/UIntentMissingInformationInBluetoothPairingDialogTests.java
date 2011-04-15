/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.settings;

import com.android.settings.bluetooth.BluetoothPairingDialog;

import android.content.Intent;
import android.test.ActivityUnitTestCase;

public class UIntentMissingInformationInBluetoothPairingDialogTests extends
        ActivityUnitTestCase<BluetoothPairingDialog> {

    public UIntentMissingInformationInBluetoothPairingDialogTests() {
        super(BluetoothPairingDialog.class);
    }

    public UIntentMissingInformationInBluetoothPairingDialogTests(
            Class<BluetoothPairingDialog> activityClass) {
        super(activityClass);
    }

    /**
     * Test sending an Intent missing action information to
     * BluetoothPairingDialog.
     */
    public void testIntentToBluetoothPairingDialog() throws Exception {
        BluetoothPairingDialog testedActivity = null;
        Intent intent = new Intent();
        intent.setClassName("com.android.settings.bluetooth", "BluetoothPairingDialog");
        intent.setAction(null);

        try {
            testedActivity = (BluetoothPairingDialog)startActivity(intent, null, null);
        } catch (Exception e) {
            fail("UIntentMissingInformationInBluetoothPairingDialogTests: " + e);
        }
    }
}
