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

package com.android.settings.wifi.calling;

import static android.app.slice.Slice.EXTRA_TOGGLE_STATE;

import static com.android.settings.slices.CustomSliceRegistry.WIFI_CALLING_PREFERENCE_URI;
import static com.android.settings.slices.CustomSliceRegistry.WIFI_CALLING_URI;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.telephony.AccessNetworkConstants;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.telephony.ims.ImsMmTelManager;
import android.telephony.ims.feature.MmTelFeature;
import android.telephony.ims.stub.ImsRegistrationImplBase;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.ListBuilder.RowBuilder;
import androidx.slice.builders.SliceAction;

import com.android.ims.ImsConfig;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.network.ims.ImsQuery;
import com.android.settings.network.ims.ImsQueryFeatureIsReady;
import com.android.settings.network.ims.ImsQueryProvisioningStat;
import com.android.settings.network.ims.ImsQueryResult;
import com.android.settings.network.ims.ImsQuerySupportStat;
import com.android.settings.network.ims.ImsQuerySystemTtyStat;
import com.android.settings.network.ims.ImsQueryTtyOnVolteStat;
import com.android.settings.network.ims.ImsQueryWfcUserSetting;
import com.android.settings.slices.SliceBroadcastReceiver;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * Helper class to control slices for wifi calling settings.
 */
public class WifiCallingSliceHelper {

    private static final String TAG = "WifiCallingSliceHelper";

    /**
     * Settings slice path to wifi calling setting.
     */
    public static final String PATH_WIFI_CALLING = "wifi_calling";

    /**
     * Settings slice path to wifi calling preference setting.
     */
    public static final String PATH_WIFI_CALLING_PREFERENCE =
            "wifi_calling_preference";

    /**
     * Action passed for changes to wifi calling slice (toggle).
     */
    public static final String ACTION_WIFI_CALLING_CHANGED =
            "com.android.settings.wifi.calling.action.WIFI_CALLING_CHANGED";

    /**
     * Action passed when user selects wifi only preference.
     */
    public static final String ACTION_WIFI_CALLING_PREFERENCE_WIFI_ONLY =
            "com.android.settings.slice.action.WIFI_CALLING_PREFERENCE_WIFI_ONLY";
    /**
     * Action passed when user selects wifi preferred preference.
     */
    public static final String ACTION_WIFI_CALLING_PREFERENCE_WIFI_PREFERRED =
            "com.android.settings.slice.action.WIFI_CALLING_PREFERENCE_WIFI_PREFERRED";
    /**
     * Action passed when user selects cellular preferred preference.
     */
    public static final String ACTION_WIFI_CALLING_PREFERENCE_CELLULAR_PREFERRED =
            "com.android.settings.slice.action.WIFI_CALLING_PREFERENCE_CELLULAR_PREFERRED";

    /**
     * Action for Wifi calling Settings activity which
     * allows setting configuration for Wifi calling
     * related settings
     */
    public static final String ACTION_WIFI_CALLING_SETTINGS_ACTIVITY =
            "android.settings.WIFI_CALLING_SETTINGS";

    /**
     * Timeout for querying wifi calling setting from ims manager.
     */
    private static final int TIMEOUT_MILLIS = 2000;

    private final Context mContext;

    @VisibleForTesting
    public WifiCallingSliceHelper(Context context) {
        mContext = context;
    }

    /**
     * Returns Slice object for wifi calling settings.
     *
     * If wifi calling is being turned on and if wifi calling activation is needed for the current
     * carrier, this method will return Slice with instructions to go to Settings App.
     *
     * If wifi calling is not supported for the current carrier, this method will return slice with
     * not supported message.
     *
     * If wifi calling setting can be changed, this method will return the slice to toggle wifi
     * calling option with ACTION_WIFI_CALLING_CHANGED as endItem.
     */
    public Slice createWifiCallingSlice(Uri sliceUri) {
        final int subId = getDefaultVoiceSubId();
        final Resources res = getResourcesForSubId(subId);

        if (subId <= SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            Log.d(TAG, "Invalid subscription Id");
            return null;
        }

        final ImsQuery isTtyEnabled = isSystemTtyEnabled();
        final ImsQuery isVolteTtyEnabled = isTtyOnVolteEnabled(subId);
        final ImsQuery isWfcSettingEnabled = isWfcEnabledByUser(subId);
        final ImsQuery isWfcProvisioned = isWfcProvisionedOnDevice(subId);
        final ImsQuery isWfcPlatformEnabled = isWfcEnabledByPlatform(subId);

        boolean isWifiCallingEnabled = false;
        try (ImsQueryResult queryResult = new ImsQueryResult(
                isWfcPlatformEnabled, isWfcProvisioned, isWfcSettingEnabled,
                isTtyEnabled, isVolteTtyEnabled)) {
            if (queryResult.get(isWfcPlatformEnabled) && queryResult.get(isWfcProvisioned)) {
                isWifiCallingEnabled = queryResult.get(isWfcSettingEnabled)
                        && (queryResult.get(isVolteTtyEnabled) || !queryResult.get(isTtyEnabled));
            } else {
                return null;
            }
        } catch (Exception exception) {
            Log.e(TAG, "Unable to create wifi calling slice", exception);
            return null;
        }

        final Intent activationAppIntent =
                getWifiCallingCarrierActivityIntent(subId);

        // Send this actionable wifi calling slice to toggle the setting
        // only when there is no need for wifi calling activation with the server
        if (activationAppIntent != null && !isWifiCallingEnabled) {
            Log.d(TAG, "Needs Activation");
            // Activation needed for the next action of the user
            // Give instructions to go to settings app
            return getNonActionableWifiCallingSlice(
                    res.getText(R.string.wifi_calling_settings_title),
                    res.getText(R.string.wifi_calling_settings_activation_instructions),
                    sliceUri, getActivityIntent(ACTION_WIFI_CALLING_SETTINGS_ACTIVITY));
        }
        return getWifiCallingSlice(sliceUri, isWifiCallingEnabled, subId);
    }

    @VisibleForTesting
    ImsQuery isWfcEnabledByUser(int subId) {
        return new ImsQueryWfcUserSetting(subId);
    }

    @VisibleForTesting
    ImsQuery isSystemTtyEnabled() {
        return new ImsQuerySystemTtyStat(mContext);
    }

    @VisibleForTesting
    ImsQuery isTtyOnVolteEnabled(int subId) {
        return new ImsQueryTtyOnVolteStat(subId);
    }

    @VisibleForTesting
    ImsQuery isImsServiceStateReady(int subId) {
        return new ImsQueryFeatureIsReady(subId);
    }

    @VisibleForTesting
    ImsQuery isWfcProvisionedOnDevice(int subId) {
        return new ImsQueryProvisioningStat(subId,
                MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VOICE,
                ImsRegistrationImplBase.REGISTRATION_TECH_IWLAN);
    }

    @VisibleForTesting
    ImsQuery isWfcEnabledByPlatform(int subId) {
        return new ImsQuerySupportStat(subId,
                MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VOICE,
                AccessNetworkConstants.TRANSPORT_TYPE_WLAN);
    }

    /**
     * Builds a toggle slice where the intent takes you to the wifi calling page and the toggle
     * enables/disables wifi calling.
     */
    private Slice getWifiCallingSlice(Uri sliceUri, boolean isWifiCallingEnabled, int subId) {
        final IconCompat icon = IconCompat.createWithResource(mContext, R.drawable.wifi_signal);
        final Resources res = getResourcesForSubId(subId);

        return new ListBuilder(mContext, sliceUri, ListBuilder.INFINITY)
                .setAccentColor(Utils.getColorAccentDefaultColor(mContext))
                .addRow(new RowBuilder()
                        .setTitle(res.getText(R.string.wifi_calling_settings_title))
                        .addEndItem(
                                SliceAction.createToggle(
                                        getBroadcastIntent(ACTION_WIFI_CALLING_CHANGED),
                                        null /* actionTitle */, isWifiCallingEnabled))
                        .setPrimaryAction(SliceAction.createDeeplink(
                                getActivityIntent(ACTION_WIFI_CALLING_SETTINGS_ACTIVITY),
                                icon,
                                ListBuilder.ICON_IMAGE,
                                res.getText(R.string.wifi_calling_settings_title))))
                .build();
    }

    /**
     * Returns Slice object for wifi calling preference.
     *
     * If wifi calling is not turned on, this method will return a slice to turn on wifi calling.
     *
     * If wifi calling preference is not user editable, this method will return a slice to display
     * appropriate message.
     *
     * If wifi calling preference can be changed, this method will return a slice with 3 or 4 rows:
     * Header Row: current preference settings
     * Row 1: wifi only option with ACTION_WIFI_CALLING_PREFERENCE_WIFI_ONLY, if wifi only option
     * is editable
     * Row 2: wifi preferred option with ACTION_WIFI_CALLING_PREFERENCE_WIFI_PREFERRED
     * Row 3: cellular preferred option with ACTION_WIFI_CALLING_PREFERENCE_CELLULAR_PREFERRED
     */
    public Slice createWifiCallingPreferenceSlice(Uri sliceUri) {
        final int subId = getDefaultVoiceSubId();

        if (subId <= SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            Log.d(TAG, "Invalid Subscription Id");
            return null;
        }

        final boolean isWifiCallingPrefEditable = isCarrierConfigManagerKeyEnabled(
                CarrierConfigManager.KEY_EDITABLE_WFC_MODE_BOOL, subId, false);
        final boolean isWifiOnlySupported = isCarrierConfigManagerKeyEnabled(
                CarrierConfigManager.KEY_CARRIER_WFC_SUPPORTS_WIFI_ONLY_BOOL, subId, true);
        if (!isWifiCallingPrefEditable) {
            Log.d(TAG, "Wifi calling preference is not editable");
            return null;
        }

        final ImsQuery isTtyEnabled = isSystemTtyEnabled();
        final ImsQuery isVolteTtyEnabled = isTtyOnVolteEnabled(subId);
        final ImsQuery isWfcSettingEnabled = isWfcEnabledByUser(subId);
        final ImsQuery isWfcProvisioned = isWfcProvisionedOnDevice(subId);
        final ImsQuery isWfcPlatformEnabled = isWfcEnabledByPlatform(subId);

        boolean isWifiCallingEnabled = false;
        try (ImsQueryResult queryResult = new ImsQueryResult(
                isWfcPlatformEnabled, isWfcProvisioned, isWfcSettingEnabled,
                isTtyEnabled, isVolteTtyEnabled)) {
            if (queryResult.get(isWfcPlatformEnabled) && queryResult.get(isWfcProvisioned)) {
                isWifiCallingEnabled = queryResult.get(isWfcSettingEnabled)
                        && (queryResult.get(isVolteTtyEnabled) || !queryResult.get(isTtyEnabled));
            } else {
                return null;
            }
        } catch (Exception exception) {
            Log.e(TAG, "Unable to get wifi calling preferred mode", exception);
            return null;
        }

        int wfcMode = -1;
        try {
            wfcMode = getWfcMode(getImsMmTelManager(subId));
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            Log.e(TAG, "Unable to get wifi calling preferred mode", e);
            return null;
        }
        if (!isWifiCallingEnabled) {
            // wifi calling is not enabled. Ask user to enable wifi calling
            final Resources res = getResourcesForSubId(subId);
            return getNonActionableWifiCallingSlice(
                    res.getText(R.string.wifi_calling_mode_title),
                    res.getText(R.string.wifi_calling_turn_on),
                    sliceUri, getActivityIntent(ACTION_WIFI_CALLING_SETTINGS_ACTIVITY));
        }
        // Return the slice to change wifi calling preference
        return getWifiCallingPreferenceSlice(
                isWifiOnlySupported, wfcMode, sliceUri, subId);
    }

    /**
     * Returns actionable wifi calling preference slice.
     *
     * @param isWifiOnlySupported adds row for wifi only if this is true
     * @param currentWfcPref      current Preference {@link ImsConfig}
     * @param sliceUri            sliceUri
     * @param subId               subscription id
     * @return Slice for actionable wifi calling preference settings
     */
    private Slice getWifiCallingPreferenceSlice(boolean isWifiOnlySupported,
            int currentWfcPref,
            Uri sliceUri,
            int subId) {
        final IconCompat icon = IconCompat.createWithResource(mContext, R.drawable.wifi_signal);
        final Resources res = getResourcesForSubId(subId);
        // Top row shows information on current preference state
        final ListBuilder listBuilder = new ListBuilder(mContext, sliceUri, ListBuilder.INFINITY)
                .setAccentColor(Utils.getColorAccentDefaultColor(mContext));
        listBuilder.setHeader(new ListBuilder.HeaderBuilder()
                .setTitle(res.getText(R.string.wifi_calling_mode_title))
                .setSubtitle(getWifiCallingPreferenceSummary(currentWfcPref, subId))
                .setPrimaryAction(SliceAction.createDeeplink(
                        getActivityIntent(ACTION_WIFI_CALLING_SETTINGS_ACTIVITY),
                        icon,
                        ListBuilder.ICON_IMAGE,
                        res.getText(R.string.wifi_calling_mode_title))));

        if (isWifiOnlySupported) {
            listBuilder.addRow(wifiPreferenceRowBuilder(listBuilder,
                    com.android.internal.R.string.wfc_mode_wifi_only_summary,
                    ACTION_WIFI_CALLING_PREFERENCE_WIFI_ONLY,
                    currentWfcPref == ImsMmTelManager.WIFI_MODE_WIFI_ONLY, subId));
        }

        listBuilder.addRow(wifiPreferenceRowBuilder(listBuilder,
                com.android.internal.R.string.wfc_mode_wifi_preferred_summary,
                ACTION_WIFI_CALLING_PREFERENCE_WIFI_PREFERRED,
                currentWfcPref == ImsMmTelManager.WIFI_MODE_WIFI_PREFERRED, subId));

        listBuilder.addRow(wifiPreferenceRowBuilder(listBuilder,
                com.android.internal.R.string.wfc_mode_cellular_preferred_summary,
                ACTION_WIFI_CALLING_PREFERENCE_CELLULAR_PREFERRED,
                currentWfcPref == ImsMmTelManager.WIFI_MODE_CELLULAR_PREFERRED, subId));

        return listBuilder.build();
    }

    /**
     * Returns RowBuilder for a new row containing specific wifi calling preference.
     *
     * @param listBuilder          ListBuilder that will be the parent for this RowBuilder
     * @param preferenceTitleResId resource Id for the preference row title
     * @param action               action to be added for the row
     * @param subId                subscription id
     * @return RowBuilder for the row
     */
    private RowBuilder wifiPreferenceRowBuilder(ListBuilder listBuilder,
            int preferenceTitleResId, String action, boolean checked, int subId) {
        final IconCompat icon =
                IconCompat.createWithResource(mContext, R.drawable.radio_button_check);
        final Resources res = getResourcesForSubId(subId);
        return new RowBuilder()
                .setTitle(res.getText(preferenceTitleResId))
                .setTitleItem(SliceAction.createToggle(getBroadcastIntent(action),
                        icon, res.getText(preferenceTitleResId), checked));
    }


    /**
     * Returns the String describing wifi calling preference mentioned in wfcMode
     *
     * @param wfcMode ImsConfig constant for the preference {@link ImsConfig}
     * @param subId   subscription id
     * @return summary/name of the wifi calling preference
     */
    private CharSequence getWifiCallingPreferenceSummary(int wfcMode, int subId) {
        final Resources res = getResourcesForSubId(subId);
        switch (wfcMode) {
            case ImsMmTelManager.WIFI_MODE_WIFI_ONLY:
                return res.getText(
                        com.android.internal.R.string.wfc_mode_wifi_only_summary);
            case ImsMmTelManager.WIFI_MODE_WIFI_PREFERRED:
                return res.getText(
                        com.android.internal.R.string.wfc_mode_wifi_preferred_summary);
            case ImsMmTelManager.WIFI_MODE_CELLULAR_PREFERRED:
                return res.getText(
                        com.android.internal.R.string.wfc_mode_cellular_preferred_summary);
            default:
                return null;
        }
    }

    protected ImsMmTelManager getImsMmTelManager(int subId) {
        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            return null;
        }
        return ImsMmTelManager.createForSubscriptionId(subId);
    }

    private int getWfcMode(ImsMmTelManager imsMmTelManager)
            throws InterruptedException, ExecutionException, TimeoutException {
        final FutureTask<Integer> wfcModeTask = new FutureTask<>(new Callable<Integer>() {
            @Override
            public Integer call() {
                return imsMmTelManager.getVoWiFiModeSetting();
            }
        });
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(wfcModeTask);
        return wfcModeTask.get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    }

    /**
     * Handles wifi calling setting change from wifi calling slice and posts notification. Should be
     * called when intent action is ACTION_WIFI_CALLING_CHANGED. Executed in @WorkerThread
     *
     * @param intent action performed
     */
    public void handleWifiCallingChanged(Intent intent) {
        final int subId = getDefaultVoiceSubId();

        if ((subId <= SubscriptionManager.INVALID_SUBSCRIPTION_ID)
                || (intent.getBooleanExtra(EXTRA_TOGGLE_STATE, true)
                != intent.getBooleanExtra(EXTRA_TOGGLE_STATE, false))) {
            notifyWifiCallingChanged();
            return;
        }

        final ImsQuery isTtyEnabled = isSystemTtyEnabled();
        final ImsQuery isVolteTtyEnabled = isTtyOnVolteEnabled(subId);
        final ImsQuery isWfcSettingEnabled = isWfcEnabledByUser(subId);
        final ImsQuery isWfcProvisioned = isWfcProvisionedOnDevice(subId);
        final ImsQuery isWfcPlatformEnabled = isWfcEnabledByPlatform(subId);

        try (ImsQueryResult queryResult = new ImsQueryResult(
                isWfcPlatformEnabled, isWfcProvisioned, isWfcSettingEnabled,
                isTtyEnabled, isVolteTtyEnabled)) {
            if (queryResult.get(isWfcPlatformEnabled) && queryResult.get(isWfcProvisioned)) {
                final boolean currentValue = queryResult.get(isWfcSettingEnabled)
                        && (queryResult.get(isVolteTtyEnabled) || !queryResult.get(isTtyEnabled));
                final boolean newValue = intent.getBooleanExtra(EXTRA_TOGGLE_STATE,
                        currentValue);
                final Intent activationAppIntent =
                        getWifiCallingCarrierActivityIntent(subId);
                if (!newValue || activationAppIntent == null) {
                    // If either the action is to turn off wifi calling setting
                    // or there is no activation involved - Update the setting
                    if (newValue != currentValue) {
                        getImsMmTelManager(subId).setVoWiFiSettingEnabled(newValue);
                    }
                }
            }
        } catch (Exception exception) {
            Log.e(TAG, "Unable to get wifi calling status. subId=" + subId, exception);
        }
        notifyWifiCallingChanged();
    }


    private void notifyWifiCallingChanged() {
        // notify change in slice in any case to get re-queried. This would result in displaying
        // appropriate message with the updated setting.
        mContext.getContentResolver().notifyChange(WIFI_CALLING_URI, null);
    }

    /**
     * Handles wifi calling preference Setting change from wifi calling preference Slice and posts
     * notification for the change. Should be called when intent action is one of the below
     * ACTION_WIFI_CALLING_PREFERENCE_WIFI_ONLY
     * ACTION_WIFI_CALLING_PREFERENCE_WIFI_PREFERRED
     * ACTION_WIFI_CALLING_PREFERENCE_CELLULAR_PREFERRED
     *
     * @param intent intent
     */
    public void handleWifiCallingPreferenceChanged(Intent intent) {
        final int subId = getDefaultVoiceSubId();
        final int errorValue = -1;

        final boolean isWifiCallingPrefEditable = isCarrierConfigManagerKeyEnabled(
                CarrierConfigManager.KEY_EDITABLE_WFC_MODE_BOOL, subId, false);

        if ((subId <= SubscriptionManager.INVALID_SUBSCRIPTION_ID)
                || (!isWifiCallingPrefEditable)) {
            notifyWifiCallingPreferenceChanged();
            return;
        }

        final ImsQuery isTtyEnabled = isSystemTtyEnabled();
        final ImsQuery isVolteTtyEnabled = isTtyOnVolteEnabled(subId);
        final ImsQuery isWfcSettingEnabled = isWfcEnabledByUser(subId);
        final ImsQuery isWfcProvisioned = isWfcProvisionedOnDevice(subId);
        final ImsQuery isWfcPlatformEnabled = isWfcEnabledByPlatform(subId);

        try (ImsQueryResult queryResult = new ImsQueryResult(
                isWfcPlatformEnabled, isWfcProvisioned, isWfcSettingEnabled,
                isTtyEnabled, isVolteTtyEnabled)) {
            if (queryResult.get(isWfcPlatformEnabled)
                    && queryResult.get(isWfcProvisioned)
                    && queryResult.get(isWfcSettingEnabled)
                    && (queryResult.get(isVolteTtyEnabled) || !queryResult.get(isTtyEnabled))
                ) {
                final ImsMmTelManager imsMmTelManager = getImsMmTelManager(subId);

                // Change the preference only when wifi calling is enabled
                // And when wifi calling preference is editable for the current carrier
                final int currentValue = imsMmTelManager.getVoWiFiModeSetting();
                int newValue = errorValue;
                switch (intent.getAction()) {
                    case ACTION_WIFI_CALLING_PREFERENCE_WIFI_ONLY:
                        final boolean isWifiOnlySupported = isCarrierConfigManagerKeyEnabled(
                                CarrierConfigManager.KEY_CARRIER_WFC_SUPPORTS_WIFI_ONLY_BOOL,
                                subId, true);
                        if (isWifiOnlySupported) {
                            // change to wifi_only when wifi_only is enabled.
                            newValue = ImsMmTelManager.WIFI_MODE_WIFI_ONLY;
                        }
                        break;
                    case ACTION_WIFI_CALLING_PREFERENCE_WIFI_PREFERRED:
                        newValue = ImsMmTelManager.WIFI_MODE_WIFI_PREFERRED;
                        break;
                    case ACTION_WIFI_CALLING_PREFERENCE_CELLULAR_PREFERRED:
                        newValue = ImsMmTelManager.WIFI_MODE_CELLULAR_PREFERRED;
                        break;
                }
                if (newValue != errorValue && newValue != currentValue) {
                    // Update the setting only when there is a valid update
                    imsMmTelManager.setVoWiFiModeSetting(newValue);
                }
            }
        } catch (Exception exception) {
            Log.e(TAG, "Unable to get wifi calling status. subId=" + subId, exception);
        }
        notifyWifiCallingPreferenceChanged();
    }

    private void notifyWifiCallingPreferenceChanged() {
        // notify change in slice in any case to get re-queried. This would result in displaying
        // appropriate message.
        mContext.getContentResolver().notifyChange(WIFI_CALLING_PREFERENCE_URI, null);
    }

    /**
     * Returns Slice with the title and subtitle provided as arguments with wifi signal Icon.
     *
     * @param title    Title of the slice
     * @param subtitle Subtitle of the slice
     * @param sliceUri slice uri
     * @return Slice with title and subtitle
     */
    private Slice getNonActionableWifiCallingSlice(CharSequence title, CharSequence subtitle,
            Uri sliceUri, PendingIntent primaryActionIntent) {
        final IconCompat icon = IconCompat.createWithResource(mContext, R.drawable.wifi_signal);
        return new ListBuilder(mContext, sliceUri, ListBuilder.INFINITY)
                .setAccentColor(Utils.getColorAccentDefaultColor(mContext))
                .addRow(new RowBuilder()
                        .setTitle(title)
                        .setSubtitle(subtitle)
                        .setPrimaryAction(SliceAction.createDeeplink(
                                primaryActionIntent, icon, ListBuilder.SMALL_IMAGE,
                                title)))
                .build();
    }

    /**
     * Returns {@code true} when the key is enabled for the carrier, and {@code false} otherwise.
     */
    protected boolean isCarrierConfigManagerKeyEnabled(String key, int subId,
            boolean defaultValue) {
        final CarrierConfigManager configManager = getCarrierConfigManager(mContext);
        boolean ret = false;
        if (configManager != null) {
            final PersistableBundle bundle = configManager.getConfigForSubId(subId);
            if (bundle != null) {
                ret = bundle.getBoolean(key, defaultValue);
            }
        }
        return ret;
    }

    protected CarrierConfigManager getCarrierConfigManager(Context mContext) {
        return mContext.getSystemService(CarrierConfigManager.class);
    }

    /**
     * Returns the current default voice subId obtained from SubscriptionManager
     */
    protected int getDefaultVoiceSubId() {
        return SubscriptionManager.getDefaultVoiceSubscriptionId();
    }

    /**
     * Returns Intent of the activation app required to activate wifi calling or null if there is no
     * need for activation.
     */
    protected Intent getWifiCallingCarrierActivityIntent(int subId) {
        final CarrierConfigManager configManager = getCarrierConfigManager(mContext);
        if (configManager == null) {
            return null;
        }

        final PersistableBundle bundle = configManager.getConfigForSubId(subId);
        if (bundle == null) {
            return null;
        }

        final String carrierApp = bundle.getString(
                CarrierConfigManager.KEY_WFC_EMERGENCY_ADDRESS_CARRIER_APP_STRING);
        if (TextUtils.isEmpty(carrierApp)) {
            return null;
        }

        final ComponentName componentName = ComponentName.unflattenFromString(carrierApp);
        if (componentName == null) {
            return null;
        }

        final Intent intent = new Intent();
        intent.setComponent(componentName);
        return intent;
    }

    /**
     * @return {@link PendingIntent} to the Settings home page.
     */
    public static PendingIntent getSettingsIntent(Context context) {
        final Intent intent = new Intent(Settings.ACTION_SETTINGS);
        return PendingIntent.getActivity(context, 0 /* requestCode */, intent, 0 /* flags */);
    }

    private PendingIntent getBroadcastIntent(String action) {
        final Intent intent = new Intent(action);
        intent.setClass(mContext, SliceBroadcastReceiver.class);
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        return PendingIntent.getBroadcast(mContext, 0 /* requestCode */, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    /**
     * Returns PendingIntent to start activity specified by action
     */
    private PendingIntent getActivityIntent(String action) {
        final Intent intent = new Intent(action);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(mContext, 0 /* requestCode */, intent, 0 /* flags */);
    }

    private Resources getResourcesForSubId(int subId) {
        return SubscriptionManager.getResourcesForSubId(mContext, subId);
    }
}
