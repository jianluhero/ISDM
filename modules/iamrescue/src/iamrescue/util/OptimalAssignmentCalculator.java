package iamrescue.util;

import java.util.Arrays;

public class OptimalAssignmentCalculator {
    /**
     * Assigns a set of tasks to agents in order to minimise the overall cost.
     * @param costs
     *            Takes an array where costs[taskID][agentID] represents a cost
     *            of a particular agent performing a given task.
     * @return An assignment of tasks to agents, such that assignment[taskID] =
     *         agentID.
     */
    public static int[] calculateOptimalAssignment(double[][] costs) {
        if (costs.length == 0 || costs[0].length == 0) {
            return new int[0];
        }
        int tasks = costs.length;
        int agents = costs[0].length;
        if (agents < tasks) {
            throw new IllegalArgumentException(
                    "There must be at least as many agents as there are tasks. "
                    + " I have " + tasks + " tasks and " + agents
                    + " agents.");
        }
        Hungarian hungarian = new Hungarian(agents, tasks);
        for (int i = 0; i < tasks; i++) {
            if (costs[i].length != agents) {
                throw new IllegalArgumentException(
                        "Different number of agents for task " + i);
            }
            double[] preferences = new double[agents];
            for (int j = 0; j < agents; j++) {
                preferences[j] = -costs[i][j];
            }
            hungarian.addColumn(i, preferences);
        }
        hungarian.solve();
        // int[] solutions = hungarian.getSolutions();
        // int[] taskSolutions = new int[tasks];
        return hungarian.getSolutions();
    }

    public static double calculateCost(double[][] costs, int[] assignment) {
        double cost = 0;

        if (assignment.length != costs.length) {
            throw new IllegalArgumentException(
                    "Length of assignment does not match length of costs: "
                    + assignment.length + " != " + costs.length + " "
                    + Arrays.toString(assignment));
        }

        for (int i = 0; i < assignment.length; i++) {
            cost += costs[i][assignment[i]];
        }
        return cost;
    }
}
