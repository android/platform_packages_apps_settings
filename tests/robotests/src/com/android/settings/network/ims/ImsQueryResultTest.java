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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.common.util.concurrent.AbstractFuture;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;


@RunWith(AndroidJUnit4.class)
public class ImsQueryResultTest {

    private static final long QUERY_TIMEOUT_MS = 2000;

    private ImsQuery mQueryForTrue;
    private ImsQuery mQueryForFalse;
    private ImsQuery mQueryForException;
    private ImsQuery mQueryForRuntimeException;

    @Before
    public void setUp() {
        mQueryForTrue = new ImsQueryBoolean(true);
        mQueryForFalse = new ImsQueryBoolean(false);
        mQueryForException = new ImsQueryException(new IllegalArgumentException());
        mQueryForRuntimeException = new ImsQueryRuntimeException();
    }

    @Test
    public void constructor_oneRequest_shouldGetCorrectResult() {
        final ImsQueryResult resultTrue = new ImsQueryResult(QUERY_TIMEOUT_MS, mQueryForTrue);
        assertTrue(resultTrue.get(mQueryForTrue));
        resultTrue.close();

        final ImsQueryResult resultFalse = new ImsQueryResult(QUERY_TIMEOUT_MS, mQueryForFalse);
        assertFalse(resultFalse.get(mQueryForFalse));
        resultFalse.close();
    }

    @Test
    public void constructor_oneRequest_exceptionShouldGetFalseResult() {
        final ImsQueryResult resultException1 = new ImsQueryResult(QUERY_TIMEOUT_MS,
                mQueryForException);
        assertFalse(resultException1.get(mQueryForException));
        resultException1.close();

        final ImsQueryResult resultException2 = new ImsQueryResult(QUERY_TIMEOUT_MS,
                mQueryForRuntimeException);
        assertFalse(resultException2.get(mQueryForRuntimeException));
        resultException2.close();
    }

    @Test
    public void constructor_twoRequests_shouldGetCorrectResult() {
        final ImsQueryResult results = new ImsQueryResult(QUERY_TIMEOUT_MS,
                mQueryForTrue, mQueryForFalse);
        assertFalse(results.get(mQueryForFalse));
        assertTrue(results.get(mQueryForTrue));
        results.close();
    }

    @Test
    public void andAll_oneRequest_shouldGetCorrectResult() {
        boolean result = false;
        try {
            result = ImsQueryResult.andAll(mQueryForTrue);
        } catch (Exception exception) {
            fail();
        }
        assertTrue(result);

        try {
            result = ImsQueryResult.andAll(mQueryForFalse);
        } catch (Exception exception) {
            fail();
        }
        assertFalse(result);
    }

    @Test
    public void andAll_twoRequests_shouldGetCorrectResult() {
        boolean result = false;
        try {
            result = ImsQueryResult.andAll(mQueryForFalse, mQueryForTrue);
        } catch (Exception exception) {
            fail();
        }
        assertFalse(result);

        try {
            result = ImsQueryResult.andAll(mQueryForTrue, mQueryForFalse);
        } catch (Exception exception) {
            fail();
        }
        assertFalse(result);
    }

    public static class ImsQueryBoolean extends AbstractFuture<Boolean> implements ImsQuery {
        public ImsQueryBoolean(boolean result) {
            super();
            set(result);
        }

        public Future<Boolean> query(ExecutorService executors) throws Throwable {
            return this;
        }
    }

    public static class ImsQueryException extends AbstractFuture<Boolean> implements ImsQuery {
        public ImsQueryException(Throwable exception) {
            super();
            setException(exception);
        }

        public Future<Boolean> query(ExecutorService executors) throws Throwable {
            return this;
        }
    }

    public static class ImsQueryRuntimeException implements ImsQuery {
        private Object mObject;
        public Future<Boolean> query(ExecutorService executors) throws Throwable {
            mObject.toString();
            return null;
        }
    }

}
