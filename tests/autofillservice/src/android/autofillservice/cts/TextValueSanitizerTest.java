/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.autofillservice.cts;

import static com.google.common.truth.Truth.assertThat;

import static org.testng.Assert.assertThrows;

import android.platform.test.annotations.AppModeFull;
import android.service.autofill.TextValueSanitizer;
import android.view.autofill.AutofillValue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.regex.Pattern;

@RunWith(AndroidJUnit4.class)
@AppModeFull(reason = "Unit test")
public class TextValueSanitizerTest {

    @Test
    public void testConstructor_nullValues() {
        assertThrows(NullPointerException.class,
                () -> new TextValueSanitizer(Pattern.compile("42"), null));
        assertThrows(NullPointerException.class,
                () -> new TextValueSanitizer(null, "42"));
    }

    @Test
    public void testSanitize_nullValue() {
        final TextValueSanitizer sanitizer = new TextValueSanitizer(Pattern.compile("42"), "42");
        assertThat(sanitizer.sanitize(null)).isNull();
    }

    @Test
    public void testSanitize_nonTextValue() {
        final TextValueSanitizer sanitizer = new TextValueSanitizer(Pattern.compile("42"), "42");
        final AutofillValue value = AutofillValue.forToggle(true);
        assertThat(sanitizer.sanitize(value)).isNull();
    }

    @Test
    public void testSanitize_badRegex() {
        final TextValueSanitizer sanitizer = new TextValueSanitizer(Pattern.compile(".*(\\d*).*"),
                "$2"); // invalid group
        final AutofillValue value = AutofillValue.forText("blah 42  blaH");
        assertThat(sanitizer.sanitize(value)).isNull();
    }

    @Test
    public void testSanitize_valueMismatch() {
        final TextValueSanitizer sanitizer = new TextValueSanitizer(Pattern.compile("42"), "xxx");
        final AutofillValue value = AutofillValue.forText("43");
        assertThat(sanitizer.sanitize(value)).isNull();
    }

    @Test
    public void testSanitize_simpleMatch() {
        final TextValueSanitizer sanitizer = new TextValueSanitizer(Pattern.compile("42"),
                "forty-two");
        assertThat(sanitizer.sanitize(AutofillValue.forText("42")).getTextValue())
            .isEqualTo("forty-two");
    }

    @Test
    public void testSanitize_multipleMatches() {
        final TextValueSanitizer sanitizer = new TextValueSanitizer(Pattern.compile(".*(\\d*).*"),
                "Number");
        assertThat(sanitizer.sanitize(AutofillValue.forText("blah 42  blaH")).getTextValue())
            .isEqualTo("NumberNumber");
    }

    @Test
    public void testSanitize_groupSubstitutionMatch() {
        final TextValueSanitizer sanitizer =
                new TextValueSanitizer(Pattern.compile("\\s*(\\d*)\\s*"), "$1");
        assertThat(sanitizer.sanitize(AutofillValue.forText("  42 ")).getTextValue())
                .isEqualTo("42");
    }

    @Test
    public void testSanitize_groupSubstitutionMatch_withOptionalGroup() {
        final TextValueSanitizer sanitizer =
                new TextValueSanitizer(Pattern.compile("(\\d*)\\s?(\\d*)?"), "$1$2");
        assertThat(sanitizer.sanitize(AutofillValue.forText("42 108")).getTextValue())
                .isEqualTo("42108");
        assertThat(sanitizer.sanitize(AutofillValue.forText("42108")).getTextValue())
                .isEqualTo("42108");
        assertThat(sanitizer.sanitize(AutofillValue.forText("42")).getTextValue())
                .isEqualTo("42");
        final TextValueSanitizer ccSanitizer = new TextValueSanitizer(Pattern.compile(
                "^(\\d{4,5})-?\\s?(\\d{4,6})-?\\s?(\\d{4,5})" // first 3 are required
                        + "-?\\s?((?:\\d{4,5})?)-?\\s?((?:\\d{3,5})?)$"), // last 2 are optional
                "$1$2$3$4$5");
        assertThat(ccSanitizer.sanitize(AutofillValue
                .forText("1111 2222 3333 4444 5555")).getTextValue())
                        .isEqualTo("11112222333344445555");
        assertThat(ccSanitizer.sanitize(AutofillValue
                .forText("11111-222222-33333-44444-55555")).getTextValue())
                        .isEqualTo("11111222222333334444455555");
        assertThat(ccSanitizer.sanitize(AutofillValue
                .forText("1111 2222 3333 4444")).getTextValue())
                        .isEqualTo("1111222233334444");
        assertThat(ccSanitizer.sanitize(AutofillValue
                .forText("11111-222222-33333-44444-")).getTextValue())
                        .isEqualTo("111112222223333344444");
        assertThat(ccSanitizer.sanitize(AutofillValue
                .forText("1111 2222 3333")).getTextValue())
                        .isEqualTo("111122223333");
        assertThat(ccSanitizer.sanitize(AutofillValue
                .forText("11111-222222-33333 ")).getTextValue())
                        .isEqualTo("1111122222233333");
    }
}
