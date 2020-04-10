package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.GameState;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public class StripsActionSet implements StripsAction
{
    private Set<StripsAction> actions;

    public StripsActionSet(Set<StripsAction> actions)
    {
        this.actions = actions;
    }

    public StripsActionSet()
    {
        this(new HashSet<>());
    }

    @Override
    public boolean preconditionsMet(GameState state)
    {
        return getActions().stream().allMatch(a -> preconditionsMet(state));
    }

    public StripsActionSet getMetSubset(GameState state)
    {
        Set<StripsAction> preconditionsMet = new HashSet<>(getActions());
        preconditionsMet.removeIf(a -> !preconditionsMet(state));
        return new StripsActionSet(preconditionsMet);
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
    public EnumSet<StripsEnum> effects()
    {
        EnumSet<StripsEnum> effects = EnumSet.noneOf(StripsEnum.class);
        getActions().stream()
                    .map(StripsAction::effects)
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

    public Set<StripsAction> getActions()
    {
        return actions;
    }
}
