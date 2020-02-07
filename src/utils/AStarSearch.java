package utils;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.environment.model.state.State;

import java.util.*;

public abstract class AStarSearch
{
    // TODO Not the right data structure for the job
    private SortedMap<Double, Action> actionSequence;
    private SortedMap<Action, State> stateSequence;
    private PriorityQueue<State> frontier; // Each should have a
    private State start;
    private Set<State> goalStates;

    abstract double edgeCost(State from, Action action, State to);

    public double pathCost(State to)
    {
        // Get previous path cost
        // Get previous state
        // Calculate edge cost: edgeCost(prevState, action, to)
        // return this cost
        return 0;
    }

    abstract double heuristic(State state);

    public double evaluationCost(State to)
    {
        return pathCost(to) + heuristic(to);
    }

    public void evaluate()
    {
        frontier
                .parallelStream()
                .min((s1, s2) -> (int) (evaluationCost(s1) - evaluationCost(s2)));
              //  .ifPresent(s -> actionSequence.add(s));
    }

    abstract void expandOptimal();

    public boolean isGoalState(State state)
    {
        return goalStates.contains(state);
    }


    public SortedMap<Double, Action> getActionSequence()
    {
        return actionSequence;
    }

    public PriorityQueue<State> getFrontier()
    {
        return frontier;
    }

    public State getStart()
    {
        return start;
    }

    public Set<State> getGoalStates()
    {
        return goalStates;
    }
}
