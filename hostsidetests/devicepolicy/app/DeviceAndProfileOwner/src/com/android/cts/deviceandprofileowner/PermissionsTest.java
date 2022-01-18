/*
 * Copyright (C) 2015 The Android Open Source Project
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

import static android.Manifest.permission.READ_CONTACTS;
import static android.Manifest.permission.WRITE_CONTACTS;
import static android.app.admin.DevicePolicyManager.PERMISSION_GRANT_STATE_DEFAULT;
import static android.app.admin.DevicePolicyManager.PERMISSION_GRANT_STATE_DENIED;
import static android.app.admin.DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED;
import static android.app.admin.DevicePolicyManager.PERMISSION_POLICY_AUTO_DENY;
import static android.app.admin.DevicePolicyManager.PERMISSION_POLICY_AUTO_GRANT;
import static android.app.admin.DevicePolicyManager.PERMISSION_POLICY_PROMPT;
import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.Manifest.permission;
import android.app.UiAutomation;
import android.app.admin.DevicePolicyManager;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject2;
import android.util.Log;

import com.android.cts.devicepolicy.PermissionBroadcastReceiver;
import com.android.cts.devicepolicy.PermissionUtils;

import com.google.android.collect.Sets;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Test Runtime Permissions APIs in DevicePolicyManager.
 */
public class PermissionsTest extends BaseDeviceAdminTest {

    private static final String TAG = "PermissionsTest";

    private static final String PERMISSION_APP_PACKAGE_NAME = "com.android.cts.permissionapp";
    private static final String PRE_M_APP_PACKAGE_NAME
            = "com.android.cts.launcherapps.simplepremapp";
    private static final String PERMISSIONS_ACTIVITY_NAME
            = PERMISSION_APP_PACKAGE_NAME + ".PermissionActivity";
    private static final String CUSTOM_PERM_A_NAME = "com.android.cts.permissionapp.permA";
    private static final String CUSTOM_PERM_B_NAME = "com.android.cts.permissionapp.permB";
    private static final String DEVELOPMENT_PERMISSION = "android.permission.INTERACT_ACROSS_USERS";

    private static final String ACTION_PERMISSION_RESULT
            = "com.android.cts.permission.action.PERMISSION_RESULT";

    private static final BySelector CRASH_POPUP_BUTTON_SELECTOR = By
            .clazz(android.widget.Button.class.getName())
            .text("OK")
            .pkg("android");
    private static final BySelector CRASH_POPUP_TEXT_SELECTOR = By
            .clazz(android.widget.TextView.class.getName())
            .pkg("android");
    private static final String CRASH_WATCHER_ID = "CRASH";
    private static final String AUTO_GRANTED_PERMISSIONS_CHANNEL_ID =
            "alerting auto granted permissions";

    private static final Set<String> LOCATION_PERMISSIONS = Sets.newHashSet(
            permission.ACCESS_FINE_LOCATION,
            permission.ACCESS_BACKGROUND_LOCATION,
            permission.ACCESS_COARSE_LOCATION);

    private static final Set<String> SENSORS_PERMISSIONS = Sets.newHashSet(
            permission.ACCESS_FINE_LOCATION,
            permission.ACCESS_COARSE_LOCATION,
            permission.CAMERA,
            permission.ACTIVITY_RECOGNITION,
            permission.BODY_SENSORS);


    private PermissionBroadcastReceiver mReceiver;
    private UiDevice mDevice;
    private UiAutomation mUiAutomation;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mReceiver = new PermissionBroadcastReceiver();
        mContext.registerReceiver(mReceiver, new IntentFilter(ACTION_PERMISSION_RESULT));
        mDevice = UiDevice.getInstance(getInstrumentation());
        mUiAutomation = getInstrumentation().getUiAutomation();
    }

    @Override
    protected void tearDown() throws Exception {
        mContext.unregisterReceiver(mReceiver);
        mDevice.removeWatcher(CRASH_WATCHER_ID);
        super.tearDown();
    }

    public void testPermissionGrantStateDenied() throws Exception {
        setPermissionGrantState(READ_CONTACTS, PERMISSION_GRANT_STATE_DENIED);

        assertPermissionGrantState(READ_CONTACTS, PERMISSION_GRANT_STATE_DENIED);
        assertCannotRequestPermissionFromActivity(READ_CONTACTS);
    }

    public void testPermissionGrantStateDenied_permissionRemainsDenied() throws Exception {
        int grantState = mDevicePolicyManager.getPermissionGrantState(ADMIN_RECEIVER_COMPONENT,
                PERMISSION_APP_PACKAGE_NAME, READ_CONTACTS);
        try {
            setPermissionGrantState(READ_CONTACTS, PERMISSION_GRANT_STATE_DENIED);

            assertNoPermissionFromActivity(READ_CONTACTS);

            // Should stay denied
            setPermissionGrantState(READ_CONTACTS, PERMISSION_GRANT_STATE_DEFAULT);

            assertNoPermissionFromActivity(READ_CONTACTS);
        } finally {
            // Restore original state
            setPermissionGrantState(READ_CONTACTS, grantState);
        }
    }

    public void testPermissionGrantStateDenied_mixedPolicies() throws Exception {
        int grantState = mDevicePolicyManager.getPermissionGrantState(ADMIN_RECEIVER_COMPONENT,
                PERMISSION_APP_PACKAGE_NAME, READ_CONTACTS);
        int permissionPolicy = mDevicePolicyManager.getPermissionPolicy(ADMIN_RECEIVER_COMPONENT);
        try {
            setPermissionGrantState(READ_CONTACTS, PERMISSION_GRANT_STATE_DENIED);

            // Check no permission by launching an activity and requesting the permission
            // Should stay denied if grant state is denied
            setPermissionPolicy(PERMISSION_POLICY_AUTO_GRANT);

            assertPermissionPolicy(PERMISSION_POLICY_AUTO_GRANT);
            assertCannotRequestPermissionFromActivity(READ_CONTACTS);

            setPermissionPolicy(PERMISSION_POLICY_AUTO_DENY);

            assertPermissionPolicy(PERMISSION_POLICY_AUTO_DENY);
            assertCannotRequestPermissionFromActivity(READ_CONTACTS);

            setPermissionPolicy(PERMISSION_POLICY_PROMPT);

            assertPermissionPolicy(PERMISSION_POLICY_PROMPT);
            assertCannotRequestPermissionFromActivity(READ_CONTACTS);
        } finally {
            // Restore original state
            setPermissionGrantState(READ_CONTACTS, grantState);
            setPermissionPolicy(permissionPolicy);
        }
    }

    public void testPermissionGrantStateDenied_otherPermissionIsGranted() throws Exception {
        int grantStateA = mDevicePolicyManager.getPermissionGrantState(ADMIN_RECEIVER_COMPONENT,
                PERMISSION_APP_PACKAGE_NAME, CUSTOM_PERM_A_NAME);
        int grantStateB = mDevicePolicyManager.getPermissionGrantState(ADMIN_RECEIVER_COMPONENT,
                PERMISSION_APP_PACKAGE_NAME, CUSTOM_PERM_B_NAME);
        try {
            setPermissionGrantState(CUSTOM_PERM_A_NAME, PERMISSION_GRANT_STATE_GRANTED);
            setPermissionGrantState(CUSTOM_PERM_B_NAME, PERMISSION_GRANT_STATE_DENIED);

            assertPermissionGrantState(CUSTOM_PERM_A_NAME, PERMISSION_GRANT_STATE_GRANTED);
            assertPermissionGrantState(CUSTOM_PERM_B_NAME, PERMISSION_GRANT_STATE_DENIED);

            /*
             * CUSTOM_PERM_A_NAME and CUSTOM_PERM_B_NAME are in the same permission group and one is
             * granted the other one is not.
             *
             * It should not be possible to get the permission that was denied via policy granted by
             * requesting it.
             */
            assertCannotRequestPermissionFromActivity(CUSTOM_PERM_B_NAME);
        } finally {
            // Restore original state
            setPermissionGrantState(CUSTOM_PERM_A_NAME, grantStateA);
            setPermissionGrantState(CUSTOM_PERM_B_NAME, grantStateB);
        }
    }

    public void testPermissionGrantStateGranted() throws Exception {
        setPermissionGrantState(READ_CONTACTS, PERMISSION_GRANT_STATE_GRANTED);

        assertPermissionGrantState(READ_CONTACTS, PERMISSION_GRANT_STATE_GRANTED);
        assertCanRequestPermissionFromActivity(READ_CONTACTS);
    }

    public void testPermissionGrantStateGranted_permissionRemainsGranted() throws Exception {
        int grantState = mDevicePolicyManager.getPermissionGrantState(ADMIN_RECEIVER_COMPONENT,
                PERMISSION_APP_PACKAGE_NAME, READ_CONTACTS);
        try {
            setPermissionGrantState(READ_CONTACTS, PERMISSION_GRANT_STATE_GRANTED);

            assertHasPermissionFromActivity(READ_CONTACTS);

            // Should stay granted
            setPermissionGrantState(READ_CONTACTS, PERMISSION_GRANT_STATE_DEFAULT);

            assertHasPermissionFromActivity(READ_CONTACTS);
        } finally {
            // Restore original state
            setPermissionGrantState(READ_CONTACTS, grantState);
        }
    }

    public void testPermissionGrantStateGranted_mixedPolicies() throws Exception {
        int grantState = mDevicePolicyManager.getPermissionGrantState(ADMIN_RECEIVER_COMPONENT,
                PERMISSION_APP_PACKAGE_NAME, READ_CONTACTS);
        int permissionPolicy = mDevicePolicyManager.getPermissionPolicy(ADMIN_RECEIVER_COMPONENT);
        try {
            setPermissionGrantState(READ_CONTACTS, PERMISSION_GRANT_STATE_GRANTED);

            // Check permission by launching an activity and requesting the permission
            setPermissionPolicy(PERMISSION_POLICY_AUTO_GRANT);

            assertPermissionPolicy(PERMISSION_POLICY_AUTO_GRANT);
            assertCanRequestPermissionFromActivity(READ_CONTACTS);

            setPermissionPolicy(PERMISSION_POLICY_AUTO_DENY);

            assertPermissionPolicy(PERMISSION_POLICY_AUTO_DENY);
            assertCanRequestPermissionFromActivity(READ_CONTACTS);

            setPermissionPolicy(PERMISSION_POLICY_PROMPT);

            assertPermissionPolicy(PERMISSION_POLICY_PROMPT);
            assertCanRequestPermissionFromActivity(READ_CONTACTS);
        } finally {
            // Restore original state
            setPermissionGrantState(READ_CONTACTS, grantState);
            setPermissionPolicy(permissionPolicy);
        }
    }

    public void testPermissionGrantStateGranted_userNotifiedOfLocationPermission()
            throws Exception {
        for (String locationPermission : LOCATION_PERMISSIONS) {
            // TODO(b/161359841): move NotificationListener to app/common
            CountDownLatch notificationLatch = initPermissionNotificationLatch();

            setPermissionGrantState(locationPermission, PERMISSION_GRANT_STATE_GRANTED);

            assertPermissionGrantState(locationPermission, PERMISSION_GRANT_STATE_GRANTED);
            assertTrue(String.format("Did not receive notification for permission %s",
                    locationPermission), notificationLatch.await(60, TimeUnit.SECONDS));
            NotificationListener.getInstance().clearListeners();
        }
    }

    public void testPermissionGrantState_developmentPermission() {
        assertCannotSetPermissionGrantStateDevelopmentPermission(PERMISSION_GRANT_STATE_DENIED);
        assertCannotSetPermissionGrantStateDevelopmentPermission(PERMISSION_GRANT_STATE_DEFAULT);
        assertCannotSetPermissionGrantStateDevelopmentPermission(PERMISSION_GRANT_STATE_GRANTED);
    }

    private void assertCannotSetPermissionGrantStateDevelopmentPermission(int value) {
        unableToSetPermissionGrantState(DEVELOPMENT_PERMISSION, value);

        assertPermissionGrantState(DEVELOPMENT_PERMISSION, PERMISSION_GRANT_STATE_DEFAULT);
        PermissionUtils.checkPermission(DEVELOPMENT_PERMISSION, PERMISSION_DENIED,
                PERMISSION_APP_PACKAGE_NAME);
    }

    public void testPermissionGrantState_preMApp_preQDeviceAdmin() throws Exception {
        // These tests are to make sure that pre-M apps are not granted/denied runtime permissions
        // by a profile owner that targets pre-Q
        assertCannotSetPermissionGrantStatePreMApp(READ_CONTACTS, PERMISSION_GRANT_STATE_DENIED);
        assertCannotSetPermissionGrantStatePreMApp(READ_CONTACTS, PERMISSION_GRANT_STATE_GRANTED);
    }

    private void assertCannotSetPermissionGrantStatePreMApp(String permission, int value)
            throws Exception {
        assertFalse(mDevicePolicyManager.setPermissionGrantState(ADMIN_RECEIVER_COMPONENT,
                PRE_M_APP_PACKAGE_NAME, permission, value));
        assertEquals(mDevicePolicyManager.getPermissionGrantState(ADMIN_RECEIVER_COMPONENT,
                PRE_M_APP_PACKAGE_NAME, permission), PERMISSION_GRANT_STATE_DEFAULT);

        // Install runtime permissions should always be granted
        PermissionUtils.checkPermission(permission, PERMISSION_GRANTED, PRE_M_APP_PACKAGE_NAME);
        PermissionUtils.checkPermissionAndAppOps(permission, PERMISSION_GRANTED,
                PRE_M_APP_PACKAGE_NAME);
    }

    public void testPermissionGrantState_preMApp() throws Exception {
        // These tests are to make sure that pre-M apps can be granted/denied runtime permissions
        // by a profile owner targets Q or later
        assertCanSetPermissionGrantStatePreMApp(READ_CONTACTS, PERMISSION_GRANT_STATE_DENIED);
        assertCanSetPermissionGrantStatePreMApp(READ_CONTACTS, PERMISSION_GRANT_STATE_GRANTED);
    }

    private void assertCanSetPermissionGrantStatePreMApp(String permission, int value)
            throws Exception {
        assertTrue(mDevicePolicyManager.setPermissionGrantState(ADMIN_RECEIVER_COMPONENT,
                PRE_M_APP_PACKAGE_NAME, permission, value));
        assertEquals(mDevicePolicyManager.getPermissionGrantState(ADMIN_RECEIVER_COMPONENT,
                PRE_M_APP_PACKAGE_NAME, permission), value);

        // Install time permissions should always be granted
        PermissionUtils.checkPermission(permission, PERMISSION_GRANTED, PRE_M_APP_PACKAGE_NAME);

        // For pre-M apps the access to the data might be prevented via app-ops. Hence check that
        // they are correctly set
        switch (value) {
            case PERMISSION_GRANT_STATE_GRANTED:
                PermissionUtils.checkPermissionAndAppOps(permission, PERMISSION_GRANTED,
                        PRE_M_APP_PACKAGE_NAME);
                break;
            case PERMISSION_GRANT_STATE_DENIED:
                PermissionUtils.checkPermissionAndAppOps(permission, PERMISSION_DENIED,
                        PRE_M_APP_PACKAGE_NAME);
                break;
            default:
                fail("unsupported policy value");
        }
    }

    public void testPermissionPolicyAutoDeny() throws Exception {
        setPermissionPolicy(PERMISSION_POLICY_AUTO_DENY);

        assertPermissionPolicy(PERMISSION_POLICY_AUTO_DENY);
        assertCannotRequestPermissionFromActivity(READ_CONTACTS);
    }

    public void testPermissionPolicyAutoDeny_permissionLocked() throws Exception {
        int grantState = mDevicePolicyManager.getPermissionGrantState(ADMIN_RECEIVER_COMPONENT,
                PERMISSION_APP_PACKAGE_NAME, READ_CONTACTS);
        int permissionPolicy = mDevicePolicyManager.getPermissionPolicy(ADMIN_RECEIVER_COMPONENT);
        try {
            setPermissionGrantState(READ_CONTACTS, PERMISSION_GRANT_STATE_DENIED);
            setPermissionGrantState(READ_CONTACTS, PERMISSION_GRANT_STATE_DEFAULT);
            testPermissionPolicyAutoDeny();

            // Permission should be locked, so changing the policy should not change the grant state
            setPermissionPolicy(PERMISSION_POLICY_PROMPT);

            assertPermissionPolicy(PERMISSION_POLICY_PROMPT);
            assertCannotRequestPermissionFromActivity(READ_CONTACTS);

            setPermissionPolicy(PERMISSION_POLICY_AUTO_GRANT);

            assertPermissionPolicy(PERMISSION_POLICY_AUTO_GRANT);
            assertCannotRequestPermissionFromActivity(READ_CONTACTS);
        } finally {
            // Restore original state
            setPermissionGrantState(READ_CONTACTS, grantState);
            setPermissionPolicy(permissionPolicy);
        }
    }

    public void testPermissionPolicyAutoGrant() throws Exception {
        setPermissionPolicy(PERMISSION_POLICY_AUTO_GRANT);

        assertPermissionPolicy(PERMISSION_POLICY_AUTO_GRANT);
        assertCanRequestPermissionFromActivity(READ_CONTACTS);
    }

    public void testPermissionPolicyAutoGrant_permissionLocked() throws Exception {
        int grantState = mDevicePolicyManager.getPermissionGrantState(ADMIN_RECEIVER_COMPONENT,
                PERMISSION_APP_PACKAGE_NAME, READ_CONTACTS);
        int permissionPolicy = mDevicePolicyManager.getPermissionPolicy(ADMIN_RECEIVER_COMPONENT);
        try {
            setPermissionGrantState(READ_CONTACTS, PERMISSION_GRANT_STATE_DEFAULT);
            setPermissionPolicy(PERMISSION_POLICY_AUTO_GRANT);

            assertPermissionPolicy(PERMISSION_POLICY_AUTO_GRANT);
            assertCanRequestPermissionFromActivity(READ_CONTACTS);

            // permission should be locked, so changing the policy should not change the grant state
            setPermissionPolicy(PERMISSION_POLICY_PROMPT);

            assertPermissionPolicy(PERMISSION_POLICY_PROMPT);
            assertCanRequestPermissionFromActivity(READ_CONTACTS);

            setPermissionPolicy(PERMISSION_POLICY_AUTO_DENY);

            assertPermissionPolicy(PERMISSION_POLICY_AUTO_DENY);
            assertCanRequestPermissionFromActivity(READ_CONTACTS);
        } finally {
            // Restore original state
            setPermissionGrantState(READ_CONTACTS, grantState);
            setPermissionPolicy(permissionPolicy);
        }
    }

    public void testPermissionPolicyAutoGrant_multiplePermissionsInGroup() throws Exception {
        setPermissionPolicy(PERMISSION_POLICY_AUTO_GRANT);

        // Both permissions should be granted
        assertPermissionPolicy(PERMISSION_POLICY_AUTO_GRANT);
        assertCanRequestPermissionFromActivity(READ_CONTACTS);
        assertCanRequestPermissionFromActivity(WRITE_CONTACTS);
    }

    public void testCannotRequestPermission() throws Exception {
        assertCannotRequestPermissionFromActivity(READ_CONTACTS);
    }

    public void testCanRequestPermission() throws Exception {
        assertCanRequestPermissionFromActivity(READ_CONTACTS);
    }

    public void testPermissionPrompts() throws Exception {
        // register a crash watcher
        mDevice.registerWatcher(CRASH_WATCHER_ID, () -> {
            UiObject2 button = mDevice.findObject(CRASH_POPUP_BUTTON_SELECTOR);
            if (button != null) {
                UiObject2 text = mDevice.findObject(CRASH_POPUP_TEXT_SELECTOR);
                Log.d(TAG, "Removing an error dialog: " + text != null ? text.getText() : null);
                button.click();
                return true;
            }
            return false;
        });
        mDevice.runWatchers();
        setPermissionPolicy(PERMISSION_POLICY_PROMPT);

        assertPermissionPolicy(PERMISSION_POLICY_PROMPT);
        PermissionUtils.launchActivityAndRequestPermission(mReceiver, mDevice, READ_CONTACTS,
                PERMISSION_DENIED, PERMISSION_APP_PACKAGE_NAME, PERMISSIONS_ACTIVITY_NAME);
        PermissionUtils.checkPermission(READ_CONTACTS, PERMISSION_DENIED,
                PERMISSION_APP_PACKAGE_NAME);
        PermissionUtils.launchActivityAndRequestPermission(mReceiver, mDevice, READ_CONTACTS,
                PERMISSION_GRANTED, PERMISSION_APP_PACKAGE_NAME, PERMISSIONS_ACTIVITY_NAME);
        PermissionUtils.checkPermission(READ_CONTACTS, PERMISSION_GRANTED,
                PERMISSION_APP_PACKAGE_NAME);
    }

    public void testSensorsRelatedPermissionsCannotBeGranted() throws Exception {
        for (String sensorPermission : SENSORS_PERMISSIONS) {
            try {
                // The permission cannot be granted.
                assertFailedToSetPermissionGrantState(
                        sensorPermission, DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);

                // But the user can grant it.
                PermissionUtils.launchActivityAndRequestPermission(mReceiver, mDevice,
                        sensorPermission, PERMISSION_GRANTED, PERMISSION_APP_PACKAGE_NAME,
                        PERMISSIONS_ACTIVITY_NAME);

                // And the package manager should show it as granted.
                PermissionUtils.checkPermission(sensorPermission, PERMISSION_GRANTED,
                        PERMISSION_APP_PACKAGE_NAME);
            } finally {
                revokePermission(sensorPermission);
            }
        }
    }

    public void testSensorsRelatedPermissionsCanBeDenied() throws Exception {
        for (String sensorPermission : SENSORS_PERMISSIONS) {
            // The permission can be denied
            setPermissionGrantState(sensorPermission, PERMISSION_GRANT_STATE_DENIED);

            assertPermissionGrantState(sensorPermission, PERMISSION_GRANT_STATE_DENIED);
            assertCannotRequestPermissionFromActivity(sensorPermission);
        }
    }

    public void testSensorsRelatedPermissionsNotGrantedViaPolicy() throws Exception {
        setPermissionPolicy(PERMISSION_POLICY_AUTO_GRANT);
        for (String sensorPermission : SENSORS_PERMISSIONS) {
            try {
                // The permission is not granted by default.
                PermissionUtils.checkPermission(sensorPermission, PERMISSION_DENIED,
                        PERMISSION_APP_PACKAGE_NAME);
                // But the user can grant it.
                PermissionUtils.launchActivityAndRequestPermission(mReceiver, mDevice,
                        sensorPermission,
                        PERMISSION_GRANTED, PERMISSION_APP_PACKAGE_NAME, PERMISSIONS_ACTIVITY_NAME);

                // And the package manager should show it as granted.
                PermissionUtils.checkPermission(sensorPermission, PERMISSION_GRANTED,
                        PERMISSION_APP_PACKAGE_NAME);
            } finally {
                revokePermission(sensorPermission);
            }
        }
    }

    public void testStateOfSensorsRelatedPermissionsCannotBeRead() throws Exception {
        for (String sensorPermission : SENSORS_PERMISSIONS) {
            try {
                // The admin tries to grant the permission.
                setPermissionGrantState(sensorPermission, PERMISSION_GRANT_STATE_GRANTED);

                // But the user denies it.
                PermissionUtils.launchActivityAndRequestPermission(mReceiver, mDevice,
                        sensorPermission, PERMISSION_DENIED, PERMISSION_APP_PACKAGE_NAME,
                        PERMISSIONS_ACTIVITY_NAME);

                // And the admin cannot learn of it.
                assertPermissionGrantState(sensorPermission, PERMISSION_GRANT_STATE_DEFAULT);
            } finally {
                revokePermission(sensorPermission);
            }
        }
    }

    private void revokePermission(String sensorPermission) {
        if (LOCATION_PERMISSIONS.contains(sensorPermission)) {
            mUiAutomation.revokeRuntimePermission(PERMISSION_APP_PACKAGE_NAME,
                    permission.ACCESS_FINE_LOCATION);
            mUiAutomation.revokeRuntimePermission(PERMISSION_APP_PACKAGE_NAME,
                    permission.ACCESS_COARSE_LOCATION);
        } else {
            mUiAutomation.revokeRuntimePermission(PERMISSION_APP_PACKAGE_NAME, sensorPermission);
        }
    }

    private void assertFailedToSetPermissionGrantState(String permission, int value) {
        assertTrue(mDevicePolicyManager.setPermissionGrantState(ADMIN_RECEIVER_COMPONENT,
                PERMISSION_APP_PACKAGE_NAME, permission, value));
        assertEquals(mDevicePolicyManager.getPermissionGrantState(ADMIN_RECEIVER_COMPONENT,
                PERMISSION_APP_PACKAGE_NAME, permission),
                DevicePolicyManager.PERMISSION_GRANT_STATE_DEFAULT);
        assertEquals(mContext.getPackageManager().checkPermission(permission,
                PERMISSION_APP_PACKAGE_NAME),
                PackageManager.PERMISSION_DENIED);
    }

    private CountDownLatch initPermissionNotificationLatch() {
        CountDownLatch notificationCounterLatch = new CountDownLatch(1);
        NotificationListener.getInstance().addListener((notification) -> {
            if (notification.getPackageName().equals(
                    mContext.getPackageManager().getPermissionControllerPackageName()) &&
                    notification.getNotification().getChannelId().equals(
                            AUTO_GRANTED_PERMISSIONS_CHANNEL_ID)) {
                notificationCounterLatch.countDown();
            }
        });
        return notificationCounterLatch;
    }

    private void setPermissionPolicy(int permissionPolicy) {
        mDevicePolicyManager.setPermissionPolicy(ADMIN_RECEIVER_COMPONENT, permissionPolicy);
    }

    private boolean setPermissionGrantState(String permission, int grantState) {
        return mDevicePolicyManager.setPermissionGrantState(ADMIN_RECEIVER_COMPONENT,
                PERMISSION_APP_PACKAGE_NAME, permission, grantState);
    }

    private void unableToSetPermissionGrantState(String permission, int grantState) {
        assertFalse(setPermissionGrantState(permission, grantState));
    }

    private void assertPermissionGrantState(String permission, int grantState) {
        assertEquals(mDevicePolicyManager.getPermissionGrantState(ADMIN_RECEIVER_COMPONENT,
                PERMISSION_APP_PACKAGE_NAME, permission), grantState);
    }

    private void assertPermissionPolicy(int permissionPolicy) {
        assertEquals(mDevicePolicyManager.getPermissionPolicy(ADMIN_RECEIVER_COMPONENT),
                permissionPolicy);
    }

    private void assertCanRequestPermissionFromActivity(String permission) throws Exception {
        PermissionUtils.launchActivityAndRequestPermission(
                mReceiver, permission, PERMISSION_GRANTED,
                PERMISSION_APP_PACKAGE_NAME, PERMISSIONS_ACTIVITY_NAME);
    }

    private void assertCannotRequestPermissionFromActivity(String permission) throws Exception {
        PermissionUtils.launchActivityAndRequestPermission(
                mReceiver, permission, PERMISSION_DENIED,
                PERMISSION_APP_PACKAGE_NAME, PERMISSIONS_ACTIVITY_NAME);
    }

    private void assertHasPermissionFromActivity(String permission) throws Exception {
        PermissionUtils.launchActivityAndCheckPermission(
                mReceiver, permission, PERMISSION_GRANTED,
                PERMISSION_APP_PACKAGE_NAME, PERMISSIONS_ACTIVITY_NAME);
    }

    private void assertNoPermissionFromActivity(String permission) throws Exception {
        PermissionUtils.launchActivityAndCheckPermission(
                mReceiver, permission, PERMISSION_DENIED,
                PERMISSION_APP_PACKAGE_NAME, PERMISSIONS_ACTIVITY_NAME);
    }
}
