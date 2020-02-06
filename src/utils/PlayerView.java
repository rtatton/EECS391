package utils;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionFeedback;
import edu.cwru.sepia.action.ActionResult;
import edu.cwru.sepia.environment.model.history.History.HistoryView;
import edu.cwru.sepia.environment.model.history.ResourcePickupLog;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.ResourceNode.ResourceView;
import edu.cwru.sepia.environment.model.state.ResourceType;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Template.TemplateView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Unifying interface that represents a player in SEPIA. Allows for a player to access its {@link StateView} and
 * {@link HistoryView}.
 */
public class PlayerView
{
    private final int playerNum;
    private StateView state;
    private HistoryView history;
    private int numFarmsBuilt;
    private int numFarmsLimit;

    public PlayerView(int playerNum, StateView state, HistoryView history)
    {
        this.playerNum = playerNum;
        this.state = state;
        this.history = history;
    }

    public PlayerView(int playerNum)
    {
        this(playerNum, null, null);
    }

    public int getPlayerNum()
    {
        return playerNum;
    }

    /***
     * @param unitType The type of unit by which to filter.
     * @return List of {@link UnitView} objects belonging to the filtered units.
     */
    public List<UnitView> getUnitsByType(Units unitType)
    {
        return getUnits()
                .stream()
                // Filter units whose name matches the unitType parameter.
                .filter(unit -> unit.getTemplateView().getName().equals(unitType.getUnit()))
                // Store the filtered information in a List.
                .collect(Collectors.toList());
    }

    /**
     * Determines of the state of the game allows the agent to build more peasants.
     *
     * @return {@code True} if the supply cap has not been exceeded, and {@code False} otherwise.
     */
    public boolean canBuildMorePeasants()
    {
        List<UnitView> peasants = getUnitsByType(Units.PEASANT);

        return hasEnoughGold(Units.PEASANT) && peasants.size() < getSupplyCap();
    }

    /**
     * Wrapper method for determining if a unit is carrying cargo.
     *
     * @param unit Unit to test of its carrying cargo.
     * @return {@code True} if the unit is carrying cargo, and {@code False} otherwise.
     */
    public boolean isCarryingCargo(UnitView unit)
    {
        return unit.getCargoAmount() > 0;
    }

    /**
     * Wrapper method for determining if a resource has a nonzero amount.
     *
     * @param resource Resource to test of it still has a remaining amount.
     * @return {@code True} if the resource has a remaining amount, and {@code False} otherwise.
     */
    public boolean hasRemaining(ResourceView resource)
    {
        return resource.getAmountRemaining() > 0;
    }

    /**
     * Attempts to more efficiently return the closest resource node that has remaining resources by first checking if
     * the last visited, closest resource node still has remaining resources. If it does not, then a new closest
     * resource node is searched.
     *
     * @param unit1 Unit that is used a start location.
     * @param townHall Unit that is used as a destination.
     * @param resourceType Type of resource to collect.
     * @return Resource node with remaining resources that is along the shortest path from a unit to a resource, and
     * back to the town hall.
     */
    public ResourceView findBestResource(UnitView unit1, UnitView townHall, ResourceNode.Type resourceType)
    {
        return lastResourceWithRemaining(resourceType).orElse(findClosestResource(unit1, townHall, resourceType));
    }

    /**
     * Finds the {@link ResourceView} such that the sum of Euclidean distance from the collecting unit to the resource
     * and the Euclidean distance from the resource to the town hall is a minimum.
     *
     * @param unit Unit that is used as a start location.
     * @param townHall Unit that is used as a destination.
     * @param type Type of resource from which to collect.
     * @return The resource with the estimated minimum distance to travel.
     */
    public ResourceView findClosestResource(UnitView unit, UnitView townHall, ResourceNode.Type type)
    {
        List<ResourceView> resources = state.getResourceNodes(type);
        ResourceView closest = resources.get(0);
        double minPathCost = Double.POSITIVE_INFINITY;

        for (ResourceView resource : resources)
        {
            if (hasRemaining(resource))
            {
                double totalPathCost = Location.distBetween(unit, resource) + Location.distBetween(townHall, resource);

                if (totalPathCost == minPathCost && resource.getAmountRemaining() > closest.getAmountRemaining())
                {
                    closest = resource;
                    minPathCost = totalPathCost;
                }

                if (totalPathCost < minPathCost)
                {
                    closest = resource;
                    minPathCost = totalPathCost;
                }
            }
        }
        return closest;
    }

    /**
     * Convenience method that determines if a player has enough wood and gold to build a specified unit.
     *
     * @param toBuild Unit to build.
     * @return {@code True} if the player has enough wood and gold, and {@code False} otherwise.
     */
    public boolean hasEnoughGoldAndWood(Units toBuild)
    {
        return hasEnoughGold(toBuild) && hasEnoughWood(toBuild);
    }

    /**
     * Determines if a player has enough gold to build a specified unit.
     *
     * @param toBuild Unit to build.
     * @return {@code True} if the player has enough gold, and {@code False} otherwise.
     */
    public boolean hasEnoughGold(Units toBuild)
    {
        int goldCost = getTemplate(toBuild).getGoldCost();
        int currGold = getResourceAmount(ResourceType.GOLD);
        return goldCost <= currGold;
    }

    /**
     * Determines if a player has enough wood to build a specified unit.
     *
     * @param toBuild Unit to build.
     * @return {@code True} if the player has enough wood, and {@code False} otherwise.
     */
    public boolean hasEnoughWood(Units toBuild)
    {
        int woodCost = getTemplate(toBuild).getWoodCost();
        int currWood = getResourceAmount(ResourceType.WOOD);
        return woodCost <= currWood;
    }

    /**
     * Finds the last visited resource that still has a remaining amount.
     *
     * @param resourceType Resource type to consider.
     * @return If the type of resource has been visited before, the most recently visited resource node that has a
     * remaining amount. If the type of resource has not been visited, the {@link Optional} will contain {@code null}.
     */
    public Optional<ResourceView> lastResourceWithRemaining(ResourceNode.Type resourceType)
    {
        return history.getResourcePickupLogs(getPlayerNum())
                .stream()
                // Get the resource nodes that have been visited.
                .map(ResourcePickupLog::getNodeID)
                .map(state::getResourceNode)
                // Consider only the resource nodes that match the resource type.
                .filter(node -> node.getType().equals(resourceType))
                // Keep only those nodes that have remaining resources.
                .filter(this::hasRemaining)
                // Return the most recent node.
                .findFirst();
    }

    public ActionsMap buildFarm(UnitView farmBuilder)
    {
        ActionsMap actionsMap = new ActionsMap(getPlayerNum());

        if (getNumFarmsBuilt() < getNumFarmsLimit() && hasEnoughGoldAndWood(Units.FARM))
        {
            Location where = Location.randomLocation(state);
            int farmId = getTemplate(Units.FARM).getID();
            actionsMap.assign(Action.createCompoundBuild(farmBuilder.getID(), farmId, where.getX(), where.getY()));
        }

        if (actionsMap.getMap().size() > 0)
            setNumFarmsBuilt(getNumFarmsBuilt() + 1);

        return actionsMap;
    }

    public ActionsMap depositResources(List<UnitView> units, UnitView townHall)
    {
        ActionsMap actionsMap = new ActionsMap(getPlayerNum());

        for (UnitView peasant : units)
            actionsMap.assign(Action.createCompoundDeposit(peasant.getID(), townHall.getID()));

        return actionsMap;
    }

    public ActionsMap buildPeasant(UnitView townHall)
    {
        ActionsMap actionsMap = new ActionsMap(getPlayerNum());

        if (canBuildMorePeasants())
            actionsMap.assign(Action.createCompoundProduction(townHall.getID(), getTemplate(Units.PEASANT).getID()));

        return actionsMap;
    }

    public ActionsMap gatherBalancedResources(List<UnitView> units, UnitView townHall)
    {
        ActionsMap gatherActions = new ActionsMap(getPlayerNum());

        for (UnitView peasant : units)
        {
            int peasantId = peasant.getID();

            if (getResourceAmount(ResourceType.GOLD) < getResourceAmount(ResourceType.WOOD))
            {
                int bestGoldMineId = findBestResource(peasant, townHall, ResourceNode.Type.GOLD_MINE).getID();
                gatherActions.assign(Action.createCompoundGather(peasantId, bestGoldMineId));
            } else
            {
                int bestTreeId = findBestResource(peasant, townHall, ResourceNode.Type.TREE).getID();
                gatherActions.assign(Action.createCompoundGather(peasantId, bestTreeId));
            }
        }

        return gatherActions;
    }

    public boolean isIncomplete(ActionResult actionResult)
    {
        return actionResult.getFeedback() == ActionFeedback.INCOMPLETE;
    }

    /**
     * Determines if another player has any units.
     *
     * @param enemyPlayer Player to check against for number of units.
     * @return {@code True} if the player has at least 1 unit, and {@code False} otherwise.
     */
    public static boolean noEnemiesExist(PlayerView enemyPlayer)
    {
        return enemyPlayer.getUnits().size() == 0;
    }

    public List<UnitView> getUnoccupiedUnits()
    {
        if (getTurnNumber() == 0)
            return getUnits();

        return this.getCommandFeedback(getTurnNumber() - 1).values()
                .stream()
                .filter(actionResult -> !isIncomplete(actionResult))
                .map(actionResult -> getUnit((actionResult.getAction().getUnitId())))
                .collect(Collectors.toList());
    }

    public int getNumFarmsLimit()
    {
        return numFarmsLimit;
    }

    public int getNumFarmsBuilt()
    {
        return numFarmsBuilt;
    }

    public List<UnitView> getUnits()
    {
        return state.getUnits(getPlayerNum());
    }

    public UnitView getUnit(int unitId)
    {
        return state.getUnit(unitId);
    }

    public Map<Integer, ActionResult> getCommandFeedback(int step)
    {
        return history.getCommandFeedback(getPlayerNum(), step);
    }

    public int getResourceAmount(ResourceType type)
    {
        return state.getResourceAmount(getPlayerNum(), type);
    }

    public TemplateView getTemplate(Units unit)
    {
        return state.getTemplate(getPlayerNum(), unit.getUnit());
    }

    public int getSupplyCap()
    {
        return state.getSupplyCap(getPlayerNum());
    }

    public int getTurnNumber()
    {
        return state.getTurnNumber();
    }

    public void setState(StateView state)
    {
        this.state = state;
    }

    public void setHistory(HistoryView history)
    {
        this.history = history;
    }

    public void setNumFarmsBuilt(int farmsBuilt)
    {
        this.numFarmsBuilt = farmsBuilt;
    }

    public void setNumFarmsLimit(int farmLimit)
    {
        this.numFarmsLimit = farmLimit;
    }

    /**
     * Units corresponding to those available in the configuration file.
     */
    public enum Units
    {
        BARRACKS("Barracks"),
        FARM("Farm"),
        FOOTMAN("Footman"),
        PEASANT("Peasant"),
        TOWNHALL("TownHall");

        private final String unit;

        Units(final String unit)
        {
            this.unit = unit;
        }

        public String getUnit()
        {
            return unit;
        }
    }
}
