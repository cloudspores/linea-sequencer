/*
 * Copyright ConsenSys Inc.
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

package net.consensys.linea.zktracer.module.mmu.instructions;

import static net.consensys.linea.zktracer.types.Conversions.*;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import net.consensys.linea.zktracer.module.euc.Euc;
import net.consensys.linea.zktracer.module.euc.EucOperation;
import net.consensys.linea.zktracer.module.mmu.MmuData;
import net.consensys.linea.zktracer.module.mmu.Trace;
import net.consensys.linea.zktracer.module.mmu.values.HubToMmuValues;
import net.consensys.linea.zktracer.module.mmu.values.MmuEucCallRecord;
import net.consensys.linea.zktracer.module.mmu.values.MmuOutAndBinValues;
import net.consensys.linea.zktracer.module.mmu.values.MmuToMmioConstantValues;
import net.consensys.linea.zktracer.module.mmu.values.MmuToMmioInstruction;
import net.consensys.linea.zktracer.module.mmu.values.MmuWcpCallRecord;
import net.consensys.linea.zktracer.module.wcp.Wcp;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.tuweni.bytes.Bytes;

public class RightPaddedWordExtraction implements MmuInstruction {
  private final Euc euc;
  private final Wcp wcp;
  private List<MmuEucCallRecord> eucCallRecords;
  private List<MmuWcpCallRecord> wcpCallRecords;

  private boolean firstLimbPadded;
  private int firstLimbByteSize;
  private boolean secondLimbPadded;
  private int secondLimbByteSize;
  private long extractionSize;
  private boolean firstLimbIsFull;
  private boolean firstLimbSingleSource;
  private boolean secondLimbSingleSource;
  private Bytes sourceLimbOffset;
  private Bytes sourceByteOffset;
  private boolean secondLimbVoid;
  private int firstMicroInst;
  private int secondMicroInst;

  public RightPaddedWordExtraction(Euc euc, Wcp wcp) {
    this.euc = euc;
    this.wcp = wcp;
    this.eucCallRecords = new ArrayList<>(Trace.NB_PP_ROWS_RIGHT_PADDED_WORD_EXTRACTION);
    this.wcpCallRecords = new ArrayList<>(Trace.NB_PP_ROWS_RIGHT_PADDED_WORD_EXTRACTION);
  }

  @Override
  public MmuData preProcess(MmuData mmuData) {
    final HubToMmuValues hubToMmuValues = mmuData.hubToMmuValues();
    row1(hubToMmuValues);
    row2(hubToMmuValues);
    row3(hubToMmuValues);
    row4(hubToMmuValues);
    row5(hubToMmuValues);

    mmuData.eucCallRecords(eucCallRecords);
    mmuData.wcpCallRecords(wcpCallRecords);

    // setting Out and Bin values
    mmuData.outAndBinValues(MmuOutAndBinValues.DEFAULT); // all 0

    mmuData.totalLeftZeroesInitials(0);
    mmuData.totalNonTrivialInitials(Trace.NB_MICRO_ROWS_TOT_RIGHT_PADDED_WORD_EXTRACTION);
    mmuData.totalRightZeroesInitials(0);

    return mmuData;
  }

  private void row1(final HubToMmuValues hubToMmuValues) {
    // row n°1
    final Bytes wcpArg1 =
        bigIntegerToBytes(hubToMmuValues.sourceOffsetLo().add(BigInteger.valueOf(32)));
    final long refSize = hubToMmuValues.referenceSize();
    final Bytes wcpArg2 = longToBytes(refSize);
    final boolean wcpResult = wcp.callLT(wcpArg1, wcpArg2);
    secondLimbPadded = wcpResult;
    extractionSize = secondLimbPadded ? refSize - hubToMmuValues.sourceOffsetLo().longValue() : 32;

    wcpCallRecords.add(
        MmuWcpCallRecord.builder().arg1Lo(wcpArg1).arg2Lo(wcpArg2).result(wcpResult).build());

    eucCallRecords.add(MmuEucCallRecord.EMPTY_CALL);
  }

  private void row2(final HubToMmuValues hubToMmuValues) {
    // row n°2
    final Bytes wcpArg1 = longToBytes(extractionSize);
    final Bytes wcpArg2 = Bytes.of(16);
    final boolean wcpResult = wcp.callLT(wcpArg1, wcpArg2);

    wcpCallRecords.add(
        MmuWcpCallRecord.builder().arg1Lo(wcpArg1).arg2Lo(wcpArg2).result(wcpResult).build());

    firstLimbPadded = wcpResult;
    if (!secondLimbPadded) {
      secondLimbByteSize = 16;
    } else {
      secondLimbByteSize = !firstLimbPadded ? (int) (extractionSize - 16) : 0;
    }

    firstLimbByteSize = !firstLimbPadded ? 16 : (int) extractionSize;

    final Bytes dividend = Bytes.of(16);
    final Bytes divisor = Bytes.of(firstLimbByteSize);
    EucOperation eucOp = euc.callEUC(dividend, divisor);

    firstLimbIsFull = BooleanUtils.toBoolean(eucOp.quotient().toInt());

    eucCallRecords.add(
        MmuEucCallRecord.builder()
            .dividend(dividend.toLong())
            .divisor(divisor.toLong())
            .quotient(eucOp.quotient().toLong())
            .remainder(eucOp.remainder().toLong())
            .build());
  }

  private void row3(final HubToMmuValues hubToMmuValues) {
    // row n°3
    final Bytes dividend = Bytes.of(16);
    final Bytes divisor =
        longToBytes(hubToMmuValues.sourceOffsetLo().longValue() + hubToMmuValues.referenceOffset());
    EucOperation eucOp = euc.callEUC(dividend, divisor);

    eucCallRecords.add(
        MmuEucCallRecord.builder()
            .dividend(dividend.toLong())
            .divisor(divisor.toLong())
            .quotient(eucOp.quotient().toLong())
            .remainder(eucOp.remainder().toLong())
            .build());

    sourceLimbOffset = eucOp.quotient();
    sourceByteOffset = eucOp.remainder();

    final Bytes wcpArg1 = Bytes.ofUnsignedShort(sourceByteOffset.toInt() + firstLimbByteSize);
    final Bytes wcpArg2 = Bytes.of(16 + 1);
    boolean wcpResult = wcp.callLT(wcpArg1, wcpArg2);

    wcpCallRecords.add(
        MmuWcpCallRecord.builder().arg1Lo(wcpArg1).arg2Lo(wcpArg2).result(wcpResult).build());

    firstLimbSingleSource = wcpResult;
  }

  private void row4(final HubToMmuValues hubToMmuValues) {
    // row n°4
    eucCallRecords.add(MmuEucCallRecord.EMPTY_CALL);

    final Bytes wcpArg1 = Bytes.ofUnsignedShort(sourceByteOffset.toInt() + secondLimbByteSize);
    final Bytes wcpArg2 = Bytes.of(16 + 1);
    boolean wcpResult = wcp.callLT(wcpArg1, wcpArg2);

    wcpCallRecords.add(
        MmuWcpCallRecord.builder().arg1Lo(wcpArg1).arg2Lo(wcpArg2).result(wcpResult).build());

    secondLimbSingleSource = wcpResult;
  }

  private void row5(final HubToMmuValues hubToMmuValues) {
    // row n°5
    eucCallRecords.add(MmuEucCallRecord.EMPTY_CALL);

    Bytes isZeroArg = Bytes.ofUnsignedShort(secondLimbByteSize);
    boolean wcpResult = wcp.callISZERO(isZeroArg);

    wcpCallRecords.add(MmuWcpCallRecord.builder().arg1Lo(isZeroArg).result(wcpResult).build());

    secondLimbVoid = wcpResult;
  }

  @Override
  public MmuData setMicroInstructions(MmuData mmuData) {
    HubToMmuValues hubToMmuValues = mmuData.hubToMmuValues();
    hubToMmuValues.exoSum(0);

    mmuData.mmuToMmioConstantValues(
        MmuToMmioConstantValues.builder().sourceContextNumber(hubToMmuValues.sourceId()).build());

    firstMicroInstruction(mmuData);
    secondMicroInstruction(mmuData);

    return mmuData;
  }

  private void firstMicroInstruction(MmuData mmuData) {
    if (firstLimbSingleSource) {
      firstMicroInst =
          firstLimbIsFull
              ? Trace.MMIO_INST_RAM_TO_LIMB_TRANSPLANT
              : Trace.MMIO_INST_RAM_TO_LIMB_ONE_SOURCE;
    } else {
      firstMicroInst = Trace.MMIO_INST_RAM_TO_LIMB_TWO_SOURCE;
    }

    mmuData.mmuToMmioInstruction(
        MmuToMmioInstruction.builder()
            .mmioInstruction(firstMicroInst)
            .size(firstLimbByteSize)
            .sourceLimbOffset(sourceLimbOffset.toInt())
            .sourceByteOffset(sourceByteOffset.toInt())
            .targetByteOffset(0)
            .limb(mmuData.hubToMmuValues().limb1())
            .build());
  }

  private void secondMicroInstruction(MmuData mmuData) {
    if (secondLimbVoid) {
      mmuData.hubToMmuValues().limb2(Bytes.EMPTY);
      secondMicroInst = Trace.MMIO_INST_LIMB_VANISHES;
    } else if (secondLimbSingleSource) {
      secondMicroInst =
          !secondLimbPadded
              ? Trace.MMIO_INST_RAM_TO_LIMB_TRANSPLANT
              : Trace.MMIO_INST_RAM_TO_LIMB_ONE_SOURCE;
    } else {
      secondMicroInst = Trace.MMIO_INST_RAM_TO_LIMB_TWO_SOURCE;
    }

    mmuData.mmuToMmioInstruction(
        MmuToMmioInstruction.builder()
            .mmioInstruction(secondMicroInst)
            .size(secondLimbByteSize)
            .sourceLimbOffset(sourceLimbOffset.toInt() + 1)
            .sourceByteOffset(sourceByteOffset.toInt())
            .targetByteOffset(0)
            .limb(mmuData.hubToMmuValues().limb2())
            .build());
  }
}
