/*
 * Copyright (C) 2019 The Android Open Source Project
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

package android.telephony.ims;

import android.app.IntentService;
import android.content.Intent;

/**
 * This service is used to provide the interface required for a default SMS application. It
 * intentionally has no custom behavior.
 */
public class SmsApplicationService extends IntentService {
    private static final String TAG = "SmsApplicationService";

    public SmsApplicationService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // Do nothing
    }
}
