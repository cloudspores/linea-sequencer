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
import net.consensys.linea.zktracer.module.romLex.RomLex;
import net.consensys.linea.zktracer.types.UnsignedByte;
import org.apache.tuweni.bytes.Bytes;

@RequiredArgsConstructor
public class StoreXInAThreeRequiredDispatcher implements MmioDispatcher {
  private final MmuData microData;

  private final CallStackReader callStackReader;

  private final RomLex romLex;

  @Override
  public MmioData dispatch() {
    MmioData mmioData = new MmioData();
    mmioData.cnA(0);
    mmioData.cnB(0);
    mmioData.cnC(0);

    int sourceLimbOffset = microData.sourceLimbOffset().toInt();
    mmioData.indexA(0);
    mmioData.indexB(0);
    mmioData.indexC(0);
    mmioData.indexX(sourceLimbOffset);

    Bytes contractByteCode = romLex.addressRomChunkMap().get(null).byteCode();

    microData.valACache(
        callStackReader.valueFromExo(contractByteCode, microData.exoSource(), mmioData.indexX()));
    microData.valBCache(
        callStackReader.valueFromExo(
            contractByteCode, microData.exoSource(), mmioData.indexX() + 1));
    microData.valCCache(
        callStackReader.valueFromExo(
            contractByteCode, microData.exoSource(), mmioData.indexX() + 2));

    mmioData.valA(microData.valACache());
    mmioData.valB(microData.valBCache());
    mmioData.valC(microData.valCCache());
    mmioData.valX(microData.valACache());

    mmioData.valANew(UnsignedByte.EMPTY_BYTES16);
    mmioData.valBNew(UnsignedByte.EMPTY_BYTES16);
    mmioData.valCNew(UnsignedByte.EMPTY_BYTES16);

    int sourceByteOffset = microData.sourceByteOffset().toInteger();
    mmioData.setValHiLoForRootContextCalldataload(sourceByteOffset);
    mmioData.updateLimbsInMemory(callStackReader.callStack());

    return mmioData;
  }

  @Override
  public void update(MmioData mmioData, int counter) {}
}