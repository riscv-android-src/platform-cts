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

.source "T_filled_new_array_8.java"
.class  public Ldot/junit/opcodes/filled_new_array/d/T_filled_new_array_8;
.super  Ljava/lang/Object;


.method public constructor <init>()V
.registers 10

       invoke-direct {v9}, Ljava/lang/Object;-><init>()V
       
       const v5, 0
       const v6, 0
       const v7, 0
       const v8, 0
       const v9, 0
       filled-new-array {v5, v6, v7, v8, v9}, [I
       move-result-object v0
       return-void
.end method

.method public run(IIIII)[I
.registers 10
    filled-new-array {v5, v6, v7, v8, v9}, [I
    move-result-object v0
    return-object v0
.end method


