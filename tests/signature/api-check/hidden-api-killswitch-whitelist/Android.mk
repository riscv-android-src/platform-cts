# Copyright (C) 2018 The Android Open Source Project
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

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_PACKAGE_NAME := CtsHiddenApiKillswitchWhitelistTestCases
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_PATH := $(TARGET_OUT_DATA_APPS)
LOCAL_MULTILIB := both
LOCAL_JNI_SHARED_LIBRARIES := libcts_dexchecker libclassdescriptors
LOCAL_NDK_STL_VARIANT := c++_static

# Tag this module as a cts test artifact
LOCAL_COMPATIBILITY_SUITE := cts vts general-tests
LOCAL_SDK_VERSION := current
LOCAL_STATIC_JAVA_LIBRARIES := cts-api-signature-test

LOCAL_USE_EMBEDDED_NATIVE_LIBS := false

include $(BUILD_CTS_PACKAGE)
