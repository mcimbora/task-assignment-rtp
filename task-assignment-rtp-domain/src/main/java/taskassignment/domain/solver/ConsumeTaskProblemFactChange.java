package taskassignment.domain.solver;

import org.optaplanner.core.impl.score.director.ScoreDirector;
import org.optaplanner.core.impl.solver.ProblemFactChange;
import taskassignment.domain.Task;
import taskassignment.domain.TaskAssigningSolution;

public class ConsumeTaskProblemFactChange implements ProblemFactChange<TaskAssigningSolution> {

    private int consumedDuration;

    // XStream marshaling
    public ConsumeTaskProblemFactChange() {
    }

    public ConsumeTaskProblemFactChange(int consumedDuration) {
        this.consumedDuration = consumedDuration;
    }

    @Override
    public void doChange(ScoreDirector<TaskAssigningSolution> scoreDirector) {
        TaskAssigningSolution solution = scoreDirector.getWorkingSolution();
        for (Task task : solution.getTaskList()) {
            if (!task.isLocked()) {
                if (task.getStartTime() != null && task.getStartTime() < consumedDuration) {
                    scoreDirector.beforeProblemPropertyChanged(task);
                    task.setLocked(true);
                    scoreDirector.afterProblemPropertyChanged(task);
                } else if (task.getReadyTime() < consumedDuration) {
                    // Prevent a non-locked task from being assigned retroactively
                    scoreDirector.beforeProblemPropertyChanged(task);
                    task.setReadyTime(consumedDuration);
                    scoreDirector.afterProblemPropertyChanged(task);
                }
            }
        }
        scoreDirector.triggerVariableListeners();
    }
}
