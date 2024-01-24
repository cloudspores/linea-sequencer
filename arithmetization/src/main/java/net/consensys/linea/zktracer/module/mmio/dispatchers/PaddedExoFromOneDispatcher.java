/*
 * Copyright Consensys Software Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package net.consensys.linea.zktracer.module.mmio.dispatchers;

import com.google.common.base.Preconditions;
import lombok.RequiredArgsConstructor;
import net.consensys.linea.zktracer.module.mmio.CallStackReader;
import net.consensys.linea.zktracer.module.mmio.MmioData;
import net.consensys.linea.zktracer.module.mmio.PowType;
import net.consensys.linea.zktracer.runtime.microdata.MicroData;
import net.consensys.linea.zktracer.types.UnsignedByte;

@RequiredArgsConstructor
public class PaddedExoFromOneDispatcher implements MmioDispatcher {
  private final MicroData microData;

  private final CallStackReader callStackReader;

  @Override
  public MmioData dispatch() {
    MmioData mmioData = new MmioData();

    int sourceContext = microData.sourceContext();
    mmioData.cnA(sourceContext);
    mmioData.cnB(0);
    mmioData.cnC(0);

    int sourceLimbOffset = microData.sourceLimbOffset().toInt();
    int targetLimbOffset = microData.targetLimbOffset().toInt();
    mmioData.indexA(sourceLimbOffset);
    mmioData.indexB(0);
    mmioData.indexC(0);
    mmioData.indexX(targetLimbOffset);

    mmioData.valA(callStackReader.valueFromMemory(mmioData.cnA(), mmioData.indexA()));
    mmioData.valB(UnsignedByte.EMPTY_BYTES16);
    mmioData.valC(UnsignedByte.EMPTY_BYTES16);
    mmioData.valX(UnsignedByte.EMPTY_BYTES16);

    int sourceByteOffset = microData.sourceByteOffset().toInteger();
    int size = microData.size();
    boolean wrongOffsets =
        sourceByteOffset < 0 || sourceByteOffset >= 16 || sourceByteOffset + size > 16;

    Preconditions.checkArgument(
        wrongOffsets,
        """
Wrong size/sourceByteOffset combo in PaddedExoFromOneDispatcher:
  sourceByteOffset = %s
  size = %d
  sourceByteOffset + size = %d > 16
  """
            .formatted(sourceByteOffset, size, sourceByteOffset + size));

    System.arraycopy(mmioData.valA(), sourceByteOffset, mmioData.valX(), 0, size);

    mmioData.valANew(mmioData.valA());
    mmioData.valBNew(UnsignedByte.EMPTY_BYTES16);
    mmioData.valCNew(UnsignedByte.EMPTY_BYTES16);

    mmioData.updateLimbsInMemory(callStackReader.callStack());

    return mmioData;
  }

  @Override
  public void update(MmioData mmioData, int counter) {
    mmioData.oneToOnePadded(
        mmioData.valA(),
        mmioData.byteA(counter),
        mmioData.acc1(),
        PowType.POW_256_1,
        microData.sourceByteOffset(),
        microData.size(),
        counter);
  }
}
