# Copyright 2018 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import os

import its.caps
import its.device
import its.image
import its.objects
import numpy as np

NAME = os.path.basename(__file__).split('.')[0]
PATTERN = 2  # Note scene0/test_test_patterns must PASS
COLOR_BARS = ['WHITE', 'YELLOW', 'CYAN', 'GREEN', 'MAGENTA', 'RED',
              'BLUE', 'BLACK']
COLOR_CHECKER = {'BLACK': [0, 0, 0], 'RED': [1, 0, 0], 'GREEN': [0, 1, 0],
                 'BLUE': [0, 0, 1], 'MAGENTA': [1, 0, 1], 'CYAN': [0, 1, 1],
                 'YELLOW': [1, 1, 0], 'WHITE': [1, 1, 1]}
DELTA = 0.0005  # crop on edge of color bars
RAW_TOL = 0.001  # 1 DN in [0:1] (1/(1023-64)
RGB_VAR_TOL = 0.0039  # 1/255
RGB_MEAN_TOL = 0.1
TONEMAP_MAX = 0.5


def check_raw_pattern(img_raw):
    """Check for RAW capture matches color bar pattern.

    Args:
        img_raw: RAW image
    """

    print 'Checking RAW/PATTERN match'
    n_bars = len(COLOR_BARS)
    color_match = []
    for i in range(n_bars):
        print 'patch:', i,
        raw_patch = its.image.get_image_patch(
                img_raw, float(i)/n_bars+DELTA, 0.0, 1.0/n_bars-2*DELTA, 1.0)
        raw_means = its.image.compute_image_means(raw_patch)
        for color in COLOR_BARS:
            if np.allclose(COLOR_CHECKER[color], raw_means, atol=RAW_TOL):
                color_match.append(color)
                print '%s' % color
    assert set(color_match) == set(COLOR_BARS), 'RAW does not have all colors'


def check_yuv_vs_raw(img_raw, img_yuv):
    """Check for YUV vs RAW match in 8 patches.

    Check for correct values and color consistency

    Args:
        img_raw: RAW image
        img_yuv: YUV image
    """

    print 'Checking YUV/RAW match'
    n_bars = len(COLOR_BARS)
    color_match_errs = []
    color_variance_errs = []
    for i in range(n_bars):
        raw_patch = its.image.get_image_patch(
                img_raw, float(i)/n_bars+DELTA, 0.0, 1.0/n_bars-2*DELTA, 1.0)
        yuv_patch = its.image.get_image_patch(
                img_yuv, float(i)/n_bars+DELTA, 0.0, 1.0/n_bars-2*DELTA, 1.0)
        raw_means = np.array(its.image.compute_image_means(raw_patch))
        raw_vars = np.array(its.image.compute_image_variances(raw_patch))
        yuv_means = np.array(its.image.compute_image_means(yuv_patch))
        yuv_means /= TONEMAP_MAX  # Normalize to tonemap max
        yuv_vars = np.array(its.image.compute_image_variances(yuv_patch))
        if not np.allclose(raw_means, yuv_means, atol=RGB_MEAN_TOL):
            color_match_errs.append('RAW: %s, RGB(norm): %s, ATOL: %.2f' % (
                    str(raw_means), str(np.round(yuv_means, 3)), RGB_MEAN_TOL))
        if not np.allclose(raw_vars, yuv_vars, atol=RGB_VAR_TOL):
            color_variance_errs.append('RAW: %s, RGB: %s, ATOL: %.4f' % (
                    str(raw_vars), str(yuv_vars), RGB_VAR_TOL))
    if color_match_errs:
        print '\nColor match errors'
        for err in color_match_errs:
            print err
    if color_variance_errs:
        print '\nColor variance errors'
        for err in color_variance_errs:
            print err
    assert not color_match_errs
    assert not color_variance_errs


def test_tonemap_curve(cam, props):
    """test tonemap curve with sensor test pattern.

    Args:
        cam: An open device session.
        props: Properties of cam
    """

    avail_patterns = props['android.sensor.availableTestPatternModes']
    print 'avail_patterns: ', avail_patterns
    sens_min, _ = props['android.sensor.info.sensitivityRange']
    exp = min(props['android.sensor.info.exposureTimeRange'])

    # Linear tonemap with maximum of 0.5
    tmap = sum([[i/63.0, i/126.0] for i in range(64)], [])

    if PATTERN in avail_patterns:
        # RAW image
        req_raw = its.objects.manual_capture_request(int(sens_min), exp)
        req_raw['android.sensor.testPatternMode'] = PATTERN
        fmt_raw = {'format': 'raw'}
        cap_raw = cam.do_capture(req_raw, fmt_raw)
        img_raw = its.image.convert_capture_to_rgb_image(
                cap_raw, props=props)

        # Save RAW pattern
        its.image.write_image(img_raw, '%s_raw_%d.jpg' % (
                NAME, PATTERN), True)
        check_raw_pattern(img_raw)

        # YUV image
        req_yuv = its.objects.manual_capture_request(int(sens_min), exp)
        req_yuv['android.sensor.testPatternMode'] = PATTERN
        req_yuv['android.distortionCorrection.mode'] = 0
        req_yuv['android.tonemap.mode'] = 0
        req_yuv['android.tonemap.curve'] = {
                'red': tmap, 'green': tmap, 'blue': tmap}
        fmt_yuv = {'format': 'yuv', 'width': 640, 'height': 480}
        cap_yuv = cam.do_capture(req_yuv, fmt_yuv)
        img_yuv = its.image.convert_capture_to_rgb_image(cap_yuv, True)

        # Save YUV pattern
        its.image.write_image(img_yuv, '%s_yuv_%d.jpg' % (
                NAME, PATTERN), True)

        # Check pattern for correctness
        check_yuv_vs_raw(img_raw, img_yuv)
    else:
        print 'Pattern not in android.sensor.availableTestPatternModes.'
        assert 0


def main():
    """Test conversion of test pattern from RAW to YUV.

    android.sensor.testPatternMode
    2: COLOR_BARS
    """

    print '\nStarting %s' % NAME
    with its.device.ItsSession() as cam:
        props = cam.get_camera_properties()
        its.caps.skip_unless(its.caps.raw16(props) and
                             its.caps.manual_sensor(props) and
                             its.caps.per_frame_control(props) and
                             its.caps.manual_post_proc(props))

        test_tonemap_curve(cam, props)

if __name__ == '__main__':
    main()
