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

import static net.consensys.linea.zktracer.module.mmio.MmioPatterns.antiPower;
import static net.consensys.linea.zktracer.module.mmio.MmioPatterns.isolateChunk;
import static net.consensys.linea.zktracer.module.mmio.MmioPatterns.isolatePrefix;
import static net.consensys.linea.zktracer.module.mmio.MmioPatterns.isolateSuffix;
import static net.consensys.linea.zktracer.module.mmio.MmioPatterns.plateau;
import static net.consensys.linea.zktracer.module.mmio.MmioPatterns.power;
import static net.consensys.linea.zktracer.module.mmio.Trace.LLARGE;

import java.util.Arrays;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.consensys.linea.zktracer.module.mmu.values.MmuToMmioConstantValues;
import net.consensys.linea.zktracer.module.mmu.values.MmuToMmioInstruction;
import net.consensys.linea.zktracer.types.Bytes16;
import org.apache.tuweni.bytes.Bytes;

@Getter
@Setter
@Accessors(fluent = true)
@AllArgsConstructor
public class MmioData {
  private int cnA;
  private int cnB;
  private int cnC;

  private int indexA;
  private int indexB;
  private int indexC;

  private Bytes16 valA;
  private Bytes16 valB;
  private Bytes16 valC;

  private Bytes16 valANew;
  private Bytes16 valBNew;
  private Bytes16 valCNew;

  // imorted from the mmu
  private int instruction;
  private int sourceContext;
  private int targetContext;
  private int sourceLimbOffset;
  private int targetLimbOffset;
  private short sourceByteOffset;
  private short targetByteOffset;
  private short size;
  private Bytes16 limb;
  private int totalSize;
  private int exoSum;
  private int exoId;
  private int kecId;
  private long phase;
  private boolean successBit;
  private ExoSumDecoder exoSumDecoder;

  private int indexX;

  private boolean[] bin1;
  private boolean[] bin2;
  private boolean[] bin3;
  private boolean[] bin4;
  private boolean[] bin5;

  private Bytes[] pow2561;
  private Bytes[] pow2562;

  private Bytes[] acc1;
  private Bytes[] acc2;
  private Bytes[] acc3;
  private Bytes[] acc4;

  // private boolean exoIsKeccak;
  // private boolean exoIsRipSha;
  // private boolean exoIsBlake2fModexp;
  // private boolean exoIsLog;
  // private boolean exoIsRom;
  // private boolean exoIsTxcd;

  public MmioData(
      MmuToMmioConstantValues mmuToMmioConstantValues,
      MmuToMmioInstruction mmuToMmioInstruction,
      ExoSumDecoder exoSumDecoder) {
    this(
        0,
        0,
        0,
        0,
        0,
        0,
        Bytes16.ZERO,
        Bytes16.ZERO,
        Bytes16.ZERO,
        Bytes16.ZERO,
        Bytes16.ZERO,
        Bytes16.ZERO,
        mmuToMmioInstruction.mmioInstruction(),
        mmuToMmioConstantValues.sourceContextNumber(),
        mmuToMmioConstantValues.targetContextNumber(),
        mmuToMmioInstruction.sourceLimbOffset(),
        mmuToMmioInstruction.targetLimbOffset(),
        mmuToMmioInstruction.sourceByteOffset(),
        mmuToMmioInstruction.targetByteOffset(),
        mmuToMmioInstruction.size(),
        mmuToMmioInstruction.limb(),
        mmuToMmioConstantValues.totalSize(),
        mmuToMmioConstantValues.exoSum(),
        mmuToMmioConstantValues.exoId(),
        mmuToMmioConstantValues.kecId(),
        mmuToMmioConstantValues.phase(),
        mmuToMmioConstantValues.successBit(),
        exoSumDecoder,
        0,
        new boolean[LLARGE],
        new boolean[LLARGE],
        new boolean[LLARGE],
        new boolean[LLARGE],
        new boolean[LLARGE],
        initBytes(new Bytes[LLARGE]),
        initBytes(new Bytes[LLARGE]),
        initBytes(new Bytes[LLARGE]),
        initBytes(new Bytes[LLARGE]),
        initBytes(new Bytes[LLARGE]),
        initBytes(new Bytes[LLARGE]));
  }

  private static Bytes[] initBytes(Bytes[] arr) {
    Arrays.fill(arr, Bytes.EMPTY);
    return arr;
  }

  public static boolean isFastOperation(final int mmioInstruction) {
    return List.of(
            Trace.MMIO_INST_LIMB_VANISHES,
            Trace.MMIO_INST_LIMB_TO_RAM_TRANSPLANT,
            Trace.MMIO_INST_RAM_TO_LIMB_TRANSPLANT,
            Trace.MMIO_INST_RAM_TO_RAM_TRANSPLANT,
            Trace.MMIO_INST_RAM_VANISHES)
        .contains(mmioInstruction);
  }

  public static int numberOfRowOfMmioInstruction(final int mmioInstruction) {
    return isFastOperation(mmioInstruction) ? 1 : LLARGE;
  }

  public void onePartialToOne(
      Bytes16 sourceBytes,
      Bytes16 targetBytes,
      short sourceOffsetTrigger,
      short targetOffsetTrigger,
      short size) {
    for (short ct = 0; ct < LLARGE; ct++) {
      bin1[ct] = plateau(targetOffsetTrigger, ct);
      bin2[ct] = plateau(targetOffsetTrigger + size, ct);
      bin3[ct] = plateau(sourceOffsetTrigger, ct);
      bin4[ct] = plateau(sourceOffsetTrigger + size, ct);
    }
    acc1 = isolateChunk(targetBytes, bin1, bin2);
    acc2 = isolateChunk(sourceBytes, bin3, bin4);
    pow2561 = power(bin2);
  }

  public void onePartialToTwo(
      Bytes16 sourceBytes,
      Bytes16 target1Bytes,
      Bytes16 target2Bytes,
      short sourceOffsetTrigger,
      short target1OffsetTrigger,
      short size) {
    for (short ct = 0; ct < LLARGE; ct++) {
      bin1[ct] = plateau(target1OffsetTrigger, ct);
      bin2[ct] = plateau(target1OffsetTrigger + size - LLARGE, ct);
      bin3[ct] = plateau(sourceOffsetTrigger, ct);
      bin4[ct] = plateau(sourceOffsetTrigger + LLARGE - target1OffsetTrigger, ct);
      bin5[ct] = plateau(sourceOffsetTrigger + size, ct);
    }
    acc1 = isolateSuffix(target1Bytes, bin1);
    acc1 = isolatePrefix(target2Bytes, bin2);
    acc3 = isolateChunk(sourceBytes, bin3, bin4);
    acc4 = isolateChunk(sourceBytes, bin4, bin5);
    pow2561 = power(bin2);
  }

  public void oneToOnePadded(
      final Bytes16 sourceBytes, final short sourceOffsetTrigger, final short size) {

    for (short ct = 0; ct < LLARGE; ct++) {

      bin1[ct] = plateau(sourceOffsetTrigger, ct);
      bin2[ct] = plateau(sourceOffsetTrigger + size, ct);
      bin3[ct] = plateau(size, ct);
    }
    acc1 = isolateChunk(sourceBytes, bin1, bin2);
    pow2561 = power(bin3);
  }

  public void excision(final Bytes16 target, final short targetOffsetTrigger, final short size) {

    for (short ct = 0; ct < LLARGE; ct++) {
      bin1[ct] = plateau(targetOffsetTrigger, ct);
      bin2[ct] = plateau(targetOffsetTrigger + size, ct);
    }

    acc1 = isolateChunk(target, bin1, bin2);
    pow2561 = power(bin2);
  }

  //  public void updateLimbsInMemory(final CallStack callStack) {
  //    StackContext pending = callStack.getByContextNumber(cnA).pending();
  //    MemorySegmentSnapshot memorySegmentSnapshot = pending.memorySegmentSnapshot();
  //
  //    memorySegmentSnapshot.updateLimb(indexA, valANew);
  //    memorySegmentSnapshot.updateLimb(indexB, valBNew);
  //    memorySegmentSnapshot.updateLimb(indexC, valCNew);
  //  }

  // private void mergeAndCopyVals(
  //     int sourceByteOffset, UnsignedByte[] valA, UnsignedByte[] valB, UnsignedByte[] valDest) {
  //   UnsignedByte[] valASubArray = ArrayUtils.subarray(valA, sourceByteOffset, valA.length);
  //   UnsignedByte[] valBSubArray = ArrayUtils.subarray(valB, 0, sourceByteOffset);
  //
  //   UnsignedByte[] valMerged =
  //       (UnsignedByte[]) Stream.of(valASubArray, valBSubArray).flatMap(Stream::of).toArray();
  //   System.arraycopy(valMerged, 0, valDest, 0, valMerged.length);
  // }

  public void twoToOnePadded(
      Bytes16 sourceBytes1, Bytes16 sourceBytes2, short sourceOffsetTrigger, short size) {

    for (short ct = 0; ct < LLARGE; ct++) {
      bin1[ct] = plateau(sourceOffsetTrigger, ct);
      bin2[ct] = plateau(sourceOffsetTrigger + size - LLARGE, ct);
      bin3[ct] = plateau(LLARGE - sourceOffsetTrigger, ct);
      bin4[ct] = plateau(size, ct);
    }
    acc1 = isolateSuffix(sourceBytes1, bin1);
    acc2 = isolatePrefix(sourceBytes2, bin2);
    pow2561 = power(bin3);
    pow2562 = power(bin4);
  }

  public void twoPartialToOne(
      final Bytes16 source1,
      final Bytes16 source2,
      final Bytes16 target,
      final short sourceOffsetTrigger,
      final short targetOffsetTrgger,
      final short size) {

    for (short ct = 0; ct < LLARGE; ct++) {
      bin1[ct] = plateau(sourceOffsetTrigger, ct);
      bin2[ct] = plateau(sourceOffsetTrigger + size - LLARGE, ct);
      bin3[ct] = plateau(targetOffsetTrgger, ct);
      bin4[ct] = plateau(targetOffsetTrgger + size, ct);
    }

    acc1 = isolateSuffix(source1, bin1);
    acc2 = isolatePrefix(source2, bin2);
    acc3 = isolateChunk(target, bin3, bin4);

    pow2561 = power(bin4);
    pow2562 = antiPower(bin2);
  }
}
