/**
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.accessibilityservice.cts;

import static android.accessibilityservice.cts.utils.ActivityLaunchUtils.launchActivityAndWaitForItToBeOnscreen;
import static android.accessibilityservice.cts.utils.AsyncUtils.DEFAULT_TIMEOUT_MS;
import static android.view.accessibility.AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_LENGTH;
import static android.view.accessibility.AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_START_INDEX;
import static android.view.accessibility.AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.accessibilityservice.cts.R;
import android.accessibilityservice.cts.activities.AccessibilityTextTraversalActivity;
import android.app.Instrumentation;
import android.app.UiAutomation;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Message;
import android.os.Parcelable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;
import android.view.accessibility.AccessibilityRequestPreparer;
import android.widget.EditText;
import android.widget.TextView;

import androidx.test.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Test cases for actions taken on text views.
 */
@RunWith(AndroidJUnit4.class)
public class AccessibilityTextActionTest {
    private static Instrumentation sInstrumentation;
    private static UiAutomation sUiAutomation;
    final Object mClickableSpanCallbackLock = new Object();
    final AtomicBoolean mClickableSpanCalled = new AtomicBoolean(false);
    

    private AccessibilityTextTraversalActivity mActivity;

    @Rule
    public ActivityTestRule<AccessibilityTextTraversalActivity> mActivityRule =
            new ActivityTestRule<>(AccessibilityTextTraversalActivity.class, false, false);

    @BeforeClass
    public static void oneTimeSetup() throws Exception {
        sInstrumentation = InstrumentationRegistry.getInstrumentation();
        sUiAutomation = sInstrumentation.getUiAutomation();
    }

    @Before
    public void setUp() throws Exception {
        mActivity = launchActivityAndWaitForItToBeOnscreen(
                sInstrumentation, sUiAutomation, mActivityRule);
        mClickableSpanCalled.set(false);
    }

    @AfterClass
    public static void postTestTearDown() {
        sUiAutomation.destroy();
    }

    @Test
    public void testNotEditableTextView_shouldNotExposeOrRespondToSetTextAction() {
        final TextView textView = (TextView) mActivity.findViewById(R.id.text);
        makeTextViewVisibleAndSetText(textView, mActivity.getString(R.string.a_b));

        final AccessibilityNodeInfo text = sUiAutomation.getRootInActiveWindow()
                .findAccessibilityNodeInfosByText(mActivity.getString(R.string.a_b)).get(0);

        assertFalse("Standard text view should not support SET_TEXT", text.getActionList()
                .contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_TEXT));
        assertEquals("Standard text view should not support SET_TEXT", 0,
                text.getActions() & AccessibilityNodeInfo.ACTION_SET_TEXT);
        Bundle args = new Bundle();
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                mActivity.getString(R.string.text_input_blah));
        assertFalse(text.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args));

        sInstrumentation.waitForIdleSync();
        assertTrue("Text view should not update on failed set text",
                TextUtils.equals(mActivity.getString(R.string.a_b), textView.getText()));
    }

    @Test
    public void testEditableTextView_shouldExposeAndRespondToSetTextAction() {
        final TextView textView = (TextView) mActivity.findViewById(R.id.text);

        sInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                textView.setVisibility(View.VISIBLE);
                textView.setText(mActivity.getString(R.string.a_b), TextView.BufferType.EDITABLE);
            }
        });

        final AccessibilityNodeInfo text = sUiAutomation.getRootInActiveWindow()
                .findAccessibilityNodeInfosByText(mActivity.getString(R.string.a_b)).get(0);

        assertTrue("Editable text view should support SET_TEXT", text.getActionList()
                .contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_TEXT));
        assertEquals("Editable text view should support SET_TEXT",
                AccessibilityNodeInfo.ACTION_SET_TEXT,
                text.getActions() & AccessibilityNodeInfo.ACTION_SET_TEXT);

        Bundle args = new Bundle();
        String textToSet = mActivity.getString(R.string.text_input_blah);
        args.putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, textToSet);

        assertTrue(text.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args));

        sInstrumentation.waitForIdleSync();
        assertTrue("Editable text should update on set text",
                TextUtils.equals(textToSet, textView.getText()));
    }

    @Test
    public void testEditText_shouldExposeAndRespondToSetTextAction() {
        final EditText editText = (EditText) mActivity.findViewById(R.id.edit);
        makeTextViewVisibleAndSetText(editText, mActivity.getString(R.string.a_b));

        final AccessibilityNodeInfo text = sUiAutomation.getRootInActiveWindow()
                .findAccessibilityNodeInfosByText(mActivity.getString(R.string.a_b)).get(0);

        assertTrue("EditText should support SET_TEXT", text.getActionList()
                .contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_TEXT));
        assertEquals("EditText view should support SET_TEXT",
                AccessibilityNodeInfo.ACTION_SET_TEXT,
                text.getActions() & AccessibilityNodeInfo.ACTION_SET_TEXT);

        Bundle args = new Bundle();
        String textToSet = mActivity.getString(R.string.text_input_blah);
        args.putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, textToSet);

        assertTrue(text.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args));

        sInstrumentation.waitForIdleSync();
        assertTrue("EditText should update on set text",
                TextUtils.equals(textToSet, editText.getText()));
    }

    @Test
    public void testClickableSpan_shouldWorkFromAccessibilityService() {
        final TextView textView = (TextView) mActivity.findViewById(R.id.text);
        final ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                assertEquals("Clickable span called back on wrong View", textView, widget);
                onClickCallback();
            }
        };
        final SpannableString textWithClickableSpan =
                new SpannableString(mActivity.getString(R.string.a_b));
        textWithClickableSpan.setSpan(clickableSpan, 0, 1, 0);
        makeTextViewVisibleAndSetText(textView, textWithClickableSpan);

        ClickableSpan clickableSpanFromA11y
                = findSingleSpanInViewWithText(R.string.a_b, ClickableSpan.class);
        clickableSpanFromA11y.onClick(null);
        assertOnClickCalled();
    }

    @Test
    public void testUrlSpan_shouldWorkFromAccessibilityService() {
        final TextView textView = (TextView) mActivity.findViewById(R.id.text);
        final String url = "com.android.some.random.url";
        final URLSpan urlSpan = new URLSpan(url) {
            @Override
            public void onClick(View widget) {
                assertEquals("Url span called back on wrong View", textView, widget);
                onClickCallback();
            }
        };
        final SpannableString textWithClickableSpan =
                new SpannableString(mActivity.getString(R.string.a_b));
        textWithClickableSpan.setSpan(urlSpan, 0, 1, 0);
        makeTextViewVisibleAndSetText(textView, textWithClickableSpan);

        URLSpan urlSpanFromA11y = findSingleSpanInViewWithText(R.string.a_b, URLSpan.class);
        assertEquals(url, urlSpanFromA11y.getURL());
        urlSpanFromA11y.onClick(null);

        assertOnClickCalled();
    }

    @Test
    public void testTextLocations_textViewShouldProvideWhenRequested() {
        final TextView textView = (TextView) mActivity.findViewById(R.id.text);
        // Use text with a strong s, since that gets replaced with a double s for all caps.
        // That replacement requires us to properly handle the length of the string changing.
        String stringToSet = mActivity.getString(R.string.german_text_with_strong_s);
        makeTextViewVisibleAndSetText(textView, stringToSet);
        sInstrumentation.runOnMainSync(() -> textView.setAllCaps(true));

        final AccessibilityNodeInfo text = sUiAutomation.getRootInActiveWindow()
                .findAccessibilityNodeInfosByText(stringToSet).get(0);
        List<String> textAvailableExtraData = text.getAvailableExtraData();
        assertTrue("Text view should offer text location to accessibility",
                textAvailableExtraData.contains(EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY));
        assertNull("Text locations should not be populated by default",
                text.getExtras().get(EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY));
        final Bundle getTextArgs = getTextLocationArguments(text);
        assertTrue("Refresh failed", text.refreshWithExtraData(
                AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY, getTextArgs));
        assertNodeContainsTextLocationInfoOnOneLineLTR(text);
    }

    @Test
    public void testTextLocations_textOutsideOfViewBounds_locationsShouldBeNull() {
        final EditText editText = (EditText) mActivity.findViewById(R.id.edit);
        makeTextViewVisibleAndSetText(editText, mActivity.getString(R.string.android_wiki));

        final AccessibilityNodeInfo text = sUiAutomation.getRootInActiveWindow()
                .findAccessibilityNodeInfosByText(mActivity.getString(R.string.android_wiki)).get(0);
        List<String> textAvailableExtraData = text.getAvailableExtraData();
        assertTrue("Text view should offer text location to accessibility",
                textAvailableExtraData.contains(EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY));
        final Bundle getTextArgs = getTextLocationArguments(text);
        assertTrue("Refresh failed", text.refreshWithExtraData(
                EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY, getTextArgs));
        Parcelable[] parcelables = text.getExtras()
                .getParcelableArray(EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY);
        final RectF[] locationsBeforeScroll = Arrays.copyOf(
                parcelables, parcelables.length, RectF[].class);
        assertEquals(text.getText().length(), locationsBeforeScroll.length);
        // The first character should be visible immediately
        assertFalse(locationsBeforeScroll[0].isEmpty());
        // Some of the characters should be off the screen, and thus have empty rects. Find the
        // break point
        int firstNullRectIndex = -1;
        for (int i = 1; i < locationsBeforeScroll.length; i++) {
            boolean isNull = locationsBeforeScroll[i] == null;
            if (firstNullRectIndex < 0) {
                if (isNull) {
                    firstNullRectIndex = i;
                }
            } else {
                assertTrue(isNull);
            }
        }

        // Scroll down one line
        sInstrumentation.runOnMainSync(() -> {
            int[] viewPosition = new int[2];
            editText.getLocationOnScreen(viewPosition);
            final int oneLineDownY = (int) locationsBeforeScroll[0].bottom - viewPosition[1];
            editText.scrollTo(0, oneLineDownY + 1);
        });

        assertTrue("Refresh failed", text.refreshWithExtraData(
                EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY, getTextArgs));
        parcelables = text.getExtras()
                .getParcelableArray(EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY);
        final RectF[] locationsAfterScroll = Arrays.copyOf(
                parcelables, parcelables.length, RectF[].class);
        // Now the first character should be off the screen
        assertNull(locationsAfterScroll[0]);
        // The first character that was off the screen should now be on it
        assertNotNull(locationsAfterScroll[firstNullRectIndex]);
    }

    @Test
    public void testTextLocations_withRequestPreparer_shouldHoldOffUntilReady() {
        final TextView textView = (TextView) mActivity.findViewById(R.id.text);
        makeTextViewVisibleAndSetText(textView, mActivity.getString(R.string.a_b));

        final AccessibilityNodeInfo text = sUiAutomation.getRootInActiveWindow()
                .findAccessibilityNodeInfosByText(mActivity.getString(R.string.a_b)).get(0);
        final List<String> textAvailableExtraData = text.getAvailableExtraData();
        final Bundle getTextArgs = getTextLocationArguments(text);

        // Register a request preparer that will capture the message indicating that preparation
        // is complete
        final AtomicReference<Message> messageRefForPrepare = new AtomicReference<>(null);
        // Use mockito's asynchronous signaling
        Runnable mockRunnableForPrepare = mock(Runnable.class);

        AccessibilityManager a11yManager =
                mActivity.getSystemService(AccessibilityManager.class);
        AccessibilityRequestPreparer requestPreparer = new AccessibilityRequestPreparer(
                textView, AccessibilityRequestPreparer.REQUEST_TYPE_EXTRA_DATA) {
            @Override
            public void onPrepareExtraData(int virtualViewId,
                    String extraDataKey, Bundle args, Message preparationFinishedMessage) {
                assertEquals(AccessibilityNodeProvider.HOST_VIEW_ID, virtualViewId);
                assertEquals(EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY, extraDataKey);
                assertEquals(0, args.getInt(EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_START_INDEX));
                assertEquals(text.getText().length(),
                        args.getInt(EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_LENGTH));
                messageRefForPrepare.set(preparationFinishedMessage);
                mockRunnableForPrepare.run();
            }
        };
        a11yManager.addAccessibilityRequestPreparer(requestPreparer);
        verify(mockRunnableForPrepare, times(0)).run();

        // Make the extra data request in another thread
        Runnable mockRunnableForData = mock(Runnable.class);
        new Thread(()-> {
                assertTrue("Refresh failed", text.refreshWithExtraData(
                        EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY, getTextArgs));
                mockRunnableForData.run();
        }).start();

        // The extra data request should trigger the request preparer
        verify(mockRunnableForPrepare, timeout(DEFAULT_TIMEOUT_MS)).run();
        // Verify that the request for extra data didn't return. This is a bit racy, as we may still
        // not catch it if it does return prematurely, but it does provide some protection.
        sInstrumentation.waitForIdleSync();
        verify(mockRunnableForData, times(0)).run();

        // Declare preparation for the request complete, and verify that it runs to completion
        messageRefForPrepare.get().sendToTarget();
        verify(mockRunnableForData, timeout(DEFAULT_TIMEOUT_MS)).run();
        assertNodeContainsTextLocationInfoOnOneLineLTR(text);
        a11yManager.removeAccessibilityRequestPreparer(requestPreparer);
    }

    @Test
    public void testTextLocations_withUnresponsiveRequestPreparer_shouldTimeout() {
        final TextView textView = (TextView) mActivity.findViewById(R.id.text);
        makeTextViewVisibleAndSetText(textView, mActivity.getString(R.string.a_b));

        final AccessibilityNodeInfo text = sUiAutomation.getRootInActiveWindow()
                .findAccessibilityNodeInfosByText(mActivity.getString(R.string.a_b)).get(0);
        final List<String> textAvailableExtraData = text.getAvailableExtraData();
        final Bundle getTextArgs = getTextLocationArguments(text);

        // Use mockito's asynchronous signaling
        Runnable mockRunnableForPrepare = mock(Runnable.class);

        AccessibilityManager a11yManager =
                mActivity.getSystemService(AccessibilityManager.class);
        AccessibilityRequestPreparer requestPreparer = new AccessibilityRequestPreparer(
                textView, AccessibilityRequestPreparer.REQUEST_TYPE_EXTRA_DATA) {
            @Override
            public void onPrepareExtraData(int virtualViewId,
                    String extraDataKey, Bundle args, Message preparationFinishedMessage) {
                mockRunnableForPrepare.run();
            }
        };
        a11yManager.addAccessibilityRequestPreparer(requestPreparer);
        verify(mockRunnableForPrepare, times(0)).run();

        // Make the extra data request in another thread
        Runnable mockRunnableForData = mock(Runnable.class);
        new Thread(() -> {
            /*
             * Don't worry about the return value, as we're timing out. We're just making
             * sure that we don't hang the system.
             */
            text.refreshWithExtraData(EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY, getTextArgs);
            mockRunnableForData.run();
        }).start();

        // The extra data request should trigger the request preparer
        verify(mockRunnableForPrepare, timeout(DEFAULT_TIMEOUT_MS)).run();

        // Declare preparation for the request complete, and verify that it runs to completion
        verify(mockRunnableForData, timeout(DEFAULT_TIMEOUT_MS)).run();
        a11yManager.removeAccessibilityRequestPreparer(requestPreparer);
    }

    private Bundle getTextLocationArguments(AccessibilityNodeInfo info) {
        Bundle args = new Bundle();
        args.putInt(EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_START_INDEX, 0);
        args.putInt(EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_LENGTH, info.getText().length());
        return args;
    }

    private void assertNodeContainsTextLocationInfoOnOneLineLTR(AccessibilityNodeInfo info) {
        final Parcelable[] parcelables = info.getExtras()
                .getParcelableArray(EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY);
        final RectF[] locations = Arrays.copyOf(parcelables, parcelables.length, RectF[].class);
        assertEquals(info.getText().length(), locations.length);
        // The text should all be on one line, running left to right
        for (int i = 0; i < locations.length; i++) {
            assertEquals(locations[0].top, locations[i].top, 0.01);
            assertEquals(locations[0].bottom, locations[i].bottom, 0.01);
            assertTrue(locations[i].right > locations[i].left);
            if (i > 0) {
                assertTrue(locations[i].left > locations[i-1].left);
            }
        }
    }

    private void onClickCallback() {
        synchronized (mClickableSpanCallbackLock) {
            mClickableSpanCalled.set(true);
            mClickableSpanCallbackLock.notifyAll();
        }
    }

    private void assertOnClickCalled() {
        synchronized (mClickableSpanCallbackLock) {
            long endTime = System.currentTimeMillis() + DEFAULT_TIMEOUT_MS;
            while (!mClickableSpanCalled.get() && (System.currentTimeMillis() < endTime)) {
                try {
                    mClickableSpanCallbackLock.wait(endTime - System.currentTimeMillis());
                } catch (InterruptedException e) {}
            }
        }
        assert(mClickableSpanCalled.get());
    }

    private <T> T findSingleSpanInViewWithText(int stringId, Class<T> type) {
        final AccessibilityNodeInfo text = sUiAutomation.getRootInActiveWindow()
                .findAccessibilityNodeInfosByText(mActivity.getString(stringId)).get(0);
        CharSequence accessibilityTextWithSpan = text.getText();
        // The span should work even with the node recycled
        text.recycle();
        assertTrue(accessibilityTextWithSpan instanceof Spanned);

        T spans[] = ((Spanned) accessibilityTextWithSpan)
                .getSpans(0, accessibilityTextWithSpan.length(), type);
        assertEquals(1, spans.length);
        return spans[0];
    }

    private void makeTextViewVisibleAndSetText(final TextView textView, final CharSequence text) {
        sInstrumentation.runOnMainSync(() -> {
            textView.setVisibility(View.VISIBLE);
            textView.setText(text);
        });
    }
}
