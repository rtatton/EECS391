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
    private Unit producer;
    private Unit product;

    public Produce(Unit producer, Unit product)
    {
        this.producer = producer;
        this.product = product;
    }

    // TODO This type of precondition could be a chicken-and-egg problem
    @Override
    public boolean preconditionsMet(GameState state)
    {
        boolean scheduled;
        boolean shouldConsider;
        boolean hasEnoughGold;
        boolean hasEnoughWood;
        scheduled = state.getUnitTracker().getItems().containsValue(PRODUCE);
        shouldConsider = state.considerBuildingPeasants();
        int goldCost = getProduct().getGoldCostToProduce();
        int woodCost = getProduct().getWoodCostToProduce();
        hasEnoughGold = state.getResourceTracker().hasEnough(GOLD, goldCost);
        hasEnoughWood = state.getResourceTracker().hasEnough(WOOD, woodCost);
        return scheduled && shouldConsider && hasEnoughGold && hasEnoughWood;
    }

    @Override
    public GameState apply(GameState state)
    {
        GameState applied = new GameState(state);
        applied.produce(getProducer(),
                        getProduct().getGoldCostToProduce(),
                        getProduct().getWoodCostToProduce());
        applied.getUnitTracker().validateAndTrack(getProducer(), IDLE);
        return applied;
    }

    @Override
    public EnumSet<StripsEnum> effects()
    {
        return StripsEnum.getValidNext(PRODUCE);
    }

    // TODO Modify for division effect
    @Override
    public double computeCost()
    {
        int goldCost = getProduct().getGoldCostToProduce();
        int woodCost = getProduct().getWoodCostToProduce();
        return goldCost + woodCost;
    }

    @Override
    public StripsEnum getStripsActionType()
    {
        return PRODUCE;
    }

    public Unit getProducer()
    {
        return producer;
    }

    public Unit getProduct()
    {
        return product;
    }
}
