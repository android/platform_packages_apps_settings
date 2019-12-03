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

package com.android.settings.network.ims;

import android.content.Context;
import android.telecom.TelecomManager;
import android.util.Log;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;


/**
 * An {@code ImsQuery} for accessing system TTY stat
 */
public class ImsQuerySystemTtyStat implements ImsQuery, Callable<Boolean> {

    public ImsQuerySystemTtyStat(Context context) {
        mContext = context;
    }

    private volatile Context mContext;

    public Future<Boolean> query(ExecutorService executors) throws Throwable {
        return executors.submit(this);
    }

    public Boolean call() {
        final TelecomManager telecomManager = mContext.getSystemService(TelecomManager.class);
        return (telecomManager.getCurrentTtyMode() != TelecomManager.TTY_MODE_OFF);
    }
}
