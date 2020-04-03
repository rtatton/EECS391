package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.GameState;

public class Strips implements StripsAction
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

    public enum ActionEnum
    {
        GATHER, DEPOSIT, PRODUCE, EMPTY, GATHER_GOLD, GATHER_WOOD,
        IDLE;
    }
}
