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
package com.android.settings.development.tests;

public final class Constants {
    // Turn on and off the flags below to switch between using the simulation code located in this
    // package, or using the real AdbManager.
    public static final boolean USE_SIMULATION = false;
    public static final boolean USE_QRCODE_SIMULATOR = USE_SIMULATION && false;
}
