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
import android.telephony.ims.feature.MmTelFeature;
import android.telephony.ims.stub.ImsRegistrationImplBase;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;


/**
 * An {@code ImsQuery} for accessing IMS avilable stat per subscription basis
 */
public class ImsQueryAvailableStat implements ImsQuery, Callable<Boolean> {

    /**
     * Constructor
     * @param subId subscription id
     * @param capability {@code MmTelFeature.MmTelCapabilities.MmTelCapability}
     * @param tech {@code @ImsRegistrationImplBase.ImsRegistrationTech}
     */
    public ImsQueryAvailableStat(int subId,
        @MmTelFeature.MmTelCapabilities.MmTelCapability int capability,
        @ImsRegistrationImplBase.ImsRegistrationTech int tech) {
        mSubId = subId;
        mCapability = capability;
        mTech = tech;
    }

    private volatile int mSubId;
    private volatile int mCapability;
    private volatile int mTech;

    /**
     * Implementation of interface {@code ImsQuery}
     *
     * @param executors {@code ExecutorService} which allows to submit {@code ImsQuery} when
     * required
     * @return result of query in format of {@code Future<Boolean>}
     */
    public Future<Boolean> query(ExecutorService executors) throws Throwable {
        return executors.submit(this);
    }

    /**
     * Query running within a {@code Callable}
     *
     * @return result of query
     */
    public Boolean call() {
        final ImsMmTelManager imsMmTelManager = ImsMmTelManager.createForSubscriptionId(mSubId);
        return imsMmTelManager.isAvailable(mCapability, mTech);
    }
}
