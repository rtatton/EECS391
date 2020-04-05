package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.GameState.Resource;
import edu.cwru.sepia.agent.planner.GameState.Unit;

import java.util.EnumSet;

import static edu.cwru.sepia.agent.planner.actions.StripsEnum.DEPOSIT;
import static edu.cwru.sepia.agent.planner.actions.StripsEnum.IDLE;

public class Deposit implements StripsAction
{
    private final Unit depositer;
    private final Resource gatheredFrom;

    public Deposit(Unit depositer, Resource gatheredFrom)
    {
        this.depositer = depositer;
        this.gatheredFrom = gatheredFrom;
    }

    public double computeCost()
    {
        return getGatheredFrom().getDistanceToTownHall();
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
        applied.deposit(getDepositer(), getGatheredFrom().getType(), 100);
        applied.getUnitTracker().validateAndTrack(getDepositer(), IDLE);
        return applied;
    }

    @Override
    public EnumSet<StripsEnum> effects()
    {
        return StripsEnum.getValidNext(DEPOSIT);
    }

    @Override
    public StripsEnum getStripsActionType()
    {
        return DEPOSIT;
    }

    public Unit getDepositer()
    {
        return depositer;
    }

    public Resource getGatheredFrom()
    {
        return gatheredFrom;
    }

}
