package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.GameState.Resource;
import edu.cwru.sepia.agent.planner.GameState.Unit;

import java.util.EnumSet;

import static edu.cwru.sepia.agent.planner.actions.StripsEnum.DEPOSIT;

public class Deposit implements StripsAction
{
    private final Unit depositor;
    private final Resource gatheredFrom;
    private final int amount;

    public Deposit(Unit depositor, Resource gatheredFrom, int amount)
    {
        this.depositor = depositor;
        this.gatheredFrom = gatheredFrom;
        this.amount = amount;
    }

    @Override
    public boolean preconditionsMet(GameState state)
    {
        return state.getUnitTracker().containsValue(DEPOSIT);
    }

    @Override
    public GameState apply(GameState state)
    {
        GameState applied = state.copy();
        applied.deposit(getDepositor(), getGatheredFrom(), 100);
        return applied;
    }

    @Override
    public EnumSet<StripsEnum> effects()
    {
        return StripsEnum.getValidNext(DEPOSIT);
    }

    @Override
    public long computeCostFactor()
    {
        return (long) getGatheredFrom().getDistanceToTownHall();
    }

    public Unit getDepositor()
    {
        return depositor;
    }

    public Resource getGatheredFrom()
    {
        return gatheredFrom;
    }

    public int getAmount()
    {
        return amount;
    }
}
