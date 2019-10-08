/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.notification;

import static android.app.NotificationManager.Policy.SUPPRESSED_EFFECT_SCREEN_OFF;
import static android.app.NotificationManager.Policy.SUPPRESSED_EFFECT_SCREEN_ON;

import android.app.ActivityManager;
import android.app.AutomaticZenRule;
import android.app.NotificationManager;
import android.content.Context;
import android.database.Cursor;
import android.icu.text.ListFormatter;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.service.notification.ZenModeConfig;
import android.service.notification.ZenPolicy;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class ZenModeBackend {
    @VisibleForTesting
    protected static final String ZEN_MODE_FROM_ANYONE = "zen_mode_from_anyone";
    @VisibleForTesting
    protected static final String ZEN_MODE_FROM_CONTACTS = "zen_mode_from_contacts";
    @VisibleForTesting
    protected static final String ZEN_MODE_FROM_STARRED = "zen_mode_from_starred";
    @VisibleForTesting
    protected static final String ZEN_MODE_FROM_NONE = "zen_mode_from_none";
    protected static final int SOURCE_NONE = -1;
    private static List<String> mDefaultRuleIds;

    private static ZenModeBackend sInstance;

    protected int mZenMode;
    /** gets policy last set by updatePolicy **/
    protected NotificationManager.Policy mPolicy;
    private final NotificationManager mNotificationManager;

    private String TAG = "ZenModeSettingsBackend";
    private final Context mContext;

    public static ZenModeBackend getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ZenModeBackend(context);
        }
        return sInstance;
    }

    public ZenModeBackend(Context context) {
        mContext = context;
        mNotificationManager = (NotificationManager) context.getSystemService(
                Context.NOTIFICATION_SERVICE);
        updateZenMode();
        updatePolicy();
    }

    protected void updatePolicy() {
        if (mNotificationManager != null) {
            mPolicy = mNotificationManager.getNotificationPolicy();
        }
    }

    protected void updateZenMode() {
        mZenMode = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.ZEN_MODE, mZenMode);
    }

    protected boolean updateZenRule(String id, AutomaticZenRule rule) {
        return NotificationManager.from(mContext).updateAutomaticZenRule(id, rule);
    }

    protected void setZenMode(int zenMode) {
        NotificationManager.from(mContext).setZenMode(zenMode, null, TAG);
        mZenMode = getZenMode();
    }

    protected void setZenModeForDuration(int minutes) {
        Uri conditionId = ZenModeConfig.toTimeCondition(mContext, minutes,
                ActivityManager.getCurrentUser(), true).id;
        mNotificationManager.setZenMode(Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS,
                conditionId, TAG);
        mZenMode = getZenMode();
    }

    protected int getZenMode() {
        mZenMode = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.ZEN_MODE, mZenMode);
        return mZenMode;
    }

    protected boolean isVisualEffectSuppressed(int visualEffect) {
        return (mPolicy.suppressedVisualEffects & visualEffect) != 0;
    }

    protected boolean isPriorityCategoryEnabled(int categoryType) {
        return (mPolicy.priorityCategories & categoryType) != 0;
    }

    protected int getNewDefaultPriorityCategories(boolean allow, int categoryType) {
        int priorityCategories = mPolicy.priorityCategories;
        if (allow) {
            priorityCategories |= categoryType;
        } else {
            priorityCategories &= ~categoryType;
        }
        return priorityCategories;
    }

    protected int getPriorityCallSenders() {
        if (isPriorityCategoryEnabled(NotificationManager.Policy.PRIORITY_CATEGORY_CALLS)) {
            return mPolicy.priorityCallSenders;
        }

        return SOURCE_NONE;
    }

    protected int getPriorityMessageSenders() {
        if (isPriorityCategoryEnabled(
                NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES)) {
            return mPolicy.priorityMessageSenders;
        }
        return SOURCE_NONE;
    }

    protected void saveVisualEffectsPolicy(int category, boolean suppress) {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ZEN_SETTINGS_UPDATED, 1);

        int suppressedEffects = getNewSuppressedEffects(suppress, category);
        savePolicy(mPolicy.priorityCategories, mPolicy.priorityCallSenders,
                mPolicy.priorityMessageSenders, suppressedEffects);
    }

    protected void saveSoundPolicy(int category, boolean allow) {
        int priorityCategories = getNewDefaultPriorityCategories(allow, category);
        savePolicy(priorityCategories, mPolicy.priorityCallSenders,
                mPolicy.priorityMessageSenders, mPolicy.suppressedVisualEffects);
    }

    protected void savePolicy(int priorityCategories, int priorityCallSenders,
            int priorityMessageSenders, int suppressedVisualEffects) {
        mPolicy = new NotificationManager.Policy(priorityCategories, priorityCallSenders,
                priorityMessageSenders, suppressedVisualEffects);
        mNotificationManager.setNotificationPolicy(mPolicy);
    }


    private int getNewSuppressedEffects(boolean suppress, int effectType) {
        int effects = mPolicy.suppressedVisualEffects;

        if (suppress) {
            effects |= effectType;
        } else {
            effects &= ~effectType;
        }

        return clearDeprecatedEffects(effects);
    }

    private int clearDeprecatedEffects(int effects) {
        return effects & ~(SUPPRESSED_EFFECT_SCREEN_ON | SUPPRESSED_EFFECT_SCREEN_OFF);
    }

    protected boolean isEffectAllowed(int effect) {
        return (mPolicy.suppressedVisualEffects & effect) == 0;
    }

    protected void saveSenders(int category, int val) {
        int priorityCallSenders = getPriorityCallSenders();
        int priorityMessagesSenders = getPriorityMessageSenders();
        int categorySenders = getPrioritySenders(category);

        final boolean allowSenders = val != SOURCE_NONE;
        final int allowSendersFrom = val == SOURCE_NONE ? categorySenders : val;

        String stringCategory = "";
        if (category == NotificationManager.Policy.PRIORITY_CATEGORY_CALLS) {
            stringCategory = "Calls";
            priorityCallSenders = allowSendersFrom;
        }

        if (category == NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES) {
            stringCategory = "Messages";
            priorityMessagesSenders = allowSendersFrom;
        }

        savePolicy(getNewDefaultPriorityCategories(allowSenders, category),
            priorityCallSenders, priorityMessagesSenders, mPolicy.suppressedVisualEffects);

        if (ZenModeSettingsBase.DEBUG) Log.d(TAG, "onPrefChange allow" +
                stringCategory + "=" + allowSenders + " allow" + stringCategory + "From="
                + ZenModeConfig.sourceToString(allowSendersFrom));
    }

    protected String getSendersKey(int category) {
        switch (getZenMode()) {
            case Settings.Global.ZEN_MODE_NO_INTERRUPTIONS:
            case Settings.Global.ZEN_MODE_ALARMS:
                return getKeyFromSetting(SOURCE_NONE);
            default:
                int prioritySenders = getPrioritySenders(category);
                return getKeyFromSetting(isPriorityCategoryEnabled(category)
                        ? prioritySenders : SOURCE_NONE);
            }
    }

    private int getPrioritySenders(int category) {
        int categorySenders = -1;

        if (category == NotificationManager.Policy.PRIORITY_CATEGORY_CALLS) {
            return getPriorityCallSenders();
        }

        if (category == NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES) {
            return getPriorityMessageSenders();
        }

        return categorySenders;
    }

    protected static String getKeyFromZenPolicySetting(int contactType) {
        switch (contactType) {
            case ZenPolicy.PEOPLE_TYPE_ANYONE:
                return ZEN_MODE_FROM_ANYONE;
            case  ZenPolicy.PEOPLE_TYPE_CONTACTS:
                return ZEN_MODE_FROM_CONTACTS;
            case ZenPolicy.PEOPLE_TYPE_STARRED:
                return ZEN_MODE_FROM_STARRED;
            case ZenPolicy.PEOPLE_TYPE_NONE:
            default:
                return ZEN_MODE_FROM_NONE;
        }
    }

    protected static String getKeyFromSetting(int contactType) {
        switch (contactType) {
            case NotificationManager.Policy.PRIORITY_SENDERS_ANY:
                return ZEN_MODE_FROM_ANYONE;
            case NotificationManager.Policy.PRIORITY_SENDERS_CONTACTS:
                return ZEN_MODE_FROM_CONTACTS;
            case NotificationManager.Policy.PRIORITY_SENDERS_STARRED:
                return ZEN_MODE_FROM_STARRED;
            case SOURCE_NONE:
            default:
                return ZEN_MODE_FROM_NONE;
        }
    }

    protected int getAlarmsTotalSilenceCallsMessagesSummary(int category) {
        if (category == NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES) {
            return R.string.zen_mode_from_none_messages;
        } else if (category == NotificationManager.Policy.PRIORITY_CATEGORY_CALLS){
            return R.string.zen_mode_from_none_calls;
        }
        return 0;
    }

    protected int getContactsSummary(int category) {
        int contactType = -1;
        if (category == NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES) {
            if (isPriorityCategoryEnabled(category)) {
                contactType = getPriorityMessageSenders();
            }
        } else if (category == NotificationManager.Policy.PRIORITY_CATEGORY_CALLS) {
            if (isPriorityCategoryEnabled(category)) {
                contactType = getPriorityCallSenders();
            }
        }

        switch (contactType) {
            case NotificationManager.Policy.PRIORITY_SENDERS_ANY:
                return R.string.zen_mode_from_anyone;
            case NotificationManager.Policy.PRIORITY_SENDERS_CONTACTS:
                return R.string.zen_mode_from_contacts;
            case NotificationManager.Policy.PRIORITY_SENDERS_STARRED:
                return R.string.zen_mode_from_starred;
            case SOURCE_NONE:
            default:
                if (category == NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES) {
                    return R.string.zen_mode_from_none_messages;
                } else {
                    return R.string.zen_mode_from_none_calls;
                }
        }
    }

    protected int getContactsCallsSummary(ZenPolicy policy) {
        int peopleType = policy.getPriorityCallSenders();
        switch (peopleType) {
            case ZenPolicy.PEOPLE_TYPE_ANYONE:
                return R.string.zen_mode_from_anyone;
            case ZenPolicy.PEOPLE_TYPE_CONTACTS:
                return R.string.zen_mode_from_contacts;
            case ZenPolicy.PEOPLE_TYPE_STARRED:
                return R.string.zen_mode_from_starred;
            case ZenPolicy.PEOPLE_TYPE_NONE:
            default:
                return R.string.zen_mode_from_none_calls;
        }
    }

    protected int getContactsMessagesSummary(ZenPolicy policy) {
        int peopleType = policy.getPriorityMessageSenders();
        switch (peopleType) {
            case ZenPolicy.PEOPLE_TYPE_ANYONE:
                return R.string.zen_mode_from_anyone;
            case ZenPolicy.PEOPLE_TYPE_CONTACTS:
                return R.string.zen_mode_from_contacts;
            case ZenPolicy.PEOPLE_TYPE_STARRED:
                return R.string.zen_mode_from_starred;
            case ZenPolicy.PEOPLE_TYPE_NONE:
            default:
                return R.string.zen_mode_from_none_messages;
        }
    }

    protected static int getZenPolicySettingFromPrefKey(String key) {
        switch (key) {
            case ZEN_MODE_FROM_ANYONE:
                return ZenPolicy.PEOPLE_TYPE_ANYONE;
            case ZEN_MODE_FROM_CONTACTS:
                return ZenPolicy.PEOPLE_TYPE_CONTACTS;
            case ZEN_MODE_FROM_STARRED:
                return ZenPolicy.PEOPLE_TYPE_STARRED;
            case ZEN_MODE_FROM_NONE:
            default:
                return ZenPolicy.PEOPLE_TYPE_NONE;
        }
    }

    protected static int getSettingFromPrefKey(String key) {
        switch (key) {
            case ZEN_MODE_FROM_ANYONE:
                return NotificationManager.Policy.PRIORITY_SENDERS_ANY;
            case ZEN_MODE_FROM_CONTACTS:
                return NotificationManager.Policy.PRIORITY_SENDERS_CONTACTS;
            case ZEN_MODE_FROM_STARRED:
                return NotificationManager.Policy.PRIORITY_SENDERS_STARRED;
            case ZEN_MODE_FROM_NONE:
            default:
                return SOURCE_NONE;
        }
    }

    public boolean removeZenRule(String ruleId) {
        return NotificationManager.from(mContext).removeAutomaticZenRule(ruleId);
    }

    public NotificationManager.Policy getConsolidatedPolicy() {
        return NotificationManager.from(mContext).getConsolidatedNotificationPolicy();
    }

    protected String addZenRule(AutomaticZenRule rule) {
        try {
            return NotificationManager.from(mContext).addAutomaticZenRule(rule);
        } catch (Exception e) {
            return null;
        }
    }

    ZenPolicy setDefaultZenPolicy(ZenPolicy zenPolicy) {
        int calls;
        if (mPolicy.allowCalls()) {
            calls = ZenModeConfig.getZenPolicySenders(mPolicy.allowCallsFrom());
        } else {
            calls = ZenPolicy.PEOPLE_TYPE_NONE;
        }

        int messages;
        if (mPolicy.allowMessages()) {
            messages = ZenModeConfig.getZenPolicySenders(mPolicy.allowMessagesFrom());
        } else {
            messages = ZenPolicy.PEOPLE_TYPE_NONE;
        }

        return new ZenPolicy.Builder(zenPolicy)
                .allowAlarms(mPolicy.allowAlarms())
                .allowCalls(calls)
                .allowEvents(mPolicy.allowEvents())
                .allowMedia(mPolicy.allowMedia())
                .allowMessages(messages)
                .allowReminders(mPolicy.allowReminders())
                .allowRepeatCallers(mPolicy.allowRepeatCallers())
                .allowSystem(mPolicy.allowSystem())
                .showFullScreenIntent(mPolicy.showFullScreenIntents())
                .showLights(mPolicy.showLights())
                .showInAmbientDisplay(mPolicy.showAmbient())
                .showInNotificationList(mPolicy.showInNotificationList())
                .showBadges(mPolicy.showBadges())
                .showPeeking(mPolicy.showPeeking())
                .showStatusBarIcons(mPolicy.showStatusBarIcons())
                .build();
    }

    protected Map.Entry<String, AutomaticZenRule>[] getAutomaticZenRules() {
        Map<String, AutomaticZenRule> ruleMap =
                NotificationManager.from(mContext).getAutomaticZenRules();
        final Map.Entry<String, AutomaticZenRule>[] rt = ruleMap.entrySet().toArray(
                new Map.Entry[ruleMap.size()]);
        Arrays.sort(rt, RULE_COMPARATOR);
        return rt;
    }

    protected AutomaticZenRule getAutomaticZenRule(String id) {
        return NotificationManager.from(mContext).getAutomaticZenRule(id);
    }

    private static List<String> getDefaultRuleIds() {
        if (mDefaultRuleIds == null) {
            mDefaultRuleIds = ZenModeConfig.DEFAULT_RULE_IDS;
        }
        return mDefaultRuleIds;
    }

    NotificationManager.Policy toNotificationPolicy(ZenPolicy policy) {
        ZenModeConfig config = new ZenModeConfig();
        return config.toNotificationPolicy(policy);
    }

    @VisibleForTesting
    List<String> getStarredContacts(Cursor cursor) {
        List<String> starredContacts = new ArrayList<>();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String contact = cursor.getString(0);
                if (contact != null) {
                    starredContacts.add(contact);
                }
            } while (cursor.moveToNext());
        }
        return starredContacts;
    }

    private List<String> getStarredContacts() {
        Cursor cursor = null;
        try {
            cursor = queryData();
            return getStarredContacts(cursor);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public String getStarredContactsSummary() {
        List<String> starredContacts = getStarredContacts();
        int numStarredContacts = starredContacts.size();

        List<String> displayContacts = new ArrayList<>();

        if (numStarredContacts == 0) {
            displayContacts.add(mContext.getString(R.string.zen_mode_from_none));
        } else {
            for (int i = 0; i < 2 && i < numStarredContacts; i++) {
                displayContacts.add(starredContacts.get(i));
            }

            if (numStarredContacts == 3) {
                displayContacts.add(starredContacts.get(2));
            } else if (numStarredContacts > 2) {
                displayContacts.add(mContext.getResources().getQuantityString(
                        R.plurals.zen_mode_starred_contacts_summary_additional_contacts,
                        numStarredContacts - 2, numStarredContacts - 2));
            }
        }

        // values in displayContacts must not be null
        return ListFormatter.getInstance().format(displayContacts);
    }

    private Cursor queryData() {
        return mContext.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI,
                new String[]{ContactsContract.Contacts.DISPLAY_NAME_PRIMARY},
                ContactsContract.Data.STARRED + "=1", null,
                ContactsContract.Data.TIMES_CONTACTED);
    }

    @VisibleForTesting
    public static final Comparator<Map.Entry<String, AutomaticZenRule>> RULE_COMPARATOR =
            new Comparator<Map.Entry<String, AutomaticZenRule>>() {
                @Override
                public int compare(Map.Entry<String, AutomaticZenRule> lhs,
                        Map.Entry<String, AutomaticZenRule> rhs) {
                    // if it's a default rule, should be at the top of automatic rules
                    boolean lhsIsDefaultRule = getDefaultRuleIds().contains(lhs.getKey());
                    boolean rhsIsDefaultRule = getDefaultRuleIds().contains(rhs.getKey());
                    if (lhsIsDefaultRule != rhsIsDefaultRule) {
                        return lhsIsDefaultRule ? -1 : 1;
                    }

                    int byDate = Long.compare(lhs.getValue().getCreationTime(),
                            rhs.getValue().getCreationTime());
                    if (byDate != 0) {
                        return byDate;
                    } else {
                        return key(lhs.getValue()).compareTo(key(rhs.getValue()));
                    }
                }

                private String key(AutomaticZenRule rule) {
                    final int type = ZenModeConfig.isValidScheduleConditionId(rule.getConditionId())
                            ? 1 : ZenModeConfig.isValidEventConditionId(rule.getConditionId())
                            ? 2 : 3;
                    return type + rule.getName().toString();
                }
            };
}
