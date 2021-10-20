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

.source "T_iget_short_12.java"
.class  public Ldot/junit/opcodes/iget_short/d/T_iget_short_12;
.super  Ldot/junit/opcodes/iget_short/d/T_iget_short_1;


.method public constructor <init>()V
.registers 1

       invoke-direct {v0}, Ldot/junit/opcodes/iget_short/d/T_iget_short_1;-><init>()V
       return-void
.end method

.method public run()S
.registers 3

       iget-short v1, v2, Ldot/junit/opcodes/iget_short/d/T_iget_short_1;->pvt1:S
       return v1
.end method


