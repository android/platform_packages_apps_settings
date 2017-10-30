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

    private static final int NUM = 6;

    // Strings in Chinese are sorted by alphabet order of their Pinyin.
    // "伦敦" -> "lundun";  "纽约" -> "niuyue";  "悉尼" -> "xini"
    // "开罗" -> "kailuo";  "雅典" -> "yadian";  "上海" -> "shanghai"
    private static final String[] TEST_CHINESE_NAME =
            new String[]{"伦敦", "纽约", "悉尼", "开罗", "雅典", "上海"};
    private static final String[] ORDERED_CHINESE_NAME =
            new String[]{"开罗", "伦敦", "纽约", "上海", "悉尼", "雅典"};

    private static final String[] TEST_ENGLISH_NAME =
            new String[]{"London", "New York", "Sydney", "Cairo", "Athens", "Shanghai"};
    private static final String[] ORDERED_ENGLISH_NAME =
            new String[]{"Athens", "Cairo", "London", "New York", "Shanghai", "Sydney"};

    private List<Map<String, Object>> mTestChineseList;
    private List<Map<String, Object>> mTestEnglishList;

    private List<Map<String, Object>> getMockZonesList(boolean testCN) {
        List<Map<String, Object>> zones = new ArrayList<>();
        TimeZone tz = TimeZone.getDefault();
        for (int i = 0; i < NUM; i++) {
            zones.add(createMockDisplayEntry(tz, "GMT+08:00",
                    testCN ? TEST_CHINESE_NAME[i] : TEST_ENGLISH_NAME[i], NUM - i - 1));
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
    public void setUp() {
        mTestChineseList = getMockZonesList(true);
        mTestEnglishList = getMockZonesList(false);
    }

    @Test
    public void testComparator_sortChineseString() {
        String sortKey = ZoneGetter.KEY_DISPLAY_LABEL;
        final ZonePicker.MyComparator comparator = new ZonePicker.MyComparator(sortKey);
        assertThat(comparator).isNotNull();
        Collections.sort(mTestChineseList, comparator);
        for (int i = 0; i < NUM; i++) {
            assertThat(mTestChineseList.get(i).get(sortKey).toString())
                    .isEqualTo(ORDERED_CHINESE_NAME[i]);
        }
    }

    @Test
    public void testComparator_sortEnglishString() {
        String sortKey = ZoneGetter.KEY_DISPLAY_LABEL;
        final ZonePicker.MyComparator comparator = new ZonePicker.MyComparator(sortKey);
        assertThat(comparator).isNotNull();
        Collections.sort(mTestEnglishList, comparator);
        for (int i = 0; i < NUM; i++) {
            assertThat(mTestEnglishList.get(i).get(sortKey).toString())
                    .isEqualTo(ORDERED_ENGLISH_NAME[i]);
        }
    }

    @Test
    public void testComparator_sortInteger() {
        String sortKey = ZoneGetter.KEY_OFFSET;
        final ZonePicker.MyComparator comparator = new ZonePicker.MyComparator(sortKey);
        assertThat(comparator).isNotNull();
        Collections.sort(mTestEnglishList, comparator);
        for (int i = 0; i < NUM; i++) {
            assertThat(mTestEnglishList.get(i).get(sortKey)).isEqualTo(i);
        }
    }
}
