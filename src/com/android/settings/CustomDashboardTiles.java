/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;

import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.dashboard.DashboardCategory;
import com.android.settings.dashboard.DashboardTile;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * This class allows adding items to the Dashboard Categories/Tiles by specifying them
 * in the overlay.
 */
public final class CustomDashboardTiles {
    private static final String TAG = CustomDashboardTiles.class.getSimpleName();
    private static final boolean DEBUG = false; // Must be false at merge

    private static final String EXTRA_AFTER_ID = "extra_after_id";
    private static final String EXTRA_DEPEND_ON_IS_VALID_INTENT = "extra_depend_on_is_valid_intent";

    /**
     * Load R.xml.{resourcesToAdd} and apply dependency and placement before
     * adding them to headers
     */
    public static void loadTilesFromResource(SettingsActivity settings, int resourcesToAdd,
            List<DashboardCategory> categories) {
        int resId = 0;
        Resources res = settings.getResources();
        String pkg = settings.getPackageName();
        ArrayList<DashboardCategory> additions = new ArrayList<DashboardCategory>();
        SettingsActivity.loadCategoriesFromResource(resourcesToAdd, additions, settings);

        // Iterate from end of additions/tiles in order to add tiles with the same
        // "after key" in the same order as specified
        while (!additions.isEmpty()) {
            DashboardCategory category = additions.remove(additions.size() - 1);
            List<DashboardTile> tiles = category.tiles;

            while (!tiles.isEmpty()) {
                DashboardTile tileToAdd = tiles.remove(tiles.size() - 1);
                Bundle bundle = tileToAdd.fragmentArguments;
                logBundle(bundle);
                if (bundle == null) {
                    // No placement information - just add anywhere
                    addTileAfterId(categories, -1, tileToAdd);
                    continue;
                }

                // Manage dependencies "To be or not to be"
                if (bundle.containsKey(EXTRA_DEPEND_ON_IS_VALID_INTENT) &&
                        bundle.getBoolean(EXTRA_DEPEND_ON_IS_VALID_INTENT)) {
                    // Skip if no matching activity
                    List<ResolveInfo> listQuery = settings.getPackageManager()
                            .queryIntentActivities(tileToAdd.intent, 0);
                    if (listQuery.isEmpty()) {
                        log("Not valid intent");
                        continue;
                    }
                }

                // Manage placement of tileToAdd
                if ((resId = getResource(res, bundle, EXTRA_AFTER_ID, "id", pkg)) != 0) {
                    addTileAfterId(categories, resId, tileToAdd);
                } else {
                    // No placement information - just add anywhere
                    addTileAfterId(categories, -1, tileToAdd);
                }
            }
        }
    }

    private static void addTileAfterId(List<DashboardCategory> categories, long id,
            DashboardTile tileToAdd) {
        if (id < 0) {
            // Special case for adding "anywhere" will add last in last category
            DashboardCategory cat = categories.get(categories.size() - 1);
            cat.addTile(tileToAdd);
            log("addTileAfterId | Added tile last in dashboard");
            return;
        }
        // Find the category or tile with correct id
        for (int i = 0, catSize = categories.size(); i < catSize; i++) {
            DashboardCategory cat = categories.get(i);
            if (cat.id == id) {
                // Add the tile as first item in this category
                cat.addTile(0, tileToAdd);
                log("addTileAfterId | Added tile at (" + i + ", 0)");
                return;
            }
            for (int j = 0, tilesCount = cat.getTilesCount(); j < tilesCount; j++) {
                DashboardTile tile = cat.getTile(j);
                if (tile.id == id) {
                    cat.addTile(j + 1, tileToAdd);
                    log("addTileAfterId | Added tile at (" + i + ", " + j + ")");
                    return;
                }
            }
        }
        log("addTileAfterId | Found no tile with id " + id);
    }

    /**
     * Convert string in bundle to a resource identifier matching type
     * ("id" or "string") and package ("com.android.settings")
     */
    private static int getResource(Resources res, Bundle bundle, String key, String type,
            String pkg) {
        String resName = bundle.getString(key);
        return (resName != null) ? res.getIdentifier(resName, type, pkg) : 0;
    }

    // Logging helper
    private static void logBundle(Bundle bundle) {
        log("Bundle contents:");
        if (bundle == null) return;
        Set<String> keys = bundle.keySet();
        for (String key : keys) {
            Object value = bundle.get(key);
            log("    (key, value): (" + key + ", " + value + ")");
        }
    }

    // Logging helper
    private static void log(String msg) {
        if (DEBUG) Log.d(TAG, msg);
    }
}
