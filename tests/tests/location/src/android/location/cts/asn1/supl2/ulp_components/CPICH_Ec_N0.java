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

package android.location.cts.asn1.supl2.ulp_components;

/*
 */


//
//
import android.location.cts.asn1.base.Asn1Integer;
import android.location.cts.asn1.base.Asn1Tag;
import android.location.cts.asn1.base.BitStream;
import android.location.cts.asn1.base.BitStreamReader;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import javax.annotation.Nullable;


/**
 */
public  class CPICH_Ec_N0 extends Asn1Integer {
  //

  private static final Asn1Tag TAG_CPICH_Ec_N0
      = Asn1Tag.fromClassAndNumber(-1, -1);

  public CPICH_Ec_N0() {
    super();
    setValueRange("0", "63");

  }

  @Override
  @Nullable
  protected Asn1Tag getTag() {
    return TAG_CPICH_Ec_N0;
  }

  @Override
  protected boolean isTagImplicit() {
    return true;
  }

  public static Collection<Asn1Tag> getPossibleFirstTags() {
    if (TAG_CPICH_Ec_N0 != null) {
      return ImmutableList.of(TAG_CPICH_Ec_N0);
    } else {
      return Asn1Integer.getPossibleFirstTags();
    }
  }

  /**
   * Creates a new CPICH_Ec_N0 from encoded stream.
   */
  public static CPICH_Ec_N0 fromPerUnaligned(byte[] encodedBytes) {
    CPICH_Ec_N0 result = new CPICH_Ec_N0();
    result.decodePerUnaligned(new BitStreamReader(encodedBytes));
    return result;
  }

  /**
   * Creates a new CPICH_Ec_N0 from encoded stream.
   */
  public static CPICH_Ec_N0 fromPerAligned(byte[] encodedBytes) {
    CPICH_Ec_N0 result = new CPICH_Ec_N0();
    result.decodePerAligned(new BitStreamReader(encodedBytes));
    return result;
  }

  @Override public Iterable<BitStream> encodePerUnaligned() {
    return super.encodePerUnaligned();
  }

  @Override public Iterable<BitStream> encodePerAligned() {
    return super.encodePerAligned();
  }

  @Override public void decodePerUnaligned(BitStreamReader reader) {
    super.decodePerUnaligned(reader);
  }

  @Override public void decodePerAligned(BitStreamReader reader) {
    super.decodePerAligned(reader);
  }

  @Override public String toString() {
    return toIndentedString("");
  }

  public String toIndentedString(String indent) {
    return "CPICH_Ec_N0 = " + getInteger() + ";\n";
  }
}
