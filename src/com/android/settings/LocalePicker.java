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

import com.android.settings.LocaleListFragment;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.Context;
import android.os.Bundle;
import android.os.SystemProperties;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import java.util.Locale;

/**
 * This class lists all installed locales and sets the locale of choice.
 * On the top of the list there are locale(s) that are probable choices for
 * the end user. They are determined this way:
 * <ol>
 * <li>The currently set locale will always be added on top.</li>
 * <li>The customization reports a language to be on top. All locales that have the same
 *     country will be sorted after this locale if not specified
 *     otherwise by customization</li>
 * <li>The sim card reports a country and the top language list items are
 *     chosen according to this country.</li>
 * <li>If there is no simcard inserted or simcard is not in a ready state.
 *     Default to the locale reported by SystemProperties ro.product.locale.language
 *     and ro.product.locale.region.</li>
 *     If they don't make up an existing locale we default to US English
 * <li>If the sim card or customization reports a country that does not exist in the
 *     current locale set. Same as (2).</li>
 * <li>If the default locale is not supported. Same as (2).</li>
 * </ol>
 */
public class LocalePicker extends LocaleListFragment
        implements LocaleListFragment.LocaleSelectionListener {

    /** Instance state save country */
    private static final String SAVED_COUNTRY = "savedCountry";
    /** Instance state save language */
    private static final String SAVED_LANGUAGE = "savedLanguage";

    private Locale mLocaleAtStart;
    private Locale mLocaleSelected;
    private Button mDoneButton;

    public LocalePicker() {
        super();
        setLocaleSelectionListener(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mLocaleSelected = new Locale(savedInstanceState.getString(SAVED_LANGUAGE),
                    savedInstanceState.getString(SAVED_COUNTRY));
        } else {
            Settings activity = (Settings) getActivity();
            mLocaleSelected = activity.getResources().getConfiguration().locale;
            mLocaleAtStart = mLocaleSelected;
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Settings activity = (Settings)getActivity();
        if (!activity.hasNextButton()) {
            RelativeLayout buttonBar = (RelativeLayout)activity
                    .findViewById(com.android.internal.R.id.button_bar);
            buttonBar.setVisibility(View.VISIBLE);
            buttonBar.findViewById(com.android.internal.R.id.back_button).setVisibility(View.GONE);
            buttonBar.findViewById(com.android.internal.R.id.next_button).setVisibility(View.GONE);
            buttonBar.findViewById(com.android.internal.R.id.skip_button).setVisibility(View.GONE);
            activity.getLayoutInflater().inflate(R.layout.locale_picker_done_button, buttonBar);
            mDoneButton = (Button) buttonBar.findViewById(R.id.locale_picker_done_button);
            mDoneButton.setText(getResources().getString(R.string.locale_picker_done_label));
            mDoneButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    selectLocale();
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getListView().setItemChecked(getListPositionOfLocale(mLocaleSelected), true);
        onLocaleSelected(mLocaleSelected);
        getListView().requestFocus();
    }

    @Override
    public void onLocaleSelected(Locale locale) {
        mLocaleSelected = locale;

        if (mDoneButton != null) {
            Settings activity = (Settings) getActivity();
            Resources res = activity.getResources();
            Configuration localeConf = new Configuration(res.getConfiguration());
            localeConf.locale = locale;
            Resources localeRes = new Resources(res.getAssets(), res.getDisplayMetrics(),
                    localeConf);

            // Only change language of title and button while in language picker
            mDoneButton.setText(localeRes.getString(R.string.locale_picker_done_label));
            activity.setTitle(localeRes.getString(R.string.phone_language));

            // Restore to startup language, needed if pressing back key
            localeConf.locale = mLocaleAtStart;

            // This is needed for the restore language to actually take effect
            new Resources(res.getAssets(), res.getDisplayMetrics(),
                    localeConf);
        } else {
            // Keep old behavior
            selectLocale();
        }
    }

    private void selectLocale() {
        getActivity().onBackPressed();
        LocaleListFragment.updateLocale(mLocaleSelected);
    }

    /**
     * Set up the locales from which the top language(s) should be picked.
     */
    @Override
    protected void setUpDefaultLocales() {
        TelephonyManager manager = getTelephonyManager();
        boolean isSimReady = isSimStateReady(manager);

        // Get the sim country
        mDefaultSimCountry = isSimReady ? getDefaultSimCountry(manager) : null;
        mDefaultSwLanguage = SystemProperties.get(PROPERTY_LANGUAGE, Locale.US.getLanguage());
        mDefaultSwCountry = SystemProperties.get(PROPERTY_COUNTRY, Locale.US.getCountry());

        // Get the customized values
        String custLocaleStr = getResources().getString(R.string.config_defaultLocale);

        if (custLocaleStr != null && custLocaleStr.length() == LOCALE_STRING_LENGTH) {
            setCustomizedCountry(getCountryPart(custLocaleStr));
            setCustomizedLanguage(getLanguagePart(custLocaleStr));

            boolean custLocaleSortOrderSingle = getResources().getBoolean(
                    R.bool.config_localeUseSingleSortOrder);
            if (custLocaleSortOrderSingle) {
                setState(STATE_CUSTOMIZED_LOCALE_SINGLE);
            } else {
                setState(STATE_CUSTOMIZED_LOCALE_TOP);
            }
        } else if (isSimReady) {
            setState(STATE_SIMCARD_COUNTRY);
        } else {
            setState(STATE_SW_LOCALE);
        }
    }

    private TelephonyManager getTelephonyManager() {
        Context context = getActivity();
        return (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
    }

    /**
     * Check if the sim card is in a ready state.
     * @param manager
     * @return true if the sim card is ready.
     */
    private boolean isSimStateReady(TelephonyManager manager) {
        return manager != null &&
            manager.getSimState() == TelephonyManager.SIM_STATE_READY;
    }

    /**
     * Read the default sim country reported by sim card.
     * @param manager
     * @return the country two letter string in upper case: GB, SE, US
     */
    private String getDefaultSimCountry(TelephonyManager manager) {
        return manager.getSimCountryIso().toUpperCase(Locale.US);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putString(SAVED_LANGUAGE, mLocaleSelected.getLanguage());
        savedInstanceState.putString(SAVED_COUNTRY, mLocaleSelected.getCountry());
        super.onSaveInstanceState(savedInstanceState);
    }
}
