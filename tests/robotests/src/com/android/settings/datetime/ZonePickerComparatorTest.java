package com.android.settings.datetime;

import com.android.settings.datetime.ZonePicker;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settingslib.datetime.ZoneGetter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import static com.google.common.truth.Truth.assertThat;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class ZonePickerComparatorTest {

    private static final int NUM = 5;
    private List<Map<String, Object>> mTestZonesList;
    private final String[] mTestDisplayName =
            new String[]{"中国标准时间", "纽约", "悉尼", "伦敦", "洛杉矶"};
    private final String[] mOrderedDisplayName =
            new String[]{"伦敦", "洛杉矶", "纽约", "悉尼", "中国标准时间"};

    private List<Map<String, Object>> getMockZonesList() {
        List<Map<String, Object>> zones = new ArrayList<>();
        TimeZone tz = TimeZone.getDefault();
        for (int i = 0; i < NUM; i++) {
            zones.add(createMockDisplayEntry(tz, "GMT+08:00", mTestDisplayName[i], NUM - i - 1));
        }
        return zones;
    }

    private Map<String, Object> createMockDisplayEntry(
            TimeZone tz, CharSequence gmtOffsetText, CharSequence displayName, int offsetMillis) {
        Map<String, Object> map = new HashMap<>();
        map.put(ZoneGetter.KEY_ID, tz.getID());
        map.put(ZoneGetter.KEY_DISPLAYNAME, displayName.toString());
        map.put(ZoneGetter.KEY_DISPLAY_LABEL, displayName);
        map.put(ZoneGetter.KEY_GMT, gmtOffsetText.toString());
        map.put(ZoneGetter.KEY_OFFSET_LABEL, gmtOffsetText);
        map.put(ZoneGetter.KEY_OFFSET, offsetMillis);
        return map;
    }

    @Before
    public void getTestList() {
        mTestZonesList = getMockZonesList();
    }

    @Test
    public void testComparator_sortString() {
        String sortKey = ZoneGetter.KEY_DISPLAY_LABEL;
        final ZonePicker.MyComparator comparator = new ZonePicker.MyComparator(sortKey);
        assertThat(comparator).isNotNull();
        Collections.sort(mTestZonesList, comparator);
        for (int i = 0; i < NUM; i++) {
            assertThat(mTestZonesList.get(i).get(sortKey).toString())
                    .isEqualTo(mOrderedDisplayName[i]);
        }
    }

    @Test
    public void testComparator_sortInteger() {
        String sortKey = ZoneGetter.KEY_OFFSET;
        final ZonePicker.MyComparator comparator = new ZonePicker.MyComparator(sortKey);
        assertThat(comparator).isNotNull();
        Collections.sort(mTestZonesList, comparator);
        for (int i = 0; i < NUM; i++) {
            assertThat(mTestZonesList.get(i).get(sortKey)).isEqualTo(i);
        }
    }
}
