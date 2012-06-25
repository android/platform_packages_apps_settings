/*
 * Copyright (C) 2010 The Android Open Source Project
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

import com.android.settings.LocaleAdapter;
import com.android.settings.R;

import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.app.ListFragment;
import android.app.backup.BackupManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import java.text.Collator;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class LocaleListFragment extends ListFragment {
    private static final String TAG = "LocaleListFragment";

    /** The length of a string representation of a locale. */
    protected static final int LOCALE_STRING_LENGTH = 5;

    /** Property key for the default sw country. */
    protected static final String PROPERTY_COUNTRY = "ro.product.locale.region";
    /** Property key for the default sw language. */
    protected static final String PROPERTY_LANGUAGE = "ro.product.locale.language";

    /** State for setting the top locales by sim country. */
    protected static final int STATE_SIMCARD_COUNTRY = 0;
    /** State for setting the top locales by sw locale. */
    protected static final int STATE_SW_LOCALE = 1;
    /** State for setting the top locale by default locale. */
    private static final int STATE_DEFAULT_LOCALE = 2;
    /** State for setting the top locale by customized locale alone on top.*/
    protected static final int STATE_CUSTOMIZED_LOCALE_SINGLE = 3;
    /** State for setting the top locale by customized locale. */
    protected static final int STATE_CUSTOMIZED_LOCALE_TOP = 4;

    /** Special translations that are not correct in the packages. */
    private String[] mSpecialLocaleCodes;
    private String[] mSpecialLocaleNames;
    private String[] mSpecialLocaleLanguageCodes;
    private String[] mSpecialLocaleLanguageNames;

    /** State holder for what we should search by. */
    private int mState;
    /** The customized language. */
    private String mCustomizedLanguage;
    /** The customized country. */
    private String mCustomizedCountry;
    /** The default sim country. */
    protected String mDefaultSimCountry;
    /** The default sw country. */
    protected String mDefaultSwCountry;
    /** The default sw language. */
    protected String mDefaultSwLanguage;
    /** The list adapter */
    private LocaleAdapter mAdapter;

    public static interface LocaleSelectionListener {
        // You can add any argument if you really need it...
        public void onLocaleSelected(Locale locale);
    }

    LocaleSelectionListener mListener;  // default to null

    public static class LocaleInfo implements Comparable<LocaleInfo> {
        private static final int LOCALE_DIVIDER_LAYOUT = R.layout.locale_divider;

        private static final int LOCALE_ITEM_LAYOUT = R.layout.locale_picker_item;

        public static final int LOCALE_DIVIDER = 0;

        public static final int LOCALE_ITEM = 1;

        public static final int NBR_OF_TYPES = LOCALE_ITEM + 1;

        static final Collator sCollator = Collator.getInstance();

        String label;
        Locale locale;
        boolean enabled;
        int resourceId;
        int type;

        private LocaleInfo(int type) {
            this.type = type;
            switch (type) {
                case LOCALE_DIVIDER:
                    resourceId = LOCALE_DIVIDER_LAYOUT;
                    enabled = false;
                    locale = null;
                    label = null;
                    break;
                case LOCALE_ITEM:
                    resourceId = LOCALE_ITEM_LAYOUT;
                    enabled = true;
                    break;
                default:
            }
        }

        public LocaleInfo(String label, Locale locale) {
            this(LOCALE_ITEM);
            this.label = label;
            this.locale = locale;
        }

        public LocaleInfo() {
            this(LOCALE_DIVIDER);
        }

        public String getLabel() {
            return label;
        }

        public Locale getLocale() {
            return locale;
        }

        public boolean getEnabled() {
            return enabled;
        }

        public int getType() {
            return type;
        }

        public int getResourceId() {
            return resourceId;
        }

        @Override
        public String toString() {
            return this.label;
        }

        @Override
        public int compareTo(LocaleInfo another) {
            return sCollator.compare(this.label, another.label);
        }

        @Override
        public int hashCode() {
            return (label != null) ? label.hashCode() : 0;
        }

        @Override
        public boolean equals(Object another) {
            if (!(another instanceof LocaleInfo)) {
                return false;
            }
            try {
                return compareTo((LocaleInfo)another) == 0;
            } catch (NullPointerException e) {
                return false;
            }
        }
    }

    private static String toTitleCase(String s) {
        if (s.length() == 0) {
            return s;
        }

        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private String getDisplayName(Locale l) {
        String code = l.toString();
        for (int i = 0; i < mSpecialLocaleCodes.length; i++) {
            if (mSpecialLocaleCodes[i].equals(code)) {
                return mSpecialLocaleNames[i];
            }
        }

        return l.getDisplayName(l);
    }

    private String getDisplayLanguage(Locale l) {
        String code = l.toString();

        for (int i = 0; i < mSpecialLocaleLanguageCodes.length; i++) {
            if (mSpecialLocaleLanguageCodes[i].equals(code)) {
                return mSpecialLocaleLanguageNames[i];
            }
        }

        return l.getDisplayLanguage(l);
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        mSpecialLocaleLanguageCodes = getResources().getStringArray(
                R.array.special_locale_language_codes);
        mSpecialLocaleLanguageNames = getResources().getStringArray(
                R.array.special_locale_language_names);
        mSpecialLocaleCodes = getResources().getStringArray(
                R.array.special_locale_codes);
        mSpecialLocaleNames = getResources().getStringArray(
                R.array.special_locale_names);

        setUpDefaultLocales();

        // Build the list and set adapter
        mAdapter = new LocaleAdapter(getActivity(), buildLanguageList(getResources().
                getAssets().getLocales()));
        setListAdapter(mAdapter);
    }

    public void setLocaleSelectionListener(LocaleSelectionListener listener) {
        mListener = listener;
    }

    @Override
    public void onResume() {
        super.onResume();
        final int position = mAdapter.getPosition(Locale.getDefault());
        if (position >= 0) {
            getListView().setItemChecked(mAdapter.getPosition(Locale.getDefault()), true);
        }
        getListView().requestFocus();
    }

    /**
     * Set up the locales from which the top language(s) should be picked.
     */
    protected void setUpDefaultLocales() {
    }

    /**
     * Get position of selected item in list view.
     */
    protected int getListPositionOfLocale(Locale l) {
        return mAdapter.getPosition(l);
    }

    /**
     * Build the language list. LocaleRecords should be displayed with country part
     * as well as language part if language is already added. Decide on what language(s)
     * to be on the top of the list in the following order:
     *
     * <ol>
     * <li>Customized value</li>
     * <li>Sim card country, all languages in that country are selected</li>
     * <li>The software locale read from SystemProperties</li>
     * <li>The default locale US english</li>
     * </ol>
     *
     * @param locales All locales installed
     * @return the sorted array with Sim default language on top
     */
    private List<LocaleInfo> buildLanguageList(String[] locales) {

        final ArrayList<LocaleInfo> localeList = new ArrayList<LocaleInfo>();

        Arrays.sort(locales);

        boolean isSwLocaleValid = false;
        boolean isSimCountryValid = false;
        boolean topLanguageFound = false;

        for (String s : locales) {
            if (s.length() == LOCALE_STRING_LENGTH) {
                final String language = getLanguagePart(s);
                final String country = getCountryPart(s);
                final Locale l = new Locale(language, country);
                final LocaleInfo loc = new LocaleInfo(toTitleCase(getDisplayLanguage(l)), l);

                if (!localeList.isEmpty()) {
                    // If the previous entry had the same language as this entry
                    // the expand the language of this entry and the previous entry
                    // to the full language, country title.
                    LocaleInfo p = localeList.get(localeList.size() - 1);
                    if (l.getLanguage().equals(p.getLocale().getLanguage())) {
                        loc.label = toTitleCase(getDisplayName(l));
                        p.label = toTitleCase(getDisplayName(p.getLocale()));
                    }
                }
                localeList.add(loc);

                if (isDefaultLocale(l)) {
                    topLanguageFound = true;
                }
                // Check if sw locale and sim country is valid in case we need
                // to switch states.
                if (!isSwLocaleValid) {
                    isSwLocaleValid = language.equals(mDefaultSwLanguage)
                            && country.equals(mDefaultSwCountry);
                }
                if (!isSimCountryValid) {
                    isSimCountryValid = language.equals(mDefaultSimCountry);
                }
            }
        }

        if (!topLanguageFound) {
            // This is the case where we did not get any default languages.
            changeStateWhenNoDefaultLocale(isSwLocaleValid, isSimCountryValid);
        }

        Collections.sort(localeList);

        // Scan in reverse direction so that moving items to the top
        // keeps them sorted in alphabetical order. If a default locale
        // is found, the divider limit is moved upwards, otherwise the
        // current location is moved downwards.
        int divider = 0;
        LocaleInfo defaultLocale = null;
        int i = localeList.size() - 1;
        while (i >= divider) {
            final LocaleInfo loc = localeList.get(i);
            final Locale l = loc.getLocale();
            if (isDefaultLocale(l)) {
                localeList.remove(loc);
                localeList.add(0, loc);
                divider++;
            } else {
                i--;
                if (defaultLocale == null && Locale.getDefault().equals(l)) {
                    defaultLocale = loc;
                }
            }
        }

        // Move a customized locale top language to the top
        if (divider != 0 && mState == STATE_CUSTOMIZED_LOCALE_TOP) {
            final Locale l = new Locale(mCustomizedLanguage,
                                        mCustomizedCountry);
            for (i = 0; i < divider; i++) {
                LocaleInfo loc = localeList.get(i);
                if (l.equals(loc.getLocale())) {
                    localeList.remove(loc);
                    localeList.add(0, loc);
                    break;
                }
            }
        }

        // Add a divider
        localeList.add(divider, new LocaleInfo());
        if (defaultLocale != null) {
            localeList.remove(defaultLocale);
            localeList.add(divider, defaultLocale);
        }
        return localeList;
    }

    /**
     * Change state since we had no default languages.
     * @param isSwLocaleValid true if we have a valid sw locale
     * @param isSimCountryValid true if a valid simcountry is reported
     */
    private void changeStateWhenNoDefaultLocale(boolean isSwLocaleValid,
            boolean isSimCountryValid) {
        switch (mState) {
        case STATE_CUSTOMIZED_LOCALE_TOP:
        case STATE_CUSTOMIZED_LOCALE_SINGLE:
            if (isSimCountryValid) {
                setState(STATE_SIMCARD_COUNTRY);
            } else if (isSwLocaleValid) {
                setState(STATE_SW_LOCALE);
            } else {
                setState(STATE_DEFAULT_LOCALE);
            }
            break;
        case STATE_SIMCARD_COUNTRY:
            if (isSwLocaleValid) {
                setState(STATE_SW_LOCALE);
            } else {
                setState(STATE_DEFAULT_LOCALE);
            }
            break;
        case STATE_SW_LOCALE:
            setState(STATE_DEFAULT_LOCALE);
            break;
        default:
            // fall through
            Log.e(TAG, "Error, wrong state. Defaulting to resolve default locale");
            setState(STATE_DEFAULT_LOCALE);
            break;
        }
    }

    protected void setState(int state) {
        mState = state;
    }

    /**
     * Check if the locale is a default locale depending on state.
     * @param locale the locale to check
     * @return true if locale is a default locale.
     */
    private boolean isDefaultLocale(Locale locale) {
        boolean isDefaultLocale = false;
        if (locale != null) {
            switch (mState) {
            case STATE_CUSTOMIZED_LOCALE_SINGLE:
                isDefaultLocale =  locale.getCountry().equals(mCustomizedCountry) &&
                                   locale.getLanguage().equals(mCustomizedLanguage);
                break;
            case STATE_CUSTOMIZED_LOCALE_TOP:
                isDefaultLocale = locale.getCountry().equals(mCustomizedCountry);
                break;
            case STATE_SIMCARD_COUNTRY:
                isDefaultLocale = locale.getCountry().equals(mDefaultSimCountry);
                break;
            case STATE_SW_LOCALE:
                isDefaultLocale = locale.getCountry().equals(mDefaultSwCountry) &&
                                  locale.getLanguage().equals(mDefaultSwLanguage);
                break;
            case STATE_DEFAULT_LOCALE:
                isDefaultLocale = locale.equals(Locale.US);
                break;
            default:
                Log.e(TAG, "Error, unsupported state.");
                break;
            }
        }
        return isDefaultLocale;
    }

    /**
     * Each listener needs to call {@link #updateLocale(Locale)} to actually change the locale.
     *
     * We don't call {@link #updateLocale(Locale)} automatically, as it halt the system for
     * a moment and some callers won't want it.
     */
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if (mListener != null) {
            l.setItemChecked(position, true);
            final Locale locale = mAdapter.getLocale(position);
            mListener.onLocaleSelected(locale);
        }
    }

    /**
     * Requests the system to update the system locale. Note that the system looks halted
     * for a while during the Locale migration, so the caller need to take care of it.
     */
    public static void updateLocale(Locale locale) {
        try {
            IActivityManager am = ActivityManagerNative.getDefault();
            Configuration config = am.getConfiguration();

            config.locale = locale;

            // indicate this isn't some passing default - the user wants this remembered
            config.userSetLocale = true;

            am.updateConfiguration(config);
            // Trigger the dirty bit for the Settings Provider.
            BackupManager.dataChanged("com.android.providers.settings");
        } catch (RemoteException e) {
            // Intentionally left blank
        }
    }

    /**
     * Get the country part from a locale string of type en_US
     * @param s the locale string
     * @return the country part of the string.
     */
    protected final String getCountryPart(String s) {
        return s.substring(3, 5);
    }

    /**
     * Get the language part from a locale string of type en_US
     * @param s the locale string
     * @return the country part of the string.
     */
    protected final String getLanguagePart(String s) {
        return s.substring(0, 2);
    }

    /** The customized language. */
    protected final void setCustomizedLanguage(String customizedLanguage) {
        mCustomizedLanguage = customizedLanguage;
    }

    /** The customized language. */
    protected final void setCustomizedCountry(String customizedCountry) {
        mCustomizedCountry = customizedCountry;
    }

}
