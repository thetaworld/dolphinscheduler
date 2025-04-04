/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dolphinscheduler.api.validator.workflow;

import org.apache.dolphinscheduler.api.validator.IValidator;
import org.apache.dolphinscheduler.common.enums.CommandType;
import org.apache.dolphinscheduler.common.enums.ReleaseState;

import org.apache.commons.collections4.CollectionUtils;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BackfillWorkflowDTOValidator implements IValidator<BackfillWorkflowDTO> {

    @Override
    public void validate(final BackfillWorkflowDTO backfillWorkflowDTO) {
        final BackfillWorkflowDTO.BackfillParamsDTO backfillParams = backfillWorkflowDTO.getBackfillParams();
        if (backfillParams == null) {
            throw new IllegalArgumentException("backfillParams is null");
        }
        if (CollectionUtils.isEmpty(backfillParams.getBackfillDateList())) {
            throw new IllegalArgumentException("backfillDateList is empty");
        }
        if (backfillParams.getExpectedParallelismNumber() < 0) {
            throw new IllegalArgumentException("expectedParallelismNumber should >= 0");
        }
        if (backfillWorkflowDTO.getExecType() != CommandType.COMPLEMENT_DATA) {
            throw new IllegalArgumentException("The execType should be START_PROCESS");
        }
        if (backfillWorkflowDTO.getWorkflowDefinition() == null) {
            throw new IllegalArgumentException("The workflowDefinition should not be null");
        }
        if (backfillWorkflowDTO.getWorkflowDefinition().getReleaseState() != ReleaseState.ONLINE) {
            throw new IllegalStateException("The workflowDefinition should be online");
        }
    }
}
