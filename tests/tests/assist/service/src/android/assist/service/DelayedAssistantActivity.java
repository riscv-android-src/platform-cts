/*
 * Copyright (C) 2014 The Android Open Source Project
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

package android.assist.service;

import android.assist.common.BaseRemoteCallbackActivity;
import android.assist.common.Utils;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteCallback;
import android.util.Log;

public class DelayedAssistantActivity extends BaseRemoteCallbackActivity {
    static final String TAG = "DelayedAssistantActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RemoteCallback remoteCallback = getIntent().getParcelableExtra(Utils.EXTRA_REMOTE_CALLBACK);

        Intent intent = new Intent();
        intent.setComponent(new ComponentName(this, MainInteractionService.class));
        intent.putExtra(Utils.EXTRA_REGISTER_RECEIVER, true);
        intent.putExtra(Utils.TESTCASE_TYPE, getIntent().getStringExtra(Utils.TESTCASE_TYPE));
        intent.putExtra(Utils.DISPLAY_WIDTH_KEY,
                getIntent().getIntExtra(Utils.DISPLAY_WIDTH_KEY, 0));
        intent.putExtra(Utils.DISPLAY_HEIGHT_KEY,
                getIntent().getIntExtra(Utils.DISPLAY_HEIGHT_KEY, 0));
        intent.putExtra(Utils.EXTRA_REMOTE_CALLBACK, remoteCallback);
        finish();
        ComponentName serviceName = startService(intent);
        Log.i(TAG, "Started service: " + serviceName);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        notify(Utils.TEST_ACTIVITY_DESTROY);
    }
}
