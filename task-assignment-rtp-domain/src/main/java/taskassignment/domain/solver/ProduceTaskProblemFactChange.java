package taskassignment.domain.solver;

import java.util.List;
import java.util.Random;

import org.optaplanner.core.impl.score.director.ScoreDirector;
import org.optaplanner.core.impl.solver.ProblemFactChange;
import taskassignment.domain.Customer;
import taskassignment.domain.Priority;
import taskassignment.domain.Task;
import taskassignment.domain.TaskAssigningSolution;
import taskassignment.domain.TaskType;

public class ProduceTaskProblemFactChange implements ProblemFactChange<TaskAssigningSolution> {

    private int newTaskCount;
    private int readyTime;
    private Random producingRandom;

    // XStream marshaling
    public ProduceTaskProblemFactChange() {
    }

    public ProduceTaskProblemFactChange(int newTaskCount,
                                        int readyTime,
                                        Random producingRandom) {
        this.newTaskCount = newTaskCount;
        this.readyTime = readyTime;
        this.producingRandom = producingRandom;
    }

    @Override
    public void doChange(ScoreDirector<TaskAssigningSolution> scoreDirector) {
        TaskAssigningSolution solution = scoreDirector.getWorkingSolution();
        List<TaskType> taskTypeList = solution.getTaskTypeList();
        List<Customer> customerList = solution.getCustomerList();
        Priority[] priorities = Priority.values();
        List<Task> taskList = solution.getTaskList();
        for (int i = 0; i < newTaskCount; i++) {
            Task task = new Task();
            TaskType taskType = taskTypeList.get(producingRandom.nextInt(taskTypeList.size()));
            long nextTaskId = 0L;
            int nextIndexInTaskType = 0;
            for (Task other : taskList) {
                if (nextTaskId <= other.getId()) {
                    nextTaskId = other.getId() + 1L;
                }
                if (taskType == other.getTaskType()) {
                    if (nextIndexInTaskType <= other.getIndexInTaskType()) {
                        nextIndexInTaskType = other.getIndexInTaskType() + 1;
                    }
                }
            }
            task.setId(nextTaskId);
            task.setTaskType(taskType);
            task.setIndexInTaskType(nextIndexInTaskType);
            task.setCustomer(customerList.get(producingRandom.nextInt(customerList.size())));
            // Prevent the new task from being assigned retroactively
            task.setReadyTime(readyTime);
            task.setPriority(priorities[producingRandom.nextInt(priorities.length)]);

            scoreDirector.beforeEntityAdded(task);
            taskList.add(task);
            scoreDirector.afterEntityAdded(task);
        }
        scoreDirector.triggerVariableListeners();
    }
}
