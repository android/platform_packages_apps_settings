/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.content.ComponentName;
import android.test.ActivityUnitTestCase;
import android.view.inputmethod.InputMethodInfo;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class ULanguageSettingsTests extends ActivityUnitTestCase<LanguageSettings> {

    public ULanguageSettingsTests() {
        super(LanguageSettings.class);
    }

    private static String[] inputMethods = {
            "com.example1/.Example1",
            "com.example2/.Example1",
            "com.example2/.Example2",
            "com.example2/.Example3",
            "com.example3/.Example4"
    };

    private static String[] keyboardOrderBy = {
            "com.example3/.Example4",
            "com.example1/.Example1",
            "com.example2/.Example3"
    };

    private static String[] expectedSortedMethods = {
            "com.example3/.Example4",
            "com.example1/.Example1",
            "com.example2/.Example3",
            "com.example2/.Example1",
            "com.example2/.Example2"
    };

    private List<InputMethodInfo> createList(String[] strings) {
        List<InputMethodInfo> list = new ArrayList<InputMethodInfo>();
        for (String componentString : strings) {
            ComponentName keyboardComponent = ComponentName.unflattenFromString(componentString);
            InputMethodInfo keyboardInfo = new InputMethodInfo(keyboardComponent.getPackageName(),
                    keyboardComponent.getClassName(), null, null);
            list.add(keyboardInfo);
        }
        return list;
    }

    public void testSortInputMethods() {
        List<InputMethodInfo> inputMethodsList = createList(inputMethods);
        Method sortInputMethods = getMethod(LanguageSettings.class, "prioritize");
        List<InputMethodInfo> sortedList = null;
        try {
            sortedList = (List<InputMethodInfo>)sortInputMethods.invoke(null, inputMethodsList,
                    keyboardOrderBy);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Invoking sortInputMethods failed");
        }

        List<InputMethodInfo> expectedList = createList(expectedSortedMethods);
        assertEquals("sortInputMethods does not sort correct", expectedList, sortedList);
    }

    /**
     * Gets the first private method of a name.
     *
     * @param clazz Class from which to get the private method
     * @param member The name of the private method
     * @return the method object
     */
    private static Method getMethod(Class clazz, String name) {
        try {
            Method[] allMethods = clazz.getDeclaredMethods();
            for (Method m : allMethods) {
                if (m.getName().equals(name)) {
                    m.setAccessible(true);
                    return m;
                }
            }
            return null;
        } catch (Exception e) {
            fail("getMethod failed " + e);
        }
        return null;
    }
}
