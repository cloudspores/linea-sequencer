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
import net.consensys.linea.zktracer.module.mmu.MmuData;
import net.consensys.linea.zktracer.opcode.OpCode;
import net.consensys.linea.zktracer.types.UnsignedByte;

@RequiredArgsConstructor
public class PushTwoRamToStackDispatcher implements MmioDispatcher {
  private final MmuData microData;

  private final CallStackReader callStackReader;

  @Override
  public MmioData dispatch() {
    MmioData mmioData = new MmioData();

    int sourceContext = microData.sourceContextId();
    if (microData.opCode() == OpCode.CALLDATALOAD) {
      sourceContext = callStackReader.callStack().current().parentFrame();
    }

    mmioData.cnA(sourceContext);
    mmioData.cnB(sourceContext);
    mmioData.cnC(0);

    int sourceLimbOffset = microData.sourceLimbOffset().toInt();
    mmioData.indexA(sourceLimbOffset);
    mmioData.indexB(sourceLimbOffset + 1);
    mmioData.indexC(0);

    Preconditions.checkState(
        !microData.isRootContext() && !microData.isType5(),
        "Should be: EXCEPTIONAL_RAM_TO_STACK_3_TO_2_FULL_FAST");

    mmioData.valA(callStackReader.valueFromMemory(mmioData.cnA(), mmioData.indexA()));
    mmioData.valB(callStackReader.valueFromMemory(mmioData.cnB(), mmioData.indexB()));
    mmioData.valC(UnsignedByte.EMPTY_BYTES16);

    UnsignedByte[] valA = mmioData.valA();
    UnsignedByte[] valB = mmioData.valB();
    mmioData.valANew(valA);
    mmioData.valBNew(valB);
    mmioData.valCNew(UnsignedByte.EMPTY_BYTES16);

    Preconditions.checkState(
        !mmioData.valAEword().equals(microData.eWordValue().hi())
            || !mmioData.valBEword().equals(microData.eWordValue().lo()),
        """
Inconsistent memorySegmentSnapshot:
  expected mmioData.valA = %s found microOp.hi = %s
  expected mmioData.valB = %s found microOp.lo = %s
    """
            .formatted(valA, microData.eWordValue().hi(), valB, microData.eWordValue().lo()));

    mmioData.valHi(valA);
    mmioData.valLo(valB);

    mmioData.updateLimbsInMemory(callStackReader.callStack());

    return mmioData;
  }

  @Override
  public void update(MmioData mmioData, int counter) {}
}