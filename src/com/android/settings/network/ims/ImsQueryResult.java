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

import com.google.common.util.concurrent.AbstractFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;


public class ImsQueryResult implements AutoCloseable {

    private static final long TIMEOUT_QUERY_MS = 2000;
    private static final boolean ENABLE_MULTI_THREADS = true;

    public ImsQueryResult(ImsQuery ... queries) {
        this(TIMEOUT_QUERY_MS, queries);
    }

    public ImsQueryResult(long queryTimeoutInMs, ImsQuery ... queries) {
        this(queryTimeoutInMs, arrayToList(queries));
    }

    public ImsQueryResult(List<ImsQuery> queries) {
        this(TIMEOUT_QUERY_MS, queries);
    }

    public ImsQueryResult(long queryTimeoutInMs, List<ImsQuery> queries) {
        mQueries = queries;
        mTimeoutMs = queryTimeoutInMs;

        if ((!ENABLE_MULTI_THREADS) || (queries.size() <= 1)) {
            mExecutors = Executors.newSingleThreadExecutor();
        } else {
            mExecutors = Executors.newFixedThreadPool(queries.size());
        }
        mResults = new ArrayList<Future<Boolean>>(queries.size());

        for (ImsQuery query : queries) {
            mResults.add(getFuture(mExecutors, query));
        }
    }

    private static List<ImsQuery> arrayToList(ImsQuery ... queries) {
        final ArrayList<ImsQuery> queryList = new ArrayList<ImsQuery>(queries.length);
        for (int idxQuery=0; idxQuery<queries.length; idxQuery++) {
            queryList.add(queries[idxQuery]);
        }
        return queryList;
    }

    private ExecutorService mExecutors;
    private long mTimeoutMs;
    private List<ImsQuery> mQueries;
    private List<Future<Boolean>> mResults;

    private Future<Boolean> getFuture(ExecutorService executors, ImsQuery query) {
        Future<Boolean> result = null;
        try {
            result = query.query(executors);
            if (result == null) {
                result = new QueryException(new IllegalArgumentException());
            }
        } catch (Throwable exception) {
            result = new QueryException(exception);
        }
        return result;
    }

    private Future<Boolean> getResult(ImsQuery query) {
        final int idxQuery = mQueries.indexOf(query);
        if (idxQuery >= 0) {
            return mResults.get(idxQuery);
        }
        return new QueryException(new IllegalArgumentException());
    }

    public boolean get(ImsQuery query) {
        try {
            final Future<Boolean> result = getResult(query);
            return result.get(mTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (Exception exception) {
        }
        return false;
    }

    public void close() {
        try {
            mExecutors.shutdownNow();
        } catch (Exception exception) {
        }
    }

    public static class QueryException extends AbstractFuture<Boolean> {
        public QueryException(Throwable exception) {
            super();
            setException(exception);
        }
    }

    public static boolean andAll(ImsQuery ... queries) throws Exception {
        return andAll(TIMEOUT_QUERY_MS, queries);
    }

    public static boolean andAll(long queryTimeoutInMs, ImsQuery ... queries) throws Exception {
        try (ImsQueryResult request = new ImsQueryResult(queryTimeoutInMs, queries)) {
            for (ImsQuery query : queries) {
                if (!request.get(query)) {
                    return false;
                }
            }
        } catch (Exception exception) {
            throw exception;
        }
        return true;
    }
}
