package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.GameState.Unit;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static edu.cwru.sepia.agent.planner.actions.StripsEnum.IDLE;

public class Idle implements StripsAction
{
    private final Unit unit;

    public Idle(Unit unit)
    {
        this.unit = unit;
    }

    @Override
    public boolean preconditionsMet(GameState state)
    {
        return state.getUnitTracker().containsValue(IDLE);
    }

    @Override
    public GameState apply(GameState state)
    {
        GameState applied = state.copy();
        applied.idle(getUnit());
        applied.getCreationActions().getActions().add(this);
        return applied;
    }

    @Override
    public Set<StripsAction> effects(GameState state)
    {
        Set<StripsAction> effects = new HashSet<>();
        StripsAction idle = new Idle(getUnit());
        StripsAction produce = new Produce(getUnit(),
                                     getUnit().getGoldCostToProduce(),
                                     getUnit().getWoodCostToProduce());
        effects.add(idle);
        effects.add(produce);
        effects.addAll(Gather.allPossibleGathers(getUnit(), state));
        return effects;
    }

    @Override
    public long computeCostFactor()
    {
        return 2;
    }

    @Override
    public Action getSepiaAction(int...none)
    {
        return null;
    }

    @Override
    public ActionType getSepiaActionType()
    {
        return null;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(getUnit());
    }

    public Unit getUnit()
    {
        return unit;
    }
}
