package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.agent.planner.GameState;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static edu.cwru.sepia.agent.planner.GameState.Unit;
import static edu.cwru.sepia.agent.planner.actions.StripsEnum.IDLE;
import static edu.cwru.sepia.agent.planner.actions.StripsEnum.PRODUCE;
import static edu.cwru.sepia.environment.model.state.ResourceType.GOLD;
import static edu.cwru.sepia.environment.model.state.ResourceType.WOOD;

public class Produce implements StripsAction
{
    private final Unit producer;
    private final int goldCost;
    private final int woodCost;

    public Produce(Unit producer, int goldCost, int woodCost)
    {
        this.producer = producer;
        this.goldCost = goldCost;
        this.woodCost = woodCost;
    }

    @Override
    public boolean preconditionsMet(GameState state)
    {
        boolean scheduled =
                state.getUnitTracker().containsAnyValue(PRODUCE, IDLE);
        boolean shouldConsider = state.considerBuildingPeasants();
        boolean enoughGold;
        enoughGold = state.getResourceTracker().hasEnough(GOLD, getGoldCost());
        boolean enoughWood;
        enoughWood = state.getResourceTracker().hasEnough(WOOD, getWoodCost());
        return scheduled && shouldConsider && enoughGold && enoughWood;
    }

    @Override
    public GameState apply(GameState state)
    {
        GameState applied = state.copy();
        applied.produce(getProducer(), getGoldCost(), getWoodCost());
        applied.getCreationActions().getActions().add(this);
        return applied;
    }

    @Override
    public Set<StripsAction> effects(GameState state)
    {
        StripsAction idle = new Idle(getProducer());
        StripsAction produce =
                new Produce(getProducer(), getGoldCost(), getWoodCost());
        Set<StripsAction> effects = new HashSet<>();
        effects.add(idle);
        effects.add(produce);
        return effects;
    }

    @Override
    public long computeCostFactor()
    {
        return (long) 0.5;
    }

    @Override
    public Action getSepiaAction(int... townHallThenTemplate)
    {
        int townHallId = townHallThenTemplate[0];
        int templateId = townHallThenTemplate[1];
        return Action.createCompoundProduction(townHallId, templateId);
    }

    @Override
    public ActionType getSepiaActionType()
    {
        return ActionType.COMPOUNDPRODUCE;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(getProducer(), getGoldCost(), getWoodCost());
    }

    public Unit getProducer()
    {
        return producer;
    }

    public int getGoldCost()
    {
        return goldCost;
    }

    public int getWoodCost()
    {
        return woodCost;
    }
}
