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

import android.telephony.ims.ProvisioningManager;
import android.telephony.ims.feature.MmTelFeature;
import android.telephony.ims.stub.ImsRegistrationImplBase;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;


/**
 * An {@code ImsQuery} for accessing IMS provision stat
 */
public class ImsQueryProvisioningStat implements ImsQuery, Callable<Boolean> {

    public ImsQueryProvisioningStat(int subId,
        @MmTelFeature.MmTelCapabilities.MmTelCapability int capability,
        @ImsRegistrationImplBase.ImsRegistrationTech int tech) {
        mSubId = subId;
        mCapability = capability;
        mTech = tech;
    }

    private volatile int mSubId;
    private volatile int mCapability;
    private volatile int mTech;

    public Future<Boolean> query(ExecutorService executors) throws Throwable {
        return executors.submit(this);
    }

    public Boolean call() {
        final ProvisioningManager privisionManager =
                ProvisioningManager.createForSubscriptionId(mSubId);
        return privisionManager.getProvisioningStatusForCapability(mCapability, mTech);
    }
}
