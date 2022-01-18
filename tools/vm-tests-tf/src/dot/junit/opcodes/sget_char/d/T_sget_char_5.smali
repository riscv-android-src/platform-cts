# Copyright (C) 2008 The Android Open Source Project
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

.source "T_sget_char_5.java"
.class  public Ldot/junit/opcodes/sget_char/d/T_sget_char_5;
.super  Ljava/lang/Object;

.field public i1:C

.method public constructor <init>()V
.registers 4

       invoke-direct {v3}, Ljava/lang/Object;-><init>()V

       const v2, 77
       iput-char v2, v3, Ldot/junit/opcodes/sget_char/d/T_sget_char_5;->i1:C
       return-void
.end method

.method public run()C
.registers 3

       sget-char v1, Ldot/junit/opcodes/sget_char/d/T_sget_char_5;->i1:C
       return v1
.end method


