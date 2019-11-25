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

package com.android.settings.network.telephony;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

class ImsMmTelBooleanState extends Semaphore implements Consumer<Boolean> {

    private static final String TAG = "ImsMmTelBooleanState";

    ImsMmTelBooleanState(boolean defaultValue) {
        super(0);
        mState = new AtomicBoolean(defaultValue);
    }

    private volatile AtomicBoolean mState;

    /**
     * Waits if necessary for the callback from IMS.
     *
     * @param state state reported from IMS
     */
    boolean get() throws Exception {
        acquire();
        return mState.get();
    }

    /**
     * Implementation of Consumer()
     *
     * @param state state reported from IMS
     */
    public void accept(Boolean state) {
        if (state != null) {
            mState.set(state);
        }
        release();
    }

}
