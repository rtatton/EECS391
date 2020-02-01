package utils;

import edu.cwru.sepia.action.ActionResult;
import edu.cwru.sepia.environment.model.history.History.HistoryView;
import edu.cwru.sepia.environment.model.history.ResourcePickupLog;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.ResourceNode.ResourceView;
import edu.cwru.sepia.environment.model.state.ResourceType;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Template.TemplateView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import edu.cwru.sepia.util.DistanceMetrics;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Unifying interface that represents a player in SEPIA. Allows for a player to access its {@link StateView} and
 * {@link HistoryView}.
 */
public class PlayerState
{
    private final int playerNum;
    private final StateView state;
    private final HistoryView history;
    private final List<Integer> unitIds;

    public PlayerState(int playerNum, StateView state, HistoryView history)
    {
        this.playerNum = playerNum;
        this.state = state;
        this.history = history;
        this.unitIds = state.getUnitIds(playerNum);
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
     * @param unit {@link UnitView} to locate.
     * @return A {@link Location} instance that contains the X and Y positions of the {@link UnitView}.
     */
    public Location getUnitLocation(UnitView unit)
    {
        return new Location(unit);
    }

    /**
     * @param resource {@link ResourceView} to locate.
     * @return A {@link Location} instance that contains the X and Y positions of the {@link ResourceView}.
     */
    public Location getResourceLocation(ResourceView resource)
    {
        return new Location(resource);
    }

    /**
     * @param unit1
     * @param unit2
     * @return Euclidean distance between two {@link UnitView}s.
     */
    public double getDistanceBetween(UnitView unit1, UnitView unit2)
    {
        Location loc1 = getUnitLocation(unit1);
        Location loc2 = getUnitLocation(unit2);
        return DistanceMetrics.euclideanDistance(loc2.X(), loc2.Y(), loc1.X(), loc1.Y());
    }

    /**
     * @param resource1
     * @param resource2
     * @return Euclidean distance between two {@link ResourceView}s.
     */
    public double getDistanceBetween(ResourceView resource1, ResourceView resource2)
    {
        Location loc1 = getResourceLocation(resource1);
        Location loc2 = getResourceLocation(resource2);
        return DistanceMetrics.euclideanDistance(loc2.X(), loc2.Y(), loc1.X(), loc1.Y());
    }

    /**
     * @param unit
     * @param resource
     * @return Euclidean distance between a {@link UnitView} and {@link ResourceView}.
     */
    public double getDistanceBetween(UnitView unit, ResourceView resource)
    {
        Location unitLoc = getUnitLocation(unit);
        Location resourceLoc = getResourceLocation(resource);
        return DistanceMetrics.euclideanDistance(resourceLoc.X(), resourceLoc.Y(), unitLoc.X(), unitLoc.Y());
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
     * @param unit
     * @return {@code True} if the unit is carrying cargo, and {@code False} otherwise.
     */
    public boolean isCarryingCargo(UnitView unit)
    {
        return unit.getCargoAmount() > 0;
    }

    /**
     * Wrapper method for determining if a resource has a nonzero amount.
     *
     * @param resource
     * @return {@code True} if the resource has a remaining amount, and {@code False} otherwise.
     */
    public boolean hasRemaining(ResourceView resource)
    {
        return resource.getAmountRemaining() > 0;
    }

    /**
     * Finds the {@link ResourceView} such that the sum of Euclidean distance from the collecting unit to the resource
     * and the Euclidean distance from the resource to the town hall is a minimum.
     *
     * @param unit
     * @param townHall
     * @param type     Type of resource from which to collect.
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
                double totalPathCost = getDistanceBetween(unit, resource) + getDistanceBetween(townHall, resource);

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

    /**
     * Attempts to more efficiently return the closest resource node that has remaining resources by first checking if
     * the last visited, closest resource node still has remaining resources. If it does not, then a new closest
     * resource node is searched.
     *
     * @param unit1
     * @param unit2
     * @param resourceType Type of resource to collect.
     * @return Resource node with remaining resources that is along the shortest path from a unit to a resource, and
     * back to the town hall.
     */
    public ResourceView findBestResource(UnitView unit1, UnitView unit2, ResourceNode.Type resourceType)
    {
        ResourceView closest = findClosestResource(unit1, unit2, resourceType);
        return lastResourceWithRemaining(resourceType).orElseGet(() -> closest);
    }

    /**
     * Convenience method that determines if a player has enough wood and gold to build a specified unit.
     *
     * @param toBuild
     * @return {@code True} if the player has enough wood and gold, and {@code False} otherwise.
     */
    public boolean hasEnoughGoldAndWood(Units toBuild)
    {
        return hasEnoughGold(toBuild) && hasEnoughGold(toBuild);
    }

    /**
     * Determines if a player has enough gold to build a specified unit.
     *
     * @param toBuild
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
     * @param toBuild
     * @return {@code True} if the player has enough wood, and {@code False} otherwise.
     */
    public boolean hasEnoughWood(Units toBuild)
    {
        int woodCost = getTemplate(toBuild).getWoodCost();
        int currWood = getResourceAmount(ResourceType.WOOD);
        return woodCost <= currWood;
    }

    /**
     * Determines if another player has any units.
     *
     * @param enemyPlayer
     * @return {@code True} if the player has at least 1 unit, and {@code False} otherwise.
     */
    public static boolean noEnemiesExist(PlayerState enemyPlayer)
    {
        return enemyPlayer.getUnits().size() == 0;
    }

    public List<UnitView> getUnits()
    {
        return state.getUnits(getPlayerNum());
    }

    public UnitView getUnit(int unitId)
    {
        return state.getUnit(unitId);
    }

    public List<Integer> getUnitIds()
    {
        return unitIds;
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

    /**
     * Units corresponding to those available in the configuration file.
     */
    public enum Units
    {
        TOWNHALL("TownHall"),
        PEASANT("Peasant");

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
