/*
 * Copyright (C) 2009 The Android Open Source Project
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

package android.provider.cts;

import static android.provider.cts.MediaStoreTest.TAG;
import static android.provider.cts.ProviderTestUtils.assertExists;
import static android.provider.cts.ProviderTestUtils.assertNotExists;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaExtractor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.os.storage.StorageManager;
import android.platform.test.annotations.Presubmit;
import android.provider.MediaStore;
import android.provider.MediaStore.Video.Media;
import android.provider.MediaStore.Video.VideoColumns;
import android.provider.cts.MediaStoreUtils.PendingParams;
import android.provider.cts.MediaStoreUtils.PendingSession;
import android.util.Log;

import androidx.test.InstrumentationRegistry;

import com.android.compatibility.common.util.FileUtils;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

@Presubmit
@RunWith(Parameterized.class)
public class MediaStore_Video_MediaTest {
    private Context mContext;
    private ContentResolver mContentResolver;

    private Uri mExternalVideo;

    @Parameter(0)
    public String mVolumeName;

    @Parameters
    public static Iterable<? extends Object> data() {
        return ProviderTestUtils.getSharedVolumeNames();
    }

    @Before
    public void setUp() throws Exception {
        mContext = InstrumentationRegistry.getTargetContext();
        mContentResolver = mContext.getContentResolver();

        Log.d(TAG, "Using volume " + mVolumeName);
        mExternalVideo = MediaStore.Video.Media.getContentUri(mVolumeName);
    }

    @Test
    public void testGetContentUri() {
        Cursor c = null;
        assertNotNull(c = mContentResolver.query(Media.getContentUri("internal"), null, null, null,
                null));
        c.close();
        assertNotNull(c = mContentResolver.query(Media.getContentUri(mVolumeName), null, null, null,
                null));
        c.close();
    }

    private void cleanExternalMediaFile(String path) {
        mContentResolver.delete(mExternalVideo, "_data=?", new String[] { path });
        new File(path).delete();
    }

    @Test
    public void testStoreVideoMediaExternal() throws Exception {
        final String externalVideoPath = new File(ProviderTestUtils.stageDir(mVolumeName),
                "testvideo.3gp").getAbsolutePath();
        final String externalVideoPath2 = new File(ProviderTestUtils.stageDir(mVolumeName),
                "testvideo1.3gp").getAbsolutePath();

        // clean up any potential left over entries from a previous aborted run
        cleanExternalMediaFile(externalVideoPath);
        cleanExternalMediaFile(externalVideoPath2);

        int numBytes = 1337;
        File videoFile = new File(externalVideoPath);
        FileUtils.createFile(videoFile, numBytes);

        ContentValues values = new ContentValues();
        values.put(Media.ALBUM, "cts");
        values.put(Media.ARTIST, "cts team");
        values.put(Media.CATEGORY, "test");
        long dateTaken = System.currentTimeMillis();
        values.put(Media.DATE_TAKEN, dateTaken);
        values.put(Media.DESCRIPTION, "This is a video");
        values.put(Media.DURATION, 8480);
        values.put(Media.LANGUAGE, "en");
        values.put(Media.IS_PRIVATE, 1);
        values.put(Media.MINI_THUMB_MAGIC, 0);
        values.put(Media.RESOLUTION, "176x144");
        values.put(Media.TAGS, "cts, test");
        values.put(Media.DATA, externalVideoPath);
        values.put(Media.DISPLAY_NAME, "testvideo.3gp");
        values.put(Media.MIME_TYPE, "video/3gpp");
        values.put(Media.SIZE, numBytes);
        values.put(Media.TITLE, "testvideo");
        long dateAdded = System.currentTimeMillis() / 1000;
        values.put(Media.DATE_ADDED, dateAdded);
        long dateModified = videoFile.lastModified() / 1000;
        values.put(Media.DATE_MODIFIED, dateModified);

        // insert
        Uri uri = mContentResolver.insert(mExternalVideo, values);
        assertNotNull(uri);

        try {
            // query
            Cursor c = mContentResolver.query(uri, null, null, null, null);
            assertEquals(1, c.getCount());
            c.moveToFirst();
            long id = c.getLong(c.getColumnIndex(Media._ID));
            assertTrue(id > 0);
            assertEquals("cts", c.getString(c.getColumnIndex(Media.ALBUM)));
            assertEquals("cts team", c.getString(c.getColumnIndex(Media.ARTIST)));
            assertEquals("test", c.getString(c.getColumnIndex(Media.CATEGORY)));
            assertEquals(dateTaken, c.getLong(c.getColumnIndex(Media.DATE_TAKEN)));
            assertEquals(8480, c.getInt(c.getColumnIndex(Media.DURATION)));
            assertEquals("This is a video",
                    c.getString(c.getColumnIndex(Media.DESCRIPTION)));
            assertEquals("en", c.getString(c.getColumnIndex(Media.LANGUAGE)));
            assertEquals(1, c.getInt(c.getColumnIndex(Media.IS_PRIVATE)));
            assertEquals(0, c.getLong(c.getColumnIndex(Media.MINI_THUMB_MAGIC)));
            assertEquals("176x144", c.getString(c.getColumnIndex(Media.RESOLUTION)));
            assertEquals("cts, test", c.getString(c.getColumnIndex(Media.TAGS)));
            assertEquals(externalVideoPath, c.getString(c.getColumnIndex(Media.DATA)));
            assertEquals("testvideo.3gp", c.getString(c.getColumnIndex(Media.DISPLAY_NAME)));
            assertEquals("video/3gpp", c.getString(c.getColumnIndex(Media.MIME_TYPE)));
            assertEquals("testvideo", c.getString(c.getColumnIndex(Media.TITLE)));
            assertEquals(numBytes, c.getInt(c.getColumnIndex(Media.SIZE)));
            long realDateAdded = c.getLong(c.getColumnIndex(Media.DATE_ADDED));
            assertTrue(realDateAdded >= dateAdded);
            assertEquals(dateModified, c.getLong(c.getColumnIndex(Media.DATE_MODIFIED)));
            c.close();
        } finally {
            // delete
            assertEquals(1, mContentResolver.delete(uri, null, null));
            new File(externalVideoPath).delete();
        }

        // check that the video file is removed when deleting the database entry
        Context context = mContext;
        Uri videoUri = insertVideo(context);
        File videofile = new File(ProviderTestUtils.stageDir(mVolumeName), "testVideo.3gp");
        assertExists(videofile);
        mContentResolver.delete(videoUri, null, null);
        assertNotExists(videofile);
    }

    private Uri insertVideo(Context context) throws IOException {
        final File dir = ProviderTestUtils.stageDir(mVolumeName);
        final File file = new File(dir, "testVideo.3gp");
        // clean up any potential left over entries from a previous aborted run
        cleanExternalMediaFile(file.getAbsolutePath());

        ProviderTestUtils.stageFile(R.raw.testvideo, file);

        ContentValues values = new ContentValues();
        values.put(VideoColumns.DATA, file.getAbsolutePath());
        return context.getContentResolver().insert(mExternalVideo, values);
    }

    /**
     * This test doesn't hold
     * {@link android.Manifest.permission#ACCESS_MEDIA_LOCATION}, so Exif and XMP
     * location information should be redacted.
     */
    @Test
    public void testLocationRedaction() throws Exception {
        // STOPSHIP: remove this once isolated storage is always enabled
        Assume.assumeTrue(StorageManager.hasIsolatedStorage());

        final String displayName = "cts" + System.nanoTime();
        final PendingParams params = new PendingParams(
                mExternalVideo, displayName, "video/mp4");

        final Uri pendingUri = MediaStoreUtils.createPending(mContext, params);
        final Uri publishUri;
        try (PendingSession session = MediaStoreUtils.openPending(mContext, pendingUri)) {
            try (InputStream in = mContext.getResources().openRawResource(R.raw.testvideo_meta);
                 OutputStream out = session.openOutputStream()) {
                android.os.FileUtils.copy(in, out);
            }
            publishUri = session.publish();
        }

        final Uri originalUri = MediaStore.setRequireOriginal(publishUri);

        // Since we own the video, we should be able to see the location
        // we ourselves contributed
        try (ParcelFileDescriptor pfd = mContentResolver.openFile(publishUri, "r", null);
                MediaMetadataRetriever mmr = new MediaMetadataRetriever()) {
            mmr.setDataSource(pfd.getFileDescriptor());
            assertEquals("+37.4217-122.0834/",
                    mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION));
            assertEquals("2", mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_NUM_TRACKS));
        }
        try (InputStream in = mContentResolver.openInputStream(publishUri)) {
            byte[] bytes = FileUtils.readInputStreamFully(in);
            byte[] xmpBytes = Arrays.copyOfRange(bytes, 3269, 3269 + 13197);
            String xmp = new String(xmpBytes);
            assertTrue("Failed to read XMP longitude", xmp.contains("10,41.751000E"));
            assertTrue("Failed to read XMP latitude", xmp.contains("53,50.070500N"));
            assertTrue("Failed to read non-location XMP", xmp.contains("13166/7763"));
        }
        // As owner, we should be able to request the original bytes
        try (ParcelFileDescriptor pfd = mContentResolver.openFileDescriptor(originalUri, "r")) {
        }

        // Now remove ownership, which means that location should be redacted
        ProviderTestUtils.executeShellCommand(
                "content update --uri " + publishUri + " --bind owner_package_name:n:",
                InstrumentationRegistry.getInstrumentation().getUiAutomation());
        try (ParcelFileDescriptor pfd = mContentResolver.openFile(publishUri, "r", null);
                MediaMetadataRetriever mmr = new MediaMetadataRetriever()) {
            mmr.setDataSource(pfd.getFileDescriptor());
            assertEquals(null,
                    mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION));
            assertEquals("2", mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_NUM_TRACKS));
        }
        try (InputStream in = mContentResolver.openInputStream(publishUri)) {
            byte[] bytes = FileUtils.readInputStreamFully(in);
            byte[] xmpBytes = Arrays.copyOfRange(bytes, 3269, 3269 + 13197);
            String xmp = new String(xmpBytes);
            assertFalse("Failed to redact XMP longitude", xmp.contains("10,41.751000E"));
            assertFalse("Failed to redact XMP latitude", xmp.contains("53,50.070500N"));
            assertTrue("Redacted non-location XMP", xmp.contains("13166/7763"));
        }
        // We can't request original bytes unless we have permission
        try (ParcelFileDescriptor pfd = mContentResolver.openFileDescriptor(originalUri, "r")) {
            fail("Able to read original content without ACCESS_MEDIA_LOCATION");
        } catch (UnsupportedOperationException expected) {
        }
    }

    @Test
    public void testLocationDeprecated() throws Exception {
        final String displayName = "cts" + System.nanoTime();
        final PendingParams params = new PendingParams(
                mExternalVideo, displayName, "video/mp4");

        final Uri pendingUri = MediaStoreUtils.createPending(mContext, params);
        final Uri publishUri;
        try (PendingSession session = MediaStoreUtils.openPending(mContext, pendingUri)) {
            try (InputStream in = mContext.getResources().openRawResource(R.raw.testvideo_meta);
                    OutputStream out = session.openOutputStream()) {
                android.os.FileUtils.copy(in, out);
            }
            publishUri = session.publish();
        }

        // Verify that location wasn't indexed
        try (Cursor c = mContentResolver.query(publishUri,
                new String[] { VideoColumns.LATITUDE, VideoColumns.LONGITUDE }, null, null)) {
            assertTrue(c.moveToFirst());
            assertTrue(c.isNull(0));
            assertTrue(c.isNull(1));
        }

        // Verify that location values aren't recorded
        final ContentValues values = new ContentValues();
        values.put(VideoColumns.LATITUDE, 32f);
        values.put(VideoColumns.LONGITUDE, 64f);
        mContentResolver.update(publishUri, values, null, null);

        try (Cursor c = mContentResolver.query(publishUri,
                new String[] { VideoColumns.LATITUDE, VideoColumns.LONGITUDE }, null, null)) {
            assertTrue(c.moveToFirst());
            assertTrue(c.isNull(0));
            assertTrue(c.isNull(1));
        }
    }

    @Test
    public void testCanonicalize() throws Exception {
        // Remove all audio left over from other tests
        ProviderTestUtils.executeShellCommand(
                "content delete --uri " + mExternalVideo,
                InstrumentationRegistry.getInstrumentation().getUiAutomation());

        // Publish some content
        final File dir = ProviderTestUtils.stageDir(mVolumeName);
        final Uri a = ProviderTestUtils.scanFileFromShell(
                ProviderTestUtils.stageFile(R.raw.testvideo, new File(dir, "a.mp4")));
        final Uri b = ProviderTestUtils.scanFileFromShell(
                ProviderTestUtils.stageFile(R.raw.testvideo_meta, new File(dir, "b.mp4")));
        final Uri c = ProviderTestUtils.scanFileFromShell(
                ProviderTestUtils.stageFile(R.raw.testvideo, new File(dir, "c.mp4")));

        // Confirm we can canonicalize and recover it
        final Uri canonicalized = mContentResolver.canonicalize(b);
        assertNotNull(canonicalized);
        assertEquals(b, mContentResolver.uncanonicalize(canonicalized));

        // Delete all items above
        mContentResolver.delete(a, null, null);
        mContentResolver.delete(b, null, null);
        mContentResolver.delete(c, null, null);

        // Confirm canonical item isn't found
        assertNull(mContentResolver.uncanonicalize(canonicalized));

        // Publish data again and confirm we can recover it
        final Uri d = ProviderTestUtils.scanFileFromShell(
                ProviderTestUtils.stageFile(R.raw.testvideo_meta, new File(dir, "d.mp4")));
        assertEquals(d, mContentResolver.uncanonicalize(canonicalized));
    }

    @Test
    public void testMetadata() throws Exception {
        final Uri uri = ProviderTestUtils.stageMedia(R.raw.testvideo_meta, mExternalVideo,
                "video/mp4");

        try (Cursor c = mContentResolver.query(uri, null, null, null)) {
            assertTrue(c.moveToFirst());

            // Confirm that we parsed Exif metadata
            assertEquals(9296, c.getLong(c.getColumnIndex(VideoColumns.DURATION)));
            assertEquals(1920, c.getLong(c.getColumnIndex(VideoColumns.WIDTH)));
            assertEquals(1080, c.getLong(c.getColumnIndex(VideoColumns.HEIGHT)));

            // Confirm that we parsed XMP metadata
            assertEquals("xmp.did:051dfd42-0b46-4302-918a-836fba5016ed",
                    c.getString(c.getColumnIndex(VideoColumns.DOCUMENT_ID)));
            assertEquals("xmp.iid:051dfd42-0b46-4302-918a-836fba5016ed",
                    c.getString(c.getColumnIndex(VideoColumns.INSTANCE_ID)));
            assertEquals("4F9DD7A46B26513A7C35272F0D623A06",
                    c.getString(c.getColumnIndex(VideoColumns.ORIGINAL_DOCUMENT_ID)));

            // Confirm that timestamp was parsed
            assertEquals(1539711603000L, c.getLong(c.getColumnIndex(VideoColumns.DATE_TAKEN)));

            // We just added and modified the file, so should be recent
            final long added = c.getLong(c.getColumnIndex(VideoColumns.DATE_ADDED));
            final long modified = c.getLong(c.getColumnIndex(VideoColumns.DATE_MODIFIED));
            final long now = System.currentTimeMillis() / 1000;
            assertTrue("Invalid added time " + added, Math.abs(added - now) < 5);
            assertTrue("Invalid modified time " + modified, Math.abs(modified - now) < 5);

            // Confirm that we trusted value from XMP metadata
            assertEquals("video/dng", c.getString(c.getColumnIndex(VideoColumns.MIME_TYPE)));

            assertEquals(20716, c.getLong(c.getColumnIndex(VideoColumns.SIZE)));

            final String displayName = c.getString(c.getColumnIndex(VideoColumns.DISPLAY_NAME));
            assertTrue("Invalid display name " + displayName, displayName.startsWith("cts"));
            assertTrue("Invalid display name " + displayName, displayName.endsWith(".mp4"));
        }
    }
}
