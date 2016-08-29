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

import static org.junit.Assert.assertEquals;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Xml;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.core.PreferenceXmlParserUtils;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

@RunWith(SettingsRobolectricTestRunner.class)
public class ListWithEntrySummaryPreferenceTest {

    private Context mContext;
    private ListWithEntrySummaryPreference mPreference;
    private String[] mListTitles;
    private String[] mListValues;
    private String[] mListSummaries;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        AttributeSet attrs = getAttributeSet(R.xml.wifi_calling_settings, "wifi_calling_mode");
        mPreference = new ListWithEntrySummaryPreference(mContext, attrs);
        Resources res = mContext.getResources();
        mListTitles = res.getStringArray(R.array.wifi_calling_mode_choices);
        mListValues = res.getStringArray(R.array.wifi_calling_mode_values);
        mListSummaries = res.getStringArray(R.array.wifi_calling_mode_summaries);
    }

    @Test
    public void test_createNewPreference_shouldDefalutLayout() {
        AlertDialog dialog = showDialog(mPreference);
        ListAdapter adapter = dialog.getListView().getAdapter();
        for (int i = 0; i < mListTitles.length; i++) {
            TextView title = adapter.getView(i, null, null).findViewById(R.id.title);
            TextView summary = adapter.getView(i, null, null).findViewById(R.id.summary);
            assertEquals("bad title text", mListTitles[i], title.getText());
            assertEquals("bad summary text", mListSummaries[i], summary.getText());
        }
    }

    @Test
    public void test_setEntrySummaries_shouldSummaryTextUpdate() {
        mPreference.setEntries(R.array.wifi_calling_mode_choices_without_wifi_only);
        mPreference.setEntryValues(R.array.wifi_calling_mode_values_without_wifi_only);
        mPreference.setEntrySummaries(R.array.wifi_calling_mode_summaries_without_wifi_only);
        AlertDialog dialog = showDialog(mPreference);
        ListAdapter adapter = dialog.getListView().getAdapter();
        String[] listSummaries = mContext.getResources()
                .getStringArray(R.array.wifi_calling_mode_summaries_without_wifi_only);
        for (int i = 0; i < listSummaries.length; i++) {
            TextView summary = adapter.getView(i, null, null).findViewById(R.id.summary);
            assertEquals("bad summary text", listSummaries[i], summary.getText());
        }
    }

    @Test
    public void test_onSaveAndRestoreInstanceState_shouldViewListNotChanged() {
        test_setEntrySummaries_shouldSummaryTextUpdate();

        final Parcelable parcelable = mPreference.onSaveInstanceState();
        AttributeSet attrs = getAttributeSet(R.xml.wifi_calling_settings, "wifi_calling_mode");
        ListWithEntrySummaryPreference preference
                = new ListWithEntrySummaryPreference(mContext, attrs);
        preference.onRestoreInstanceState(parcelable);
        AlertDialog dialog = showDialog(preference);

        Resources res = mContext.getResources();
        String[] listTitles = mContext.getResources()
                .getStringArray(R.array.wifi_calling_mode_choices_without_wifi_only);
        String[] listSummaries = mContext.getResources()
                .getStringArray(R.array.wifi_calling_mode_summaries_without_wifi_only);
        ListAdapter adapter = dialog.getListView().getAdapter();
        for (int i = 0; i < listTitles.length; i++) {
            TextView title = adapter.getView(i, null, null).findViewById(R.id.title);
            TextView summary = adapter.getView(i, null, null).findViewById(R.id.summary);
            assertEquals("bad title text", listTitles[i], title.getText());
            assertEquals("bad summary text", listSummaries[i], summary.getText());
        }
    }

    private AttributeSet getAttributeSet(int xmlId, String key) {
        final XmlResourceParser parser = mContext.getResources().getXml(xmlId);
        AttributeSet attrs = null;
        String datakey;
        try {
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                try {
                    datakey = PreferenceXmlParserUtils.getDataKey(mContext,
                            Xml.asAttributeSet(parser));
                    if (key.equals(datakey)) {
                        attrs = Xml.asAttributeSet(parser);
                        break;
                    }
                } catch (NullPointerException e) {
                    continue;
                } catch (Resources.NotFoundException e) {
                    continue;
                }
            }
        } catch (java.io.IOException e) {
            return null;
        } catch (XmlPullParserException e) {
            return null;
        }

        return attrs;
    }

    private AlertDialog showDialog(ListWithEntrySummaryPreference preference) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        preference.onPrepareDialogBuilder(builder, null);
        return builder.show();
    }
}
