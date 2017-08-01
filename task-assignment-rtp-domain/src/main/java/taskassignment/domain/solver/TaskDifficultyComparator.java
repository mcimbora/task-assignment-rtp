/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
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

package taskassignment.domain.solver;

import java.io.Serializable;
import java.util.Comparator;

import taskassignment.domain.Task;

//import org.apache.commons.lang3.builder.CompareToBuilder;

public class TaskDifficultyComparator implements Comparator<Task>,
                                                 Serializable {

    @Override
    public int compare(Task a,
                       Task b) {
//        return new CompareToBuilder()
//                .append(a.getPriority(), b.getPriority())
//                .append(a.getTaskType().getRequiredSkillList().size(), b.getTaskType().getRequiredSkillList().size())
//                .append(a.getTaskType().getBaseDuration(), b.getTaskType().getBaseDuration())
//                .append(a.getId(), b.getId())
//                .toComparison();
        return 0;
    }
}
