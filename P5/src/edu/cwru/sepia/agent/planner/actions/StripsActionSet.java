package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.agent.planner.GameState;

import java.util.HashSet;
import java.util.Set;

public class StripsActionSet implements StripsAction
{
    private Set<StripsAction> actions;

    public StripsActionSet(Set<StripsAction> actions)
    {
        this.actions = actions;
    }

    public StripsActionSet(StripsActionSet stripsActionSet)
    {
        this(new HashSet<>(stripsActionSet.getActions()));
    }

    public StripsActionSet()
    {
        this(new HashSet<>());
    }

    public StripsActionSet copy()
    {
        return new StripsActionSet(this);
    }

    @Override
    public boolean preconditionsMet(GameState state)
    {
        return getActions().stream().allMatch(a -> preconditionsMet(state));
    }

    @Override
    public GameState apply(GameState state)
    {
        GameState applied = state.copy();
        for (StripsAction action : getActions())
            applied = action.apply(applied);
        return applied;
    }

    @Override
    public Set<StripsAction> effects(GameState state)
    {
        Set<StripsAction> effects = new HashSet<>();
        getActions().stream()
                    .map(a -> a.effects(state))
                    .forEach(effects::addAll);
        return effects;
    }

    @Override
    public long computeCostFactor()
    {
        return getActions().stream()
                           .map(StripsAction::computeCostFactor)
                           .reduce((long) 0, Math::multiplyExact);
    }

    @Override
    public Action getSepiaAction(int... actionComponents)
    {
        return null;
    }

    @Override
    public ActionType getSepiaActionType()
    {
        return null;
    }

    @Override
    public int hashCode()
    {
        return getActions().hashCode();
    }

    public Set<StripsAction> getActions()
    {
        return actions;
    }
}
