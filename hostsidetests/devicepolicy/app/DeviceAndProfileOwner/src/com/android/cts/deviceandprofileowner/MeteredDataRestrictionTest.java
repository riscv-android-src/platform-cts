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

package com.android.cts.deviceandprofileowner;

import static com.android.compatibility.common.util.ShellIdentityUtils.invokeStaticMethodWithShellPermissions;

import static org.junit.Assert.assertNotEquals;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.NetworkInfo.State;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.SystemClock;
import android.util.Log;

import com.android.bedstead.dpmwrapper.TestAppSystemServiceFactory;
import com.android.compatibility.common.util.SystemUtil;

import org.junit.After;
import org.junit.Before;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class MeteredDataRestrictionTest extends BaseDeviceAdminTest {
    private static final String TAG = MeteredDataRestrictionTest.class.getSimpleName();

    private static final String METERED_DATA_APP_PKG =
            "com.android.cts.devicepolicy.metereddatatestapp";
    private static final String METERED_DATA_APP_MAIN_ACTIVITY = METERED_DATA_APP_PKG
            + ".MainActivity";

    private static final long WAIT_FOR_NETWORK_RECONNECTION_TIMEOUT_SEC = 10;
    private static final long WAIT_FOR_NETWORK_INFO_TIMEOUT_SEC = 8;

    private static final int NUM_TRIES_METERED_STATUS_CHECK = 20;
    private static final long INTERVAL_METERED_STATUS_CHECK_MS = 500;

    private static final String EXTRA_MESSENGER = "messenger";
    private static final int MSG_NOTIFY_NETWORK_STATE = 1;

    private final Messenger mCallbackMessenger = new Messenger(new CallbackHandler());
    private final BlockingQueue<NetworkInfo> mNetworkInfos = new LinkedBlockingQueue<>(1);

    private ConnectivityManager mCm;
    private WifiManager mWm;
    private String mMeteredWifi;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        mCm = mContext.getSystemService(ConnectivityManager.class);
        mWm = TestAppSystemServiceFactory.getWifiManager(mContext, BasicAdminReceiver.class);
        setMeteredNetwork();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        resetMeteredNetwork();
    }

    public void testSetMeteredDataDisabledPackages() {
        final List<String> restrictedPkgs = new ArrayList<>();
        restrictedPkgs.add(METERED_DATA_APP_PKG);
        final List<String> excludedPkgs = mDevicePolicyManager.setMeteredDataDisabledPackages(
                ADMIN_RECEIVER_COMPONENT, restrictedPkgs);
        assertTrue("Packages not restricted: " + excludedPkgs, excludedPkgs.isEmpty());

        List<String> actualRestrictedPkgs = mDevicePolicyManager.getMeteredDataDisabledPackages(
                ADMIN_RECEIVER_COMPONENT);
        assertEquals("Actual restricted pkgs: " + actualRestrictedPkgs,
                1, actualRestrictedPkgs.size());
        assertTrue("Actual restricted pkgs: " + actualRestrictedPkgs,
                actualRestrictedPkgs.contains(METERED_DATA_APP_PKG));
        verifyAppNetworkState(true);

        restrictedPkgs.clear();
        mDevicePolicyManager.setMeteredDataDisabledPackages(ADMIN_RECEIVER_COMPONENT,
                restrictedPkgs);
        actualRestrictedPkgs = mDevicePolicyManager.getMeteredDataDisabledPackages(
                ADMIN_RECEIVER_COMPONENT);
        assertTrue("Actual restricted pkgs: " + actualRestrictedPkgs,
                actualRestrictedPkgs.isEmpty());
        verifyAppNetworkState(false);
    }

    private void verifyAppNetworkState(boolean blocked) {
        final Bundle extras = new Bundle();
        extras.putBinder(EXTRA_MESSENGER, mCallbackMessenger.getBinder());
        mNetworkInfos.clear();
        final Intent launchIntent = new Intent()
                .setClassName(METERED_DATA_APP_PKG, METERED_DATA_APP_MAIN_ACTIVITY)
                .putExtras(extras)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(launchIntent);

        try {
            final NetworkInfo networkInfo = mNetworkInfos.poll(WAIT_FOR_NETWORK_INFO_TIMEOUT_SEC,
                    TimeUnit.SECONDS);
            if (networkInfo == null) {
                fail("Timed out waiting for the network info");
            }

            final String expectedState = (blocked ? State.DISCONNECTED : State.CONNECTED).name();
            final String expectedDetailedState = (blocked ? DetailedState.BLOCKED
                    : DetailedState.CONNECTED).name();
            assertEquals("Wrong state: " + networkInfo,
                    expectedState, networkInfo.getState().name());
            assertEquals("Wrong detailed state: " + networkInfo,
                    expectedDetailedState, networkInfo.getDetailedState().name());
        } catch (InterruptedException e) {
            fail("Waiting for networkinfo got interrupted: " + e);
        }
    }

    private class CallbackHandler extends Handler {
        public CallbackHandler() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_NOTIFY_NETWORK_STATE) {
                final NetworkInfo networkInfo = (NetworkInfo) msg.obj;
                if (!mNetworkInfos.offer(networkInfo)) {
                    Log.e(TAG, "Error while adding networkinfo");
                }
            } else {
                Log.e(TAG, "Unknown msg type: " + msg.what);
            }
        }
    }

    private void setMeteredNetwork() throws Exception {
        final int oldNetId = getActiveNetworkNetId();
        final boolean oldMeteredState = mCm.isActiveNetworkMetered();
        final NetworkInfo networkInfo = mCm.getActiveNetworkInfo();
        Log.d(TAG, "setMeteredNetwork(): oldNetId=" + oldNetId
                + ", oldMeteredState=" + oldMeteredState + ", activeNetworkInfo=" + networkInfo);
        if (networkInfo == null) {
            fail("Active network is not available");
        } else if (networkInfo.getType() != ConnectivityManager.TYPE_WIFI) {
            fail("Active network doesn't support setting metered status: " + networkInfo);
        }
        final String ssid = setWifiMeteredStatus(true);

        // Set flag so status is reverted on resetMeteredNetwork();
        mMeteredWifi = ssid;

        // When transitioning from unmetered to metered, the network stack will discconect
        // the current WiFi connection and reconnect it. In this case we need to wait for
        // the new network to come up.
        if (!oldMeteredState) {
            waitForReconnection(oldNetId);
        }
        assertWifiMeteredStatus(ssid, true);
        assertActiveNetworkMetered(true);
    }

    private void resetMeteredNetwork() throws Exception {
        if (mMeteredWifi != null) {
            Log.i(TAG, "Resetting metered status for netId=" + mMeteredWifi);
            setWifiMeteredStatus(mMeteredWifi, /* metered= */ null);
            assertWifiMeteredStatus(mMeteredWifi, /* metered= */ null);
            assertActiveNetworkMetered(false);
        }
    }

    private String setWifiMeteredStatus(Boolean metered) throws Exception {
        // Must use Shell permissions to get the connection info because on headless system user
        // mode the method would be called by the device owner on system user, which have location
        // disabled (and hence the returned connectionInfo would have the SSID redacted).
        WifiInfo connectionInfo = invokeStaticMethodWithShellPermissions(
                () -> mWm.getConnectionInfo());

        String ssid = connectionInfo.getSSID();
        assertNotNull("null SSID", ssid);
        assertNotEquals("unknown SSID", WifiManager.UNKNOWN_SSID, ssid);

        final String netId = ssid.trim().replaceAll("\"", ""); // remove quotes, if any.
        assertFalse("empty SSID", ssid.isEmpty());

        Log.d(TAG, "setWifiMeteredStatus(" + metered + "): setting " + connectionInfo);
        setWifiMeteredStatus(netId, metered);
        return netId;
    }

    private void setWifiMeteredStatus(String ssid, Boolean metered) throws Exception {
        Log.i(TAG, "Setting wi-fi network " + ssid + " metered status to " + metered);
        executeCmd("cmd netpolicy set metered-network " + ssid + " " +
                (metered != null ? metered.toString() : "undefined"));
    }

    private void assertWifiMeteredStatus(String ssid, Boolean metered) throws Exception {
        final String cmd = "cmd netpolicy list wifi-networks";
        final String expectedResult = ssid + ";" + (metered != null ? metered.toString() : "none");
        String cmdResult = null;
        for (int i = 0; i < NUM_TRIES_METERED_STATUS_CHECK; ++i) {
            cmdResult = executeCmd(cmd);
            if (cmdResult.contains(expectedResult)) {
                return;
            }
            SystemClock.sleep(INTERVAL_METERED_STATUS_CHECK_MS);
        }
        fail("Timed out waiting for wifi metered status to change. expected=" + expectedResult
                + ", actual status=" + cmdResult);
    }

    private void assertActiveNetworkMetered(boolean metered) {
        boolean actualMeteredStatus = !metered;
        for (int i = 0; i < NUM_TRIES_METERED_STATUS_CHECK; ++i) {
            actualMeteredStatus = mCm.isActiveNetworkMetered();
            if (actualMeteredStatus == metered) {
                return;
            }
            SystemClock.sleep(INTERVAL_METERED_STATUS_CHECK_MS);
        }
        fail("Timed out waiting for active network metered status to change. expected="
                + metered + "; actual=" + actualMeteredStatus
                + "; networkInfo=" + mCm.getActiveNetwork());
    }

    private String executeCmd(String cmd) throws Exception {
        final String result = SystemUtil.runShellCommand(getInstrumentation(), cmd);
        Log.i(TAG, "Cmd '" + cmd + "' result: " + result);
        return result;
    }

    private int getActiveNetworkNetId() {
        Network network = mCm.getActiveNetwork();
        if (network == null) {
            return 0;
        }
        return network.getNetId();
    }

    private void waitForReconnection(int oldNetId) throws InterruptedException {
        long pollingDeadline = System.currentTimeMillis()
                + WAIT_FOR_NETWORK_RECONNECTION_TIMEOUT_SEC * 1000;
        int latestNetId;
        do {
            Thread.sleep(1000);
            if (System.currentTimeMillis() >= pollingDeadline) {
                fail("Timeout waiting for network reconnection");
            }
            latestNetId = getActiveNetworkNetId();
            // NetId will be 0 while old network is disconnected but new network
            // has not come up yet.
        } while (latestNetId == 0 || latestNetId == oldNetId);
    }
}
