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

package net.consensys.linea.zktracer.module.mmu;

import lombok.Builder;

@Builder
public record RowTypeRecord(
    int total,
    int totalLeftZeroes,
    int totalNonTrivial,
    int totalRightZeroes,
    boolean leftZeroRow,
    boolean onlyNonTrivialRow,
    boolean firstNonTrivialRow,
    boolean middleNonTrivialRow,
    boolean lastNonTrivialRow,
    boolean onlyRightZeroRow,
    boolean firstRightZeroRow,
    boolean middleRightZeroRow,
    boolean lastRightZeroRow) {}