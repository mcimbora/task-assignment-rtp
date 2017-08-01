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

package taskassignment.swingui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Random;
import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.Timer;

import taskassignment.domain.Customer;
import taskassignment.domain.Priority;
import taskassignment.domain.Task;
import taskassignment.domain.TaskAssigningSolution;
import taskassignment.domain.TaskType;
import taskassignment.domain.solver.ConsumeTaskProblemFactChange;
import taskassignment.domain.solver.ProduceTaskProblemFactChange;

import static taskassignment.TaskAssigningGenerator.BASE_DURATION_AVERAGE;

public class TaskAssigningPanel extends SolutionPanel<TaskAssigningSolution> {

    public static final String LOGO_PATH = "/org/optaplanner/examples/taskassigning/swingui/taskAssigningLogo.png";

    private final TaskOverviewPanel taskOverviewPanel;

    private JSpinner consumeRateField;
    private AbstractAction consumeAction;
    private Timer consumeTimer;
    private JSpinner produceRateField;
    private AbstractAction produceAction;
    private Timer produceTimer;

    private int consumedDurationInSeconds = 0;
    private int previousConsumedDuration = 0; // In minutes
    private int producedDurationInSeconds = 0;
    private int previousProducedDuration = 0; // In minutes
    private volatile Random producingRandom;

    public TaskAssigningPanel() {
        setLayout(new BorderLayout());
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);
        taskOverviewPanel = new TaskOverviewPanel(this);
        add(new JScrollPane(taskOverviewPanel), BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new GridLayout(1, 0));
        JPanel consumePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        consumePanel.add(new JLabel("Consume rate:"));
        consumeRateField = new JSpinner(new SpinnerNumberModel(1000, 10, 3600, 10));
        consumePanel.add(consumeRateField);
        consumeTimer = new Timer(1000, e -> {
            consumedDurationInSeconds += (Integer) consumeRateField.getValue();
            consumeUpTo(consumedDurationInSeconds / 60);
            repaint();
        });
        consumeAction = new AbstractAction("Consume") {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!consumeTimer.isRunning()) {
                    consumeRateField.setEnabled(false);
                    consumeTimer.start();
                } else {
                    consumeRateField.setEnabled(true);
                    consumeTimer.stop();
                }
            }
        };
        consumePanel.add(new JToggleButton(consumeAction));
        headerPanel.add(consumePanel);
        JPanel producePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        producePanel.add(new JLabel("Produce rate:"));
        produceRateField = new JSpinner(new SpinnerNumberModel(1000, 10, 3600, 10));
        producePanel.add(produceRateField);
        produceTimer = new Timer(1000, e -> {
            producedDurationInSeconds += (Integer) produceRateField.getValue();
            produceUpTo(producedDurationInSeconds / 60);
            repaint();
        });
        produceAction = new AbstractAction("Produce") {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!produceTimer.isRunning()) {
                    produceRateField.setEnabled(false);
                    produceTimer.start();
                } else {
                    produceRateField.setEnabled(true);
                    produceTimer.stop();
                }
            }
        };
        producePanel.add(new JToggleButton(produceAction));
        headerPanel.add(producePanel);
        return headerPanel;
    }

    /**
     * @param consumedDuration in minutes, just like {@link Task#getStartTime()}
     */
    public void consumeUpTo(final int consumedDuration) {
        taskOverviewPanel.setConsumedDuration(consumedDuration);
        if (consumedDuration <= previousConsumedDuration) {
            // Occurs due to rounding down of consumedDurationInSeconds
            return;
        }
        logger.debug("Scheduling consumption of all tasks up to {} minutes.", consumedDuration);
        previousConsumedDuration = consumedDuration;

        doProblemFactChange(new ConsumeTaskProblemFactChange(consumedDuration));
    }

    /**
     * @param producedDuration in minutes, just like {@link Task#getStartTime()}
     */
    public void produceUpTo(final int producedDuration) {
        if (producedDuration <= previousProducedDuration) {
            // Occurs due to rounding down of producedDurationInSeconds
            return;
        }
        final int baseDurationBudgetPerEmployee = (producedDuration - previousProducedDuration);
        if (baseDurationBudgetPerEmployee < BASE_DURATION_AVERAGE) {
            return;
        }
        final int newTaskCount = Math.max(1,
                getSolution().getEmployeeList().size() * baseDurationBudgetPerEmployee / BASE_DURATION_AVERAGE);
        logger.debug("Scheduling production of {} new tasks.", newTaskCount);
        previousProducedDuration = producedDuration;
        final int readyTime = previousConsumedDuration;

        doProblemFactChange(new ProduceTaskProblemFactChange(newTaskCount,
                                                             readyTime,
                                                             producingRandom));
    }

    @Override
    public boolean isWrapInScrollPane() {
        return false;
    }

    @Override
    public void resetPanel(TaskAssigningSolution taskAssigningSolution) {
        consumedDurationInSeconds = 0;
        previousConsumedDuration = 0;
        producedDurationInSeconds = 0;
        previousProducedDuration = 0;
        producingRandom = new Random(0); // Random is thread safe
        taskOverviewPanel.resetPanel(taskAssigningSolution);
        taskOverviewPanel.setConsumedDuration(consumedDurationInSeconds / 60);
    }

    @Override
    public void updatePanel(TaskAssigningSolution taskAssigningSolution) {
        taskOverviewPanel.resetPanel(taskAssigningSolution);
    }

}
