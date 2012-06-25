package com.android.settings;

import com.android.settings.LocaleAdapter;
import com.android.settings.Settings;
import com.android.settings.R;
import com.android.settings.LocaleListFragment;
import com.android.settings.LocaleListFragment.LocaleInfo;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.preference.PreferenceActivity;
import android.telephony.TelephonyManager;
import android.test.ActivityInstrumentationTestCase2;
import android.test.TouchUtils;
import android.test.UiThreadTest;
import android.util.Log;
import android.widget.Button;
import android.widget.ListView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Locale;

public class LocalePickerTest extends
        ActivityInstrumentationTestCase2<Settings.LocalePickerActivity> {

    private static final String UNSUPPORTED_COUNTRY = "XY"; // XYLotopia
    private static final String SUPPORTED_SINGLE_COUNTRY = "SE"; // Sweden
    private static final String SUPPORTED_SINGLE_LANG = "sv"; // Swedish
    private static final String SUPPORTED_MULTIPLE_COUNTRY = "CA"; // Canada
    private static final String SUPPORTED_MULTIPLE_LANG_EN = "en"; // English
    private static final String SUPPORTED_MULTIPLE_LANG_FR = "fr"; // French

    private static final String CUSTOMIZED_COUNTRY = "ES"; // Spain
    private static final String CUSTOMIZED_LANGUAGE = "es"; // Spanish

    // Reflection of adapter view types
    private static final int LOCALE_DIVIDER = 0;
    private static final int LOCALE_ITEM = 1;

    // Reflection of state constants.
    private static final int STATE_SIMCARD_COUNTRY = 0;
    private static final int STATE_SW_LOCALE = 1;
    private static final int STATE_DEFAULT_LOCALE = 2;
    private static final int STATE_CUSTOMIZED_LOCALE_SINGLE = 3;
    private static final int STATE_CUSTOMIZED_LOCALE_TOP = 4;


    private static final String TAG = "LocalePickerTest";

    // Test locales
    private final String[] locales = {"en_CA", "fr_CA", "uk_UA", "en_ZA", "en_GB",
                                      "id_ID", "en_IE", "bg_BG", "en_SG", "fi_FI",
                                      "sl_SI", "sk_SK", "en_IN", "ro_RO", "pt_BR",
                                      "hr_HR", "ca_ES", "es_ES", "sr_RS", "en_US",
                                      "es_US", "lt_LT", "pt_PT", "en_AU", "hu_HU",
                                      "lv_LV", "en_NZ", "nl_BE", "fr_BE", "de_DE",
                                      "sv_SE", "de_CH", "fr_CH", "it_CH", "tl_PH",
                                      "de_LI", "da_DK", "nl_NL", "pl_PL", "nb_NO",
                                      "fr_FR", "el_GR", "tr_TR", "de_AT", "it_IT",
                                      "ru_RU", "cs_CZ"};

    private PreferenceActivity oldLocalePicker;
    private Locale mCurrentLocale;

    private LocalePicker mLocalePicker;

    public LocalePickerTest() {
        super(Settings.LocalePickerActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        oldLocalePicker = getActivity();
        FragmentManager manager = oldLocalePicker.getFragmentManager();
        Fragment fragment = manager.findFragmentById(com.android.internal.R.id.prefs);
        mLocalePicker = (LocalePicker) fragment;
        mCurrentLocale = Locale.getDefault();
        assertNotNull("Could not get Activity LocalePicker.", mLocalePicker);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        oldLocalePicker = null;
    }

    /**
     * Setup the adapter with predefined values.
     *
     * @param state The state the LocalePicker is in.
     * @return The adapter with LocaleRecords
     */
    private LocaleAdapter setUpAdapterForTest(int state) {
        setMember(LocaleListFragment.class, mLocalePicker, "mState",
                new Integer(state));
        ArrayList<LocaleInfo> localeInfo = (ArrayList<LocaleInfo>)
                callMethod(LocaleListFragment.class, mLocalePicker,
                        "buildLanguageList", (Object[])locales);
        return new LocaleAdapter(oldLocalePicker, localeInfo);
    }

    private void setUpDefaultSimCountry(String simCountry) {
        setMember(LocaleListFragment.class, mLocalePicker, "mDefaultSimCountry",
                simCountry);
    }

    /**
     * Setup the default SystemProperties locale data.
     * @param language The language spoken
     * @param country The country
     */
    private void setUpSwDefaultLocaleData(String language, String country) {
        setMember(LocaleListFragment.class, mLocalePicker, "mDefaultSwLanguage",
                language);
        setMember(LocaleListFragment.class, mLocalePicker, "mDefaultSwCountry",
                country);
    }

    /**
     * Setup the customized values.
     *
     * @param language the customized language.
     * @param country the customized country
     */
    private void setUpCustomizedValues(String language, String country) {
        setMember(LocaleListFragment.class, mLocalePicker, "mCustomizedCountry",
                country);
        setMember(LocaleListFragment.class, mLocalePicker,
                "mCustomizedLanguage", language);
    }

    // Count the number of items before the divider item
    private int getNbrOfLanguagesOnTop() {
        int nbrOfLanguagesOnTop;
        final int size = mLocalePicker.getListAdapter().getCount();
        int type = -1;
        for (nbrOfLanguagesOnTop = 0; nbrOfLanguagesOnTop < size; nbrOfLanguagesOnTop++) {
            type = mLocalePicker.getListAdapter().getItemViewType(nbrOfLanguagesOnTop);
            if (type == LOCALE_DIVIDER) {
                break;
            }
        }
        return nbrOfLanguagesOnTop;
    }

    /**
     * Test that number of locales are correct in the adapter.
     */
    @UiThreadTest
    public void testNumberOfLocales() throws Exception {
        LocaleAdapter adapter = setUpAdapterForTest(STATE_DEFAULT_LOCALE);
        // -1 since the divider should be present
        assertEquals("Not the same number of locales", locales.length, adapter.getCount() - 1);
    }

    /**
     * Test that the correct language is checked.
     */
    public void testCheckedLocale() throws Exception {
        Locale localeExpected = Locale.getDefault();
        ListView listView = mLocalePicker.getListView();
        int checkedPosition = listView.getCheckedItemPosition();
        LocaleAdapter adapter = (LocaleAdapter)listView.getAdapter();
        // Get locale at checkedPosition
        Locale localeActual = adapter.getLocale(checkedPosition);
        assertEquals("Wrong locale is checked.", localeExpected, localeActual);
    }

    /**
     * Test setting country where a single language is spoken.
     * Language on top should be Swedish.
     */
    @UiThreadTest
    public void testSupportedSingleSimLanguageOnTop() throws Exception {
        Locale expected = new Locale(SUPPORTED_SINGLE_LANG, SUPPORTED_SINGLE_COUNTRY);
        setUpDefaultSimCountry(SUPPORTED_SINGLE_COUNTRY);
        LocaleAdapter adapter = setUpAdapterForTest(STATE_SIMCARD_COUNTRY);
        mLocalePicker.setListAdapter(adapter);
        adapter.notifyDataSetChanged();

        final int nbrOfDefaultLanguages = getNbrOfLanguagesOnTop();
        // Assert that default locale and default sim country locale(s) should be on top
        assertEquals("Top locale is wrong", expected, adapter.getLocale(0));
        for (int i = 0; i < nbrOfDefaultLanguages; i++) {
            Locale l = adapter.getLocale(i);
            assertTrue("Wrong default locale among the top locales",
                l.equals(mCurrentLocale) || l.getCountry().equals(SUPPORTED_SINGLE_COUNTRY));
        }
        // Verify that the country is not represented elsewhere
        for (int i = nbrOfDefaultLanguages + 1; i < adapter.getCount(); i++) {
            if (adapter.getItemViewType(i) == LOCALE_ITEM) {
                assertFalse("Country is wrong.",
                    adapter.getLocale(i).getCountry().equals(SUPPORTED_SINGLE_COUNTRY));
            }
        }
    }

    /**
     * Test setting a country where multiple languages are spoken.
     * Language on top should be French(Canada) and English(Canada).
     */
    @UiThreadTest
    public void testSupportedMultipleSimLanguagesOnTop() throws Exception {
        setUpDefaultSimCountry(SUPPORTED_MULTIPLE_COUNTRY);
        LocaleAdapter adapter = setUpAdapterForTest(STATE_SIMCARD_COUNTRY);
        mLocalePicker.setListAdapter(adapter);
        adapter.notifyDataSetChanged();
        final int nbrOfDefaultLanguages = getNbrOfLanguagesOnTop();
        // Assert that default locale and default sim country locale(s) should be on top
        for (int i = 0; i < nbrOfDefaultLanguages; i++) {
            Locale l = adapter.getLocale(i);
            assertTrue("Wrong default locale among the top locales",
                l.equals(mCurrentLocale) || l.getCountry().equals(SUPPORTED_MULTIPLE_COUNTRY));
        }
        // Verify that the country is not represented elsewhere.
        String country;
        for (int i = nbrOfDefaultLanguages + 1; i < adapter.getCount(); i++) {
            if (adapter.getItemViewType(i) == LOCALE_ITEM) {
                country = adapter.getLocale(i).getCountry();
                assertFalse("Country is wrong.",
                    SUPPORTED_MULTIPLE_COUNTRY.equals(country));
            }
        }
    }

    /**
     * Test setting a language as sim default that is not supported.
     * Language on top should be swedish since it is set as
     * software default locale.
     */
    @UiThreadTest
    public void testUnsupportedSimCountry() throws Exception {
        Locale expected = new Locale(SUPPORTED_SINGLE_LANG, SUPPORTED_SINGLE_COUNTRY);
        // Setup swedish as sw default locale
        setUpSwDefaultLocaleData(SUPPORTED_SINGLE_LANG, SUPPORTED_SINGLE_COUNTRY);
        // default sim country not supported
        setUpDefaultSimCountry(UNSUPPORTED_COUNTRY);

        LocaleAdapter adapter = setUpAdapterForTest(STATE_SIMCARD_COUNTRY);
        mLocalePicker.setListAdapter(adapter);
        adapter.notifyDataSetChanged();

        final int nbrOfDefaultLanguages = getNbrOfLanguagesOnTop();
        // Assert that default locale and default sim country locale(s) should be on top
        for (int i = 0; i < nbrOfDefaultLanguages; i++) {
            Locale l = adapter.getLocale(i);
            assertTrue("Wrong default locale among the top locales",
                l.equals(mCurrentLocale) || l.equals(expected));
        }

        // Check if locale is represented elsewhere
        for (int i = nbrOfDefaultLanguages + 1; i < adapter.getCount(); i++) {
            if (adapter.getItemViewType(i) == LOCALE_ITEM) {
                assertFalse("Locale has duplicates.", adapter.getLocale(i).equals(expected));
            }
        }
    }

    /**
     * Test the sim card not inserted and the software language is not supported.
     * Language on top should be US English
     */
    @UiThreadTest
    public void testUnsupportedSimCountryAndSwLanguage() throws Exception {
        Locale expected = Locale.US;
        // Setup unsupported sw default
        setUpSwDefaultLocaleData(SUPPORTED_SINGLE_LANG, UNSUPPORTED_COUNTRY); // sv_ZX
        setUpDefaultSimCountry(UNSUPPORTED_COUNTRY);

        LocaleAdapter adapter = setUpAdapterForTest(STATE_SIMCARD_COUNTRY);
        mLocalePicker.setListAdapter(adapter);
        adapter.notifyDataSetChanged();

        final int nbrOfDefaultLanguages = getNbrOfLanguagesOnTop();
        // Assert that default locale and default sim country locale(s) should be on top
        for (int i = 0; i < nbrOfDefaultLanguages; i++) {
            Locale l = adapter.getLocale(i);
            assertTrue("Wrong default locale among the top locales",
                l.equals(mCurrentLocale) || l.equals(expected));
        }

        // Verify that the locale is not represented elsewhere.
        for (int i = nbrOfDefaultLanguages + 1; i < adapter.getCount(); i++) {
            if (adapter.getItemViewType(i) == LOCALE_ITEM) {
                assertFalse("Locale is duplicate.", adapter.getLocale(i).equals(expected));
            }
        }
    }

    /**
     * Test the customized locale on top alone.
     * @throws Exception
     */
    @UiThreadTest
    public void testCustomizedValueOnTopSingle() throws Exception {
        Locale expected = new Locale(CUSTOMIZED_LANGUAGE, CUSTOMIZED_COUNTRY);
        setUpCustomizedValues(CUSTOMIZED_LANGUAGE, CUSTOMIZED_COUNTRY);

        LocaleAdapter adapter = setUpAdapterForTest(STATE_CUSTOMIZED_LOCALE_SINGLE);
        mLocalePicker.setListAdapter(adapter);
        adapter.notifyDataSetChanged();

        final int nbrOfDefaultLanguages = getNbrOfLanguagesOnTop();
        // Assert that default locale and default sim country locale(s) should be on top
        for (int i = 0; i < nbrOfDefaultLanguages; i++) {
            Locale l = adapter.getLocale(i);
            assertTrue("Wrong default locale among the top locales",
                l.equals(mCurrentLocale) || l.equals(expected));
        }

        // Verify that the locale is not represented elsewhere.
        for (int i = nbrOfDefaultLanguages + 1; i < adapter.getCount(); i++) {
            if (adapter.getItemViewType(i) == LOCALE_ITEM) {
                assertFalse("Locale is duplicate.", adapter.getLocale(i).equals(expected));
            }
        }
    }

    /**
     * Test the customized locale on top with locales of same country sorted alphabetically after.
     * @throws Exeption
     */
    @UiThreadTest
    public void testSupportedCustomizedValueOnTop() throws Exception {
        Locale expected = new Locale(CUSTOMIZED_LANGUAGE, CUSTOMIZED_COUNTRY);
        setUpCustomizedValues(CUSTOMIZED_LANGUAGE, CUSTOMIZED_COUNTRY);

        LocaleAdapter adapter = setUpAdapterForTest(STATE_CUSTOMIZED_LOCALE_TOP);
        mLocalePicker.setListAdapter(adapter);
        adapter.notifyDataSetChanged();

        final int nbrOfDefaultLanguages = getNbrOfLanguagesOnTop();
        // Assert that default locale and default sim country locale(s) should be on top
        for (int i = 0; i < nbrOfDefaultLanguages; i++) {
            Locale l = adapter.getLocale(i);
            assertTrue("Wrong default locale among the top locales",
                l.equals(mCurrentLocale) || l.getCountry().equals(CUSTOMIZED_COUNTRY));
        }

        // Verify that the locale is not represented elsewhere.
        for (int i = nbrOfDefaultLanguages + 1; i < adapter.getCount(); i++) {
            if (adapter.getItemViewType(i) == LOCALE_ITEM) {
                assertFalse("Locale is duplicate.",
                        adapter.getLocale(i).getCountry().equals(CUSTOMIZED_COUNTRY));
            }
        }
    }

    /**
     * Test that going from state Customized to default works.
     * @throws Exception
     */
    @UiThreadTest
    public void testUnsupportedEveryThing() throws Exception {
        Locale expected = Locale.US;
        setUpCustomizedValues(SUPPORTED_SINGLE_COUNTRY, UNSUPPORTED_COUNTRY);
        setUpDefaultSimCountry(UNSUPPORTED_COUNTRY);
        setUpSwDefaultLocaleData(SUPPORTED_SINGLE_COUNTRY, UNSUPPORTED_COUNTRY);

        LocaleAdapter adapter = setUpAdapterForTest(STATE_CUSTOMIZED_LOCALE_TOP);
        mLocalePicker.setListAdapter(adapter);
        adapter.notifyDataSetChanged();

        final int nbrOfDefaultLanguages = getNbrOfLanguagesOnTop();
        // Assert that default locale and default sim country locale(s) should be on top
        for (int i = 0; i < nbrOfDefaultLanguages; i++) {
            Locale l = adapter.getLocale(i);
            assertTrue("Wrong default locale among the top locales",
                l.equals(mCurrentLocale) || l.equals(expected));
        }
        // Verify that the locale is not represented elsewhere.
        for (int i = nbrOfDefaultLanguages + 1; i < adapter.getCount(); i++) {
            if (adapter.getItemViewType(i) == LOCALE_ITEM) {
                assertFalse("Locale is duplicate.", adapter.getLocale(i).equals(expected));
            }
        }
    }

    // Reflections

    /**
     * Set a private member
     * @param instance
     * @param member
     * @param value
     */
    public static void setMember(Object instance, String member, Object value) {
        try {
            Class clazz = instance.getClass();
            Field field = clazz.getDeclaredField(member);
            field.setAccessible(true);
            field.set(instance, value);
        } catch (Exception e) {
            fail("setMember failed " + e);
        }
    }

    public static void setMember(Class clazz, Object instance, String member, Object value) {
        try {
            Field field = clazz.getDeclaredField(member);
            field.setAccessible(true);
            field.set(instance, value);
        } catch (Exception e) {
            fail("setMember failed " + e);
        }
    }

    /**
     * Get a private member
     * @param instance
     * @param member
     * @return
     */
    public static Object getMember(Object instance, String member) {
        try {
            Class clazz = instance.getClass();
            Field field = clazz.getDeclaredField(member);
            field.setAccessible(true);
            return field.get(instance);
        } catch (Exception e) {
            fail();
        }
        return null;
    }

    /**
     * Calls a private method
     */
    private static Object callMethod(Class clazz, Object instance, String methodName,
            Object... parameters) {
        try {
            Method method = null;
            method = clazz.getDeclaredMethod(methodName, String[].class);
            if (method != null) {
                method.setAccessible(true);
                String[] args = (String[])parameters;
                return method.invoke(instance, new Object[] { args });
            }
            return null;
        } catch (Exception e) {
            fail("callMethod failed "+e);
        }
        return null;
    }
}
