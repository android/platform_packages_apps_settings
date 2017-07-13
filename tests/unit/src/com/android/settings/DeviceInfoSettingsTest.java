/*
 * Copyright (C) 2012 The Android Open Source Project
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

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.android.settingslib.DeviceInfoUtils;

public class DeviceInfoSettingsTest extends AndroidTestCase {

    @SmallTest
    public void testGetFormattedKernelVersion() throws Exception {
        if ("Unavailable".equals(DeviceInfoUtils.getFormattedKernelVersion())) {
            fail("formatKernelVersion can't cope with this device's /proc/version");
        }
    }

    @SmallTest
    public void testFormatKernelVersion() throws Exception {
        assertEquals("Unavailable", DeviceInfoUtils.formatKernelVersion(""));
        assertEquals("2.6.38.8-gg784 (gcc version 4.4.3 (Ubuntu 4.4.3-4ubuntu5) )\n" +
                        "root@hpao4.eem.corp.google.com #2\n" +
                        "Fri Feb 24 03:31:23 PST 2012",
                DeviceInfoUtils.formatKernelVersion("Linux version 2.6.38.8-gg784 " +
                        "(root@hpao4.eem.corp.google.com) " +
                        "(gcc version 4.4.3 (Ubuntu 4.4.3-4ubuntu5) ) #2 SMP " +
                        "Fri Feb 24 03:31:23 PST 2012"));
        assertEquals("3.0.31-g6fb96c9 (gcc version 4.6.x-google 20120106 (prerelease) (GCC) )\n" +
                        "android-build@vpbs1.mtv.corp.google.com #1\n" +
                        "Thu Jun 28 11:02:39 PDT 2012",
                DeviceInfoUtils.formatKernelVersion("Linux version 3.0.31-g6fb96c9 " +
                        "(android-build@vpbs1.mtv.corp.google.com) " +
                        "(gcc version 4.6.x-google 20120106 (prerelease) (GCC) ) #1 " +
                        "SMP PREEMPT Thu Jun 28 11:02:39 PDT 2012"));
        assertEquals("2.6.38.8-a-b-jellybean+ (gcc version 4.4.3 (GCC) )\n" +
                        "x@y #1\n" +
                        "Tue Aug 28 22:10:46 CDT 2012",
                DeviceInfoUtils.formatKernelVersion("Linux version " +
                        "2.6.38.8-a-b-jellybean+ (x@y) " +
                        "(gcc version 4.4.3 (GCC) ) #1 PREEMPT Tue Aug 28 22:10:46 CDT 2012"));

        assertEquals("4.9.34-g6817df3 (clang version 5.0.0 )\n" +
                        "x@y #15\n" +
                        "Thu Jul 13 09:47:26 CST 2017",
                DeviceInfoUtils.formatKernelVersion("Linux version " +
                        "4.9.34-g6817df3 (x@y) " +
                        "(clang version 5.0.0 ) #15 SMP PREEMPT Thu Jul 13 09:47:26 CST 2017"));

        assertEquals("4.9.34-g6817df3 (Android clang version 5.0.300080 (based on LLVM 5.0.300080))\n" +
                        "x@y #16\n" +
                        "Thu Jul 13 11:37:03 CST 2017",
                DeviceInfoUtils.formatKernelVersion("Linux version " +
                        "4.9.34-g6817df3 (x@y) " +
                        "(Android clang version 5.0.300080 (based on LLVM 5.0.300080)) " +
                        "#16 SMP PREEMPT Thu Jul 13 11:37:03 CST 2017"));

        assertEquals("4.9.34-g6817df3 (clang version 5.0.0 " +
                        "(https://git.llvm.org/git/clang.git 48dec96ecc02497042ad46c7371383db981b1482) " +
                        "(https://git.llvm.org/git/llvm.git cc6cfc778f99aa110435666c181896e665fd7551))\n" +
                        "x@y #17\n" +
                        "Thu Jul 13 14:09:38 CST 2017",
                DeviceInfoUtils.formatKernelVersion("Linux version " +
                        "4.9.34-g6817df3 (x@y) (clang version 5.0.0 " +
                        "(https://git.llvm.org/git/clang.git 48dec96ecc02497042ad46c7371383db981b1482) " +
                        "(https://git.llvm.org/git/llvm.git cc6cfc778f99aa110435666c181896e665fd7551)) " +
                        "#17 SMP PREEMPT Thu Jul 13 14:09:38 CST 2017"));
    }
}
