package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.GameState;

import java.util.EnumSet;

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
        boolean scheduled = state.getUnitTracker().containsAnyValue(PRODUCE, IDLE);
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
        return applied;
    }

    @Override
    public EnumSet<StripsEnum> effects()
    {
        return StripsEnum.getValidNext(PRODUCE);
    }

    @Override
    public long computeCostFactor()
    {
        return (long) 0.5;
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
