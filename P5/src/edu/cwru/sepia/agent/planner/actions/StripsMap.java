package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.GameState;

import java.util.EnumSet;
import java.util.Set;

public class StripsMap implements StripsAction
{
    private Set<StripsAction> actions;

    public StripsMap(Set<StripsAction> actions)
    {
        this.actions = actions;
    }

    @Override
    public boolean preconditionsMet(GameState state)
    {
        return getActions().stream().allMatch(a -> preconditionsMet(state));
    }

    @Override
    public GameState apply(GameState state)
    {
        GameState applied = new GameState(state);
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
    public double computeCost()
    {
        return getActions().stream()
                           .mapToDouble(StripsAction::computeCost)
                           .sum();
    }

    public Set<StripsAction> getActions()
    {
        return actions;
    }
}
