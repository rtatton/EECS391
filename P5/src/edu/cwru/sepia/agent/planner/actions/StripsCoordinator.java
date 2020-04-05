package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.GameState;

import java.util.EnumSet;

// TODO This will be the way to handle multiple StripsActions in one go
public class StripsCoordinator implements StripsAction
{
    @Override
    public boolean preconditionsMet(GameState state)
    {
        return false;
    }

    @Override
    public GameState apply(GameState state)
    {
        return null;
    }

    @Override
    public EnumSet<StripsEnum> effects()
    {
        return null;
    }

    @Override
    public double computeCost()
    {
        return 0;
    }

    @Override
    public StripsEnum getStripsActionType()
    {
        return null;
    }
}
