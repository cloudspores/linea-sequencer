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

package net.consensys.linea.zktracer.module.mmio;

import static net.consensys.linea.zktracer.module.mmio.MmioData.isFastOperation;
import static net.consensys.linea.zktracer.module.mmio.MmioData.numberOfRowOfMmioInstruction;
import static net.consensys.linea.zktracer.types.Conversions.unsignedBytesToBytes;

import java.nio.MappedByteBuffer;
import java.util.List;

import net.consensys.linea.zktracer.ColumnHeader;
import net.consensys.linea.zktracer.module.Module;
import net.consensys.linea.zktracer.module.mmu.Mmu;
import net.consensys.linea.zktracer.module.mmu.MmuData;
import net.consensys.linea.zktracer.module.mmu.MmuOperation;
import net.consensys.linea.zktracer.module.mmu.values.MmuToMmioConstantValues;
import net.consensys.linea.zktracer.runtime.callstack.CallStack;
import net.consensys.linea.zktracer.types.UnsignedByte;
import org.apache.tuweni.bytes.Bytes;

public class Mmio implements Module {
  private final Mmu mmu;
  private final CallStackReader callStackReader;

  public Mmio(Mmu mmu, CallStack callStack) {
    this.mmu = mmu;
    this.callStackReader = new CallStackReader(callStack);
  }

  @Override
  public String moduleKey() {
    return "MMIO";
  }

  @Override
  public void enterTransaction() {}

  @Override
  public void popTransaction() {}

  @Override
  public int lineCount() {
    int sum = 0;
    for (MmuOperation o : mmu.mmuOperations()) {
      sum += o.computeMmioLineCount();
    }

    return sum;
  }

  @Override
  public List<ColumnHeader> columnsHeaders() {
    return Trace.headers(this.lineCount());
  }

  @Override
  public void commit(List<MappedByteBuffer> buffers) {
    Trace trace = new Trace(buffers);
    int stamp = 0;
    for (MmuOperation mmuOperation : this.mmu.mmuOperations()) {
      final MmuData currentMmuData = mmuOperation.mmuData();

      final MmuToMmioConstantValues mmuToMmioConstantValues =
          currentMmuData.mmuToMmioConstantValues();

      final int sourceContextNumber = mmuToMmioConstantValues.sourceContextNumber();
      if (sourceContextNumber != 0) {
        final Bytes sourceMemory =
            unsignedBytesToBytes(callStackReader.valueFromMemory(sourceContextNumber));
        currentMmuData.sourceRamBytes(sourceMemory);
      }

      final int targetContextNumber = mmuToMmioConstantValues.targetContextNumber();
      if (targetContextNumber != 0) {
        final Bytes targetMemory =
            unsignedBytesToBytes(callStackReader.valueFromMemory(targetContextNumber));
        currentMmuData.targetRamBytes(targetMemory);
      }

      for (int currentMmioInstNumber = 0;
          currentMmioInstNumber < currentMmuData.numberMmioInstructions();
          currentMmioInstNumber++) {
        stamp++;

        final MmioInstructions mmioInstructions =
            new MmioInstructions(currentMmuData, currentMmioInstNumber);
        final MmioData mmioData =
            mmioInstructions.compute(
                currentMmuData
                    .mmuToMmioInstructions()
                    .get(currentMmioInstNumber)
                    .mmioInstruction());

        trace(trace, mmioData, stamp);
      }
    }
  }

  void trace(Trace trace, MmioData mmioData, final int stamp) {

    final boolean isFast = isFastOperation(mmioData.instruction());

    for (short ct = 0; ct < numberOfRowOfMmioInstruction(mmioData.instruction()); ct++) {
      ExoSumDecoder exoSumDecoder = mmioData.exoSumDecoder();
      trace
          .cnA(mmioData.cnA())
          .cnB(mmioData.cnB())
          .cnC(mmioData.cnC())
          .indexA(mmioData.indexA())
          .indexB(mmioData.indexB())
          .indexC(mmioData.indexC())
          .valA(mmioData.valA())
          .valB(mmioData.valB())
          .valC(mmioData.valC())
          .valANew(mmioData.valANew())
          .valBNew(mmioData.valBNew())
          .valCNew(mmioData.valCNew())
          .byteA(UnsignedByte.of(mmioData.valA().get(ct)))
          .byteB(UnsignedByte.of(mmioData.valB().get(ct)))
          .byteC(UnsignedByte.of(mmioData.valC().get(ct)))
          .accA(mmioData.valA().slice(0, ct + 1))
          .accB(mmioData.valB().slice(0, ct + 1))
          .accC(mmioData.valC().slice(0, ct + 1))
          .mmioStamp(stamp)
          .mmioInstruction(mmioData.instruction())
          .contextSource(mmioData.sourceContext())
          .contextTarget(mmioData.targetContext())
          .sourceLimbOffset(mmioData.sourceLimbOffset())
          .targetLimbOffset(mmioData.targetLimbOffset())
          .sourceByteOffset(mmioData.sourceByteOffset())
          .targetByteOffset(mmioData.targetByteOffset())
          .size(mmioData.size())
          .limb(mmioData.limb())
          .totalSize(Bytes.ofUnsignedShort(mmioData.totalSize()))
          .exoSum(mmioData.exoSum())
          .exoId(mmioData.exoId())
          .exoIsTxcd(exoSumDecoder.exoIsTxcd())
          .exoIsRom(exoSumDecoder.exoIsRom())
          .exoIsLog(exoSumDecoder.exoIsLog())
          .exoIsEcdata(exoSumDecoder.exoIsEcData())
          .exoIsBlakemodexp(exoSumDecoder.exoIsBlake2fModexp())
          .exoIsRipsha(exoSumDecoder.exoIsRipSha())
          .exoIsKec(exoSumDecoder.exoIsKeccak())
          .kecId(mmioData.kecId())
          .phase(mmioData.phase())
          .successBit(mmioData.successBit())
          .fast(isFast)
          .slow(!isFast)
          .isLimbToRamOneTarget(mmioData.instruction() == Trace.MMIO_INST_LIMB_TO_RAM_ONE_TARGET)
          .isLimbToRamTransplant(mmioData.instruction() == Trace.MMIO_INST_LIMB_TO_RAM_TRANSPLANT)
          .isLimbToRamTwoTarget(mmioData.instruction() == Trace.MMIO_INST_LIMB_TO_RAM_TWO_TARGET)
          .isLimbVanishes(mmioData.instruction() == Trace.MMIO_INST_LIMB_VANISHES)
          .isRamExcision(mmioData.instruction() == Trace.MMIO_INST_RAM_EXCISION)
          .isRamToLimbOneSource(mmioData.instruction() == Trace.MMIO_INST_RAM_TO_LIMB_ONE_SOURCE)
          .isRamToLimbTransplant(mmioData.instruction() == Trace.MMIO_INST_RAM_TO_LIMB_TRANSPLANT)
          .isRamToLimbTwoSource(mmioData.instruction() == Trace.MMIO_INST_RAM_TO_LIMB_TWO_SOURCE)
          .isRamToRamPartial(mmioData.instruction() == Trace.MMIO_INST_RAM_TO_RAM_PARTIAL)
          .isRamToRamTransplant(mmioData.instruction() == Trace.MMIO_INST_RAM_TO_RAM_TRANSPLANT)
          .isRamToRamTwoSource(mmioData.instruction() == Trace.MMIO_INST_RAM_TO_RAM_TWO_SOURCE)
          .isRamToRamTwoTarget(mmioData.instruction() == Trace.MMIO_INST_RAM_TO_RAM_TWO_TARGET)
          .isRamVanishes(mmioData.instruction() == Trace.MMIO_INST_RAM_VANISHES)
          .indexX(mmioData.indexX())
          .byteLimb(UnsignedByte.of(mmioData.limb().get(ct)))
          .accLimb(mmioData.limb().slice(0, ct + 1))
          .bin1(mmioData.bin1()[ct])
          .bin2(mmioData.bin2()[ct])
          .bin3(mmioData.bin3()[ct])
          .bin4(mmioData.bin4()[ct])
          .bin5(mmioData.bin5()[ct])
          .acc1(mmioData.acc1()[ct])
          .acc2(mmioData.acc2()[ct])
          .acc3(mmioData.acc3()[ct])
          .acc4(mmioData.acc4()[ct])
          .pow2561(mmioData.pow2561()[ct])
          .pow2562(mmioData.pow2562()[ct])
          .counter(ct)
          .validateRow();
    }
  }
}
