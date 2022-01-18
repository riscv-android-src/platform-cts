/*
 * Copyright (C) 2020 The Android Open Source Project
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

package android.location.cts.privileged;

import android.Manifest;
import android.location.Location;
import android.location.cts.common.GnssTestCase;
import android.location.cts.common.SoftAssert;
import android.location.cts.common.TestLocationListener;
import android.location.cts.common.TestLocationManager;
import android.location.cts.common.TestMeasurementUtil;
import android.os.Build;
import android.util.Log;

import androidx.test.InstrumentationRegistry;

import org.junit.Assert;

/**
 * Test the {@link Location} values.
 *
 * Test steps:
 * 1. Register for location updates.
 * 2. Wait for {@link #LOCATION_TO_COLLECT_COUNT} locations.
 *          3.1 Confirm locations have been found.
 * 3. Get LastKnownLocation, verified all fields are in the correct range.
 */
public class GnssLocationValuesTest extends GnssTestCase {

    private static final String TAG = "GnssLocationValuesTest";
    private static final int LOCATION_TO_COLLECT_COUNT = 5;
    private TestLocationListener mLocationListener;
    // TODO(b/65458848): Re-tighten the limit to 0.001 when sufficient devices in the market comply
    private static final double MINIMUM_SPEED_FOR_BEARING = 1.000;
    private static final int MIN_ANDROID_SDK_VERSION_REQUIRED = Build.VERSION_CODES.O;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        InstrumentationRegistry.getInstrumentation().getUiAutomation()
                .adoptShellPermissionIdentity(Manifest.permission.LOCATION_HARDWARE);
        mTestLocationManager = new TestLocationManager(getContext());
        mLocationListener = new TestLocationListener(LOCATION_TO_COLLECT_COUNT);
    }

    @Override
    protected void tearDown() throws Exception {
        // Unregister listeners
        if (mLocationListener != null) {
            mTestLocationManager.removeLocationUpdates(mLocationListener);
        }

        InstrumentationRegistry.getInstrumentation().getUiAutomation()
                .dropShellPermissionIdentity();
        super.tearDown();
    }

    /**
     * 1. Get regular GNSS locations to warm up the engine.
     * 2. Get low-power GNSS locations.
     * 3. Check whether all fields' value make sense.
     */
    public void testLowPowerModeGnssLocation() throws Exception {
        // Checks if GPS hardware feature is present, skips test (pass) if not,
        if (!TestMeasurementUtil.canTestRunOnCurrentDevice(mTestLocationManager, TAG)) {
            return;
        }

        // Get regular GNSS locations to warm up the engine.
        waitForRegularGnssLocations();

        mTestLocationManager.requestLowPowerModeGnssLocationUpdates(5000, mLocationListener);

        waitAndValidateLowPowerLocations();
    }


    private void waitForRegularGnssLocations() throws InterruptedException {
        TestLocationListener locationListener = new TestLocationListener(LOCATION_TO_COLLECT_COUNT);
        mTestLocationManager.requestLocationUpdates(locationListener);
        boolean success = locationListener.await();
        mTestLocationManager.removeLocationUpdates(locationListener);

        if (success) {
            Log.i(TAG, "Successfully received " + LOCATION_TO_COLLECT_COUNT
                    + " regular GNSS locations.");
        }

        Assert.assertTrue("Time elapsed without getting enough regular GNSS locations."
                + " Possibly, the test has been run deep indoors."
                + " Consider retrying test outdoors.", success);
    }

    private void waitAndValidateLowPowerLocations() throws InterruptedException {
        boolean success = mLocationListener.await();
        SoftAssert softAssert = new SoftAssert(TAG);
        softAssert.assertTrue(
                "Time elapsed without getting the low-power GNSS locations."
                        + " Possibly, the test has been run deep indoors."
                        + " Consider retrying test outdoors.",
                success);

        // don't check speed of first GNSS location - it may not be ready in some cases
        boolean checkSpeed = false;
        for (Location location : mLocationListener.getReceivedLocationList()) {
            checkLocationRegularFields(softAssert, location, checkSpeed);
            checkSpeed = true;
        }

        softAssert.assertAll();
    }

    private static void checkLocationRegularFields(SoftAssert softAssert, Location location,
            boolean checkSpeed) {
        // For the altitude: the unit is meter
        // The lowest exposed land on Earth is at the Dead Sea shore, at -413 meters.
        // Whilst University of Tokyo Atacama Obsevatory is on 5,640m above sea level.

        softAssert.assertTrue("All GNSS locations generated by the LocationManager "
                + "must have altitudes.", location.hasAltitude());
        if (location.hasAltitude()) {
            softAssert.assertTrue("Altitude should be greater than -500 (meters).",
                    location.getAltitude() >= -500);
            softAssert.assertTrue("Altitude should be less than 6000 (meters).",
                    location.getAltitude() < 6000);
        }

        // It is guaranteed to be in the range [0.0, 360.0] if the device has a bearing.
        // The API will return 0.0 if there is no bearing
        if (location.hasSpeed() && location.getSpeed() > MINIMUM_SPEED_FOR_BEARING) {
            softAssert.assertTrue("When speed is greater than 0, all GNSS locations generated by "
                    + "the LocationManager must have bearings.", location.hasBearing());
            if (location.hasBearing()) {
                softAssert.assertTrue("Bearing should be in the range of [0.0, 360.0]",
                        location.getBearing() >= 0 && location.getBearing() <= 360);
            }
        }

        softAssert.assertTrue("ElapsedRaltimeNanos should be great than 0.",
                location.getElapsedRealtimeNanos() > 0);

        assertEquals("gps", location.getProvider());
        assertTrue(location.getTime() > 0);

        softAssert.assertTrue("Longitude should be in the range of [-180.0, 180.0] degrees",
                location.getLongitude() >= -180 && location.getLongitude() <= 180);

        softAssert.assertTrue("Latitude should be in the range of [-90.0, 90.0] degrees",
                location.getLatitude() >= -90 && location.getLatitude() <= 90);

        if (checkSpeed) {
            softAssert.assertTrue("All but the first GNSS location from LocationManager "
                    + "must have speeds.", location.hasSpeed());
        }

        // For the speed, during the cts test device shouldn't move faster than 1m/s, but
        // allowing up to 5m/s for possible early fix noise in moderate signal test environments.
        if (location.hasSpeed()) {
            softAssert.assertTrue(
                    "In the test environment, speed should be in the range of [0, 5] m/s",
                    location.getSpeed() >= 0 && location.getSpeed() <= 5);
        }
    }
}
