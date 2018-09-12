package com.android.settings.intelligence.search;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.ArrayMap;
import android.util.Log;

import com.android.settings.intelligence.overlay.FeatureFactory;
import com.android.settings.intelligence.search.query.DatabaseResultTask;
import com.android.settings.intelligence.search.query.SearchQueryTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Collects the sorted list of all setting search results.
 */
public class SearchResultAggregator {

    private static final String TAG = "SearchResultAggregator";

    /**
     * Timeout for subsequent tasks to allow for fast returning tasks.
     * TODO(70164062): Tweak the timeout values.
     */
    private static final long SHORT_CHECK_TASK_TIMEOUT_MS = 600;

    private static SearchResultAggregator sResultAggregator;

    private SearchResultAggregator() {
    }

    public static SearchResultAggregator getInstance() {
        if (sResultAggregator == null) {
            sResultAggregator = new SearchResultAggregator();
        }

        return sResultAggregator;
    }

    @NonNull
    public synchronized List<? extends SearchResult> fetchResults(Context context, String query) {
        final SearchFeatureProvider mFeatureProvider = FeatureFactory.get(context)
                .searchFeatureProvider();
        final ExecutorService executorService = mFeatureProvider.getExecutorService();

        final List<SearchQueryTask> tasks =
                mFeatureProvider.getSearchQueryTasks(context, query);
        // Start tasks
        for (SearchQueryTask task : tasks) {
            executorService.execute(task);
        }

        // Collect results
        final Map<Integer, List<? extends SearchResult>> taskResults = new ArrayMap<>();
        final long allTasksStart = System.currentTimeMillis();
        for (SearchQueryTask task : tasks) {
            final int taskId = task.getTaskId();
            try {
                taskResults.put(taskId,
                        task.get(SHORT_CHECK_TASK_TIMEOUT_MS, TimeUnit.MILLISECONDS));
            } catch (TimeoutException | InterruptedException | ExecutionException e) {
                Log.d(TAG, "Could not retrieve result in time: " + taskId, e);
                taskResults.put(taskId, Collections.EMPTY_LIST);
            }
        }

        // Merge results
        final long mergeStartTime = System.currentTimeMillis();
        if (SearchFeatureProvider.DEBUG) {
            Log.d(TAG, "Total result loader time: " + (mergeStartTime - allTasksStart));
        }
        final List<? extends SearchResult> mergedResults = mergeSearchResults(taskResults);
        if (SearchFeatureProvider.DEBUG) {
            Log.d(TAG, "Total merge time: " + (System.currentTimeMillis() - mergeStartTime));
            Log.d(TAG, "Total aggregator time: " + (System.currentTimeMillis() - allTasksStart));
        }

        return mergedResults;
    }

    // TODO (b/68255021) scale the dynamic search results ranks
    private List<? extends SearchResult> mergeSearchResults(
            Map<Integer, List<? extends SearchResult>> taskResults) {

        final List<SearchResult> searchResults = new ArrayList<>();
        // First add db results as a special case
        searchResults.addAll(taskResults.remove(DatabaseResultTask.QUERY_WORKER_ID));

        // Merge the rest into result list: add everything to heap then pop them out one by one.
        final PriorityQueue<SearchResult> heap = new PriorityQueue<>();
        for (List<? extends SearchResult> taskResult : taskResults.values()) {
            heap.addAll(taskResult);
        }
        while (!heap.isEmpty()) {
            searchResults.add(heap.poll());
        }
        return searchResults;
    }
}
