package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.GameState.Resource;
import edu.cwru.sepia.agent.planner.GameState.Unit;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
        applied.getCreationActions().getActions().add(this);
        return applied;
    }

    @Override
    public Set<StripsAction> effects(GameState state)
    {
        StripsAction deposit =
                new Deposit(getGatherer(), 100, computeCostFactor());
        Set<StripsAction> effects = new HashSet<>();
        effects.add(deposit);
        return effects;
    }

    @Override
    public long computeCostFactor()
    {
        return (long) getGatherFrom().getDistanceToTownHall();
    }

    public static Set<StripsAction> allPossibleGathers(Unit unit,
                                                       GameState state)
    {
        return state.getResourceTracker()
                    .getResources()
                    .stream()
                    .map(r -> new Gather(unit, r))
                    .collect(Collectors.toSet());
    }

    @Override
    public Action getSepiaAction(int... unitId)
    {
        return Action.createCompoundGather(unitId[0], getGatherFrom().getId());
    }

    @Override
    public ActionType getSepiaActionType()
    {
        return ActionType.COMPOUNDGATHER;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(getGatherFrom(), getGatherer());
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
