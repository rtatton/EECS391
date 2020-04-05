package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.action.ActionType;

import java.util.EnumSet;

public enum StripsEnum
{
    BUILD, DEPOSIT, GATHER, IDLE, PRODUCE;

    public static EnumSet<StripsEnum> getValidNext(StripsEnum action)
    {
        switch (action)
        {
            case GATHER:
                return EnumSet.of(IDLE, DEPOSIT);
            case DEPOSIT:
                return EnumSet.of(IDLE, GATHER, BUILD);
            case IDLE:
                return EnumSet.of(IDLE, GATHER, PRODUCE, BUILD);
            case PRODUCE:
                return EnumSet.of(IDLE, PRODUCE);
            case BUILD:
                return EnumSet.of(IDLE, BUILD, GATHER);
            default:
                return EnumSet.of(IDLE);
        }
    }

    public static ActionType getActionType(StripsEnum action)
    {
        switch (action)
        {
            case DEPOSIT:
                return ActionType.COMPOUNDDEPOSIT;
            case PRODUCE:
                return ActionType.COMPOUNDPRODUCE;
            case BUILD:
                return ActionType.COMPOUNDBUILD;
            case GATHER:
                return ActionType.COMPOUNDGATHER;
            default:
                return null;
        }
    }
}
