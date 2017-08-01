/*
 * Copyright 2010 Red Hat, Inc. and/or its affiliates.
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
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import taskassignment.business.SolutionBusiness;

public class ConstraintMatchesDialog extends JDialog {

    protected final transient Logger logger = LoggerFactory.getLogger(getClass());

    protected final SolutionBusiness solutionBusiness;

    public ConstraintMatchesDialog(SolverAndPersistenceFrame solverAndPersistenceFrame,
            SolutionBusiness solutionBusiness) {
        super(solverAndPersistenceFrame, "Constraint matches", true);
        this.solutionBusiness = solutionBusiness;
    }

    public void resetContentPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout());
        Action okAction = new AbstractAction("OK") {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        };
        buttonPanel.add(new JButton(okAction));
        JPanel unsupportedPanel = new JPanel(new BorderLayout());
        JLabel unsupportedLabel = new JLabel("Constraint matches are not supported with this ScoreDirector.");
        unsupportedPanel.add(unsupportedLabel,
                             BorderLayout.CENTER);
        unsupportedPanel.add(buttonPanel,
                             BorderLayout.SOUTH);
        setContentPane(unsupportedPanel);
        pack();
        setLocationRelativeTo(getParent());
    }
}
