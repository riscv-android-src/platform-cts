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
 * limitations under the License
 */

package android.telecom.cts.screeningtestapp;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.IBinder;
import android.telecom.Call;
import android.telecom.CallScreeningService;

import android.text.TextUtils;
import android.util.Log;

import java.util.List;

/**
 * Provides a CTS-test implementation of {@link CallScreeningService}.
 * This emulates a third-party implementation of {@link CallScreeningService}, where
 * {@code MockCallScreeningService} is intended to be the {@link CallScreeningService} bundled with
 * the default dialer app.
 */
public class CtsCallScreeningService extends CallScreeningService {
    private static final String TAG = CtsCallScreeningService.class.getSimpleName();

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind: returning actual service");
        return super.onBind(intent);
    }

    @Override
    public void onScreenCall(Call.Details callDetails) {
        Log.i(TAG, "onScreenCall");
        CallScreeningServiceControl control = CallScreeningServiceControl.getInstance();
        if (control != null) {
            respondToCall(callDetails, control.getCallResponse());
        } else {
            Log.w(TAG, "No control interface.");
        }
    }
}
