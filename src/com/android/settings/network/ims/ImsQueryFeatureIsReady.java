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

import android.telephony.ims.ImsMmTelManager;
import android.telephony.ims.feature.ImsFeature;

import com.google.common.util.concurrent.AbstractFuture;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Consumer;


/**
 * An {@code ImsQuery} for accessing IMS feature stat
 */
public class ImsQueryFeatureIsReady extends AbstractFuture<Boolean>
        implements ImsQuery, Consumer<Integer> {

    /**
     * Constructor
     * @param subId subscription id
     */
    public ImsQueryFeatureIsReady(int subId) {
        super();
        mSubId = subId;
    }

    private volatile int mSubId;

    /**
     * Implementation of interface {@code ImsQuery}
     *
     * @param executors {@code ExecutorService} which allows to submit {@code ImsQuery} when
     * required
     * @return result of query in format of {@code Future<Boolean>}
     */
    public Future<Boolean> query(ExecutorService executors) throws Throwable {
        final ImsMmTelManager imsMmTelManager = ImsMmTelManager.createForSubscriptionId(mSubId);
        imsMmTelManager.getFeatureState(executors, this);
        return this;
    }

    /**
     * Query running within a {@code Consumer}
     *
     * @param result result of query
     */
    public void accept(Integer result) {
        set((result != null) && (result == ImsFeature.STATE_READY));
    }
}
