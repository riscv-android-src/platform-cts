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

package android.preference.cts;

import android.test.ActivityInstrumentationTestCase2;

public class CustomDialogPreferenceTest
        extends ActivityInstrumentationTestCase2<PreferencesFromXml> {

    private PreferencesFromXml mActivity;
    private CustomDialogPreference mDialogPref;

    public CustomDialogPreferenceTest() {
        super(PreferencesFromXml.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
        mDialogPref = (CustomDialogPreference) mActivity.findPreference(
                "custom_dialog_pref_1");
    }

    public void testNotNull() {
        assertNotNull(mDialogPref);
    }
}
