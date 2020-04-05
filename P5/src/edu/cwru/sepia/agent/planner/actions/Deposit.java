package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.GameState.Resource;
import edu.cwru.sepia.agent.planner.GameState.Unit;

import java.util.EnumSet;

import static edu.cwru.sepia.agent.planner.actions.StripsEnum.DEPOSIT;
import static edu.cwru.sepia.agent.planner.actions.StripsEnum.IDLE;

public class Deposit implements StripsAction
{
    private final Unit depositor;
    private final Resource gatheredFrom;

    public Deposit(Unit depositor, Resource gatheredFrom)
    {
        this.depositor = depositor;
        this.gatheredFrom = gatheredFrom;
    }

    @Override
    public boolean preconditionsMet(GameState state)
    {
        return state.getUnitTracker().getItems().containsValue(DEPOSIT);
    }

    @Override
    public GameState apply(GameState state)
    {
        GameState applied = new GameState(state);
        applied.deposit(getDepositor(), getGatheredFrom().getType(), 100);
        applied.getUnitTracker().validateAndTrack(getDepositor(), IDLE);
        return applied;
    }

    @Override
    public EnumSet<StripsEnum> effects()
    {
        return StripsEnum.getValidNext(DEPOSIT);
    }

    @Override
    public double computeCost()
    {
        return getGatheredFrom().getDistanceToTownHall();
    }

    public Unit getDepositor()
    {
        return depositor;
    }

    public Resource getGatheredFrom()
    {
        return gatheredFrom;
    }

}
