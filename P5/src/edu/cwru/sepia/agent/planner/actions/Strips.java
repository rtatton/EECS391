package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.action.ActionType;
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
        BUILD, DEPOSIT, EMPTY, GATHER, IDLE, PRODUCE;

        public static ActionType getActionType(ActionEnum action)
        {
            switch (action)
            {
                case DEPOSIT:
                    return ActionType.COMPOUNDDEPOSIT;
                case PRODUCE:
                    return ActionType.COMPOUNDPRODUCE;
                case BUILD:
                    return ActionType.COMPOUNDBUILD;
                default:
                    return ActionType.COMPOUNDGATHER;
            }
        }
    }
}
