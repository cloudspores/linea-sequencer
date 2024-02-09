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

import lombok.RequiredArgsConstructor;
import net.consensys.linea.zktracer.module.mmio.CallStackReader;
import net.consensys.linea.zktracer.module.mmio.MmioData;
import net.consensys.linea.zktracer.module.mmu.MmuData;

@RequiredArgsConstructor
public class RamToRamSlideOverlappingChunkDispatcher implements MmioDispatcher {
  private final MmuData mmuData;

  private final CallStackReader callStackReader;

  @Override
  public MmioData dispatch() {
    MmioData mmioData = new MmioData();
    mmioData.cnA(mmuData.sourceContextId());
    mmioData.cnB(mmuData.targetContextId());
    mmioData.cnC(0);

    int sourceLimbOffset = mmuData.sourceLimbOffset().toInt();
    mmioData.indexA(sourceLimbOffset);

    int targetedLimbOffset = mmuData.targetLimbOffset().toInt();
    mmioData.indexB(targetedLimbOffset);
    mmioData.indexC(targetedLimbOffset + 1);

    mmioData.valA(callStackReader.valueFromMemory(mmioData.cnA(), mmioData.indexA()));
    mmioData.valB(callStackReader.valueFromMemory(mmioData.cnB(), mmioData.indexB()));
    mmioData.valC(callStackReader.valueFromMemory(mmioData.cnC(), mmioData.indexC()));

    mmioData.valANew(mmioData.valA());
    mmioData.valBNew(mmioData.valB());

    int targetByteOffset = mmuData.targetByteOffset().toInteger();
    int sourceByteOffset = mmuData.sourceByteOffset().toInteger();

    System.arraycopy(
        mmioData.valA(),
        sourceByteOffset - targetByteOffset,
        mmioData.valBNew(),
        targetByteOffset,
        16 - targetByteOffset);

    mmioData.valCNew(mmioData.valC());

    int size = mmuData.mmuToMmioInstructions().size();
    int maxIndex = size - (16 - targetByteOffset);
    for (int i = 0; i < maxIndex; i++) {
      mmioData.valCNew()[i] = mmioData.valA()[sourceByteOffset + (16 - targetByteOffset) + i];
    }

    mmioData.updateLimbsInMemory(callStackReader.callStack());

    return mmioData;
  }

  @Override
  public void update(MmioData mmioData, int counter) {
    mmioData.onePartialToTwo(
        mmioData.byteA(counter),
        mmioData.byteB(counter),
        mmioData.byteC(counter),
        mmioData.acc1(),
        mmioData.acc2(),
        mmioData.acc3(),
        mmioData.acc4(),
        mmuData.sourceByteOffset(),
        mmuData.targetByteOffset(),
        mmuData.mmuToMmioInstructions().size(),
        counter);
  }
}
