package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.GameState.Resource;
import edu.cwru.sepia.agent.planner.GameState.Unit;

import java.util.EnumSet;

import static edu.cwru.sepia.agent.planner.actions.StripsEnum.GATHER;
import static edu.cwru.sepia.agent.planner.actions.StripsEnum.IDLE;

public class Gather implements StripsAction
{
    private final Unit gatherer;
    private final Resource gatherFrom;

    public Gather(Unit gatherer, Resource gatherFrom)
    {
        this.gatherer = gatherer;
        this.gatherFrom = gatherFrom;
    }

    public long computeCostFactor()
    {
        return (long) getGatherFrom().getDistanceToTownHall();
    }

    @Override
    public boolean preconditionsMet(GameState state)
    {
        return state.getUnitTracker().containsAnyValue(GATHER, IDLE);
    }

    @Override
    public GameState apply(GameState state)
    {
        GameState applied = state.copy();
        applied.gather(getGatherer(), getGatherFrom(), 100);
        return applied;
    }

    @Override
    public EnumSet<StripsEnum> effects()
    {
        return StripsEnum.getValidNext(GATHER);
    }

    public Unit getGatherer()
    {
        return gatherer;
    }

    public Resource getGatherFrom()
    {
        return gatherFrom;
    }
}
