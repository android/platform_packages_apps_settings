/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.development;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.Button;

/**
 * Foundation interface glues between Activities and UIs like {@link AdbWirelessDialog}.
 */
public interface AdbWirelessDialogUiBase {
    /**
     * Dialog shown when pairing a device via six-digit code.
     */
    int MODE_PAIRING = 0;
    /**
     *  Dialog shown when connecting to a paired device failed.
     */
    int MODE_CONNECTION_FAILED = 1;
    /**
     * Dialog shown when pairing failed.
     */
    int MODE_PAIRING_FAILED = 2;

    /**
     * Dialog shown when QR code pairing failed.
     */
    int MODE_QRCODE_FAILED = 3;

    /**
     * Dialog shown when adb wireless discovery failed to enable.
     */
    int MODE_DISCOVERY_FAILED = 4;

    public Context getContext();
    public AdbWirelessDialogController getController();
    public LayoutInflater getLayoutInflater();
    public int getMode();

    public void dismiss();
    public void dispatchSubmit();

    public void setCanceledOnTouchOutside(boolean cancel);

    public void setTitle(int id);
    public void setTitle(CharSequence title);

    public void setSubmitButton(CharSequence text);
    public void setCancelButton(CharSequence text);
    public Button getSubmitButton();
    public Button getCancelButton();
}
