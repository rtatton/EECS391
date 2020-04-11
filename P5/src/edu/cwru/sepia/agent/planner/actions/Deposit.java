package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.GameState.Unit;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static edu.cwru.sepia.agent.planner.actions.StripsEnum.DEPOSIT;

public class Deposit implements StripsAction
{
    private final Unit depositor;
    private final int amount;
    private final long costFactor;

    public Deposit(Unit depositor, int amount, long costFactor)
    {
        this.depositor = depositor;
        this.amount = amount;
        this.costFactor = costFactor;
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
        applied.deposit(getDepositor(), 100);
        applied.getCreationActions().getActions().add(this);
        return applied;
    }

    @Override
    public Set<StripsAction> effects(GameState state)
    {
        Set<StripsAction> effects = new HashSet<>();
        StripsAction idle = new Idle(getDepositor());
        effects.add(idle);
        effects.addAll(Gather.allPossibleGathers(getDepositor(), state));
        return effects;
    }

    @Override
    public long computeCostFactor()
    {
        return getCostFactor();
    }

    @Override
    public Action getSepiaAction(int... unitThenTownHall)
    {
        int unitId = unitThenTownHall[0];
        int townHallId = unitThenTownHall[1];
        return Action.createCompoundDeposit(unitId, townHallId);
    }

    @Override
    public ActionType getSepiaActionType()
    {
        return ActionType.COMPOUNDDEPOSIT;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(getDepositor(), getAmount(), getCostFactor());
    }

    public Unit getDepositor()
    {
        return depositor;
    }

    public int getAmount()
    {
        return amount;
    }

    public long getCostFactor()
    {
        return costFactor;
    }
}
