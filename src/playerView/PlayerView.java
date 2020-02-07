package playerView;

import action.ActionMap;
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
import utils.Location;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Unifying wrapper class that represents a player in SEPIA. Allows for a
 * player to access its {@link StateView} and {@link HistoryView}, thus
 * allowing a "player" to both consider the state of the game and the history
 * in order to make moves. This class is essentially the Agent, but without
 * needing to specify separating the {@link StateView} and{@link HistoryView}
 * with every method call.
 */
public class PlayerView
{
    private final int playerNum;
    private StateView state;
    private HistoryView history;
    private int numFarmsBuilt;
    private int numFarmsLimit;
    private int numPeasantsBuilt;
    private int numPeasantsLimit;
    private int numFootmenBuilt;
    private int numFootmenLimit;

    /**
     * Constructor explicitly calling for all fields of a PlayerView.
     *
     * @param playerNum        The SEPIA identifier given to the player that
     *                         relates to internal game mechanics.
     * @param state            The StateView of this player.
     * @param history          The HistoryView of this player.
     * @param numFarmsLimit    The number of farms to limit the player from
     *                         building.
     * @param numPeasantsLimit The number of peasants to limit the player
     *                         from building.
     * @param numFootmenLimit  The number of footmen to limit the player from
     *                         building.
     */
    public PlayerView(int playerNum,
                      StateView state,
                      HistoryView history,
                      int numFarmsLimit,
                      int numPeasantsLimit,
                      int numFootmenLimit)
    {
        this.playerNum = playerNum;
        this.state = state;
        this.history = history;
        this.numFarmsLimit = numFarmsLimit;
        this.numFarmsBuilt = 0;
        this.numPeasantsLimit = numPeasantsLimit;
        this.numPeasantsBuilt = 1;
        this.numFootmenLimit = numFootmenLimit;
        this.numFootmenBuilt = 0;
    }

    /**
     * Alternative PlayerView constructor that only requires the playernum,
     * the StateView and the HistoryView. All Unit limits are default set to 3.
     *
     * @param playerNum The SEPIA identifier given to the player that relates
     *                  to internal game mechanics.
     * @param state     The StateView of this player.
     * @param history   The HistoryView of this player.
     */
    public PlayerView(int playerNum,
                      StateView state,
                      HistoryView history)
    {
        this(playerNum, state, history, 3, 3, 3);
    }

    /**
     * Alternative PlayerView constructor that only requires the playernum.
     * The StateView and the HistoryView are set to null and all Unit limits
     * are default set to 3. This is the same constructor called by {@link
     * #createPlayer(int)}. Since the {@link StateView} and
     * {@link HistoryView} of the player have not been instantiated, these
     * fields must be set prior to all.
     *
     * @param playerNum The SEPIA identifier given to the player that relates
     *                  to internal game mechanics.
     */
    public PlayerView(int playerNum)
    {
        this(playerNum, null, null, 3, 3, 3);
    }

    /**
     * Factor method for instantiating PlayerView.
     *
     * @param playerNum The SEPIA identifier given to the player that relates
     *                  to internal game mechanics.
     * @return A new instance of PlayerView that has default Unit
     * limits, {@code null} StateView and {@code null} HistoryView.
     */
    public static PlayerView createPlayer(int playerNum)
    {
        return new PlayerView(playerNum);
    }

    /**
     * Filters a {@link List<UnitView>} that matches a specified
     * {@link Units} enum type.
     *
     * @param units    The list of {@link UnitView} instances to filter.
     * @param unitType The {@link Units} enum specifying which units are of
     *                 interest.
     * @return The filtered {@link List<UnitView>}. If no instances in the
     * list were found to match th {@code unitType}, an empty {@link
     * ArrayList<UnitView>} is returned, as opposed to {@code null}.
     */
    public List<UnitView> getUnitsByType(List<UnitView> units, Units unitType)
    {
        String unitTypeStr = unitType.toString();
        List<UnitView> filtered = units
                .stream()
                .filter(unit -> unit.getTemplateView().getName().equals(unitTypeStr))
                .collect(Collectors.toList());

        return filtered.isEmpty() ? new ArrayList<>() : filtered;
    }

    /**
     * Filters all {@link UnitView}s visible to the player that matches a
     * specified {@link Units} enum type.
     *
     * @param unitType The {@link Units} enum specifying which units are of
     *                 interest.
     * @return The filtered {@link List<UnitView>}. If no instances in the list
     * were found to match the {@code unitType}, an empty
     * {@link ArrayList<UnitView>} is returned, as opposed to {@code null}.
     */
    public List<UnitView> getUnitsByType(Units unitType)
    {
        return getUnitsByType(getUnits(), unitType);
    }

    /**
     * Filters all {@link UnitView}s visible to the player that matches a
     * specified {@link Units} enum type and returns the first instance in the
     * resulting list.
     *
     * @param unitType The {@link Units} enum specifying which units are of
     *                 interest.
     * @return The single {@link UnitView} of the filtered list.
     * @throws IndexOutOfBoundsException Thrown if no {@link UnitView} are in
     *                                   the list.
     */
    public UnitView getUnitByType(Units unitType) throws IndexOutOfBoundsException
    {
        return getUnitsByType(unitType).get(0);
    }

    /**
     * Explicit version of {@link #getUnitsByType(List, Units)} that filters
     * by {@link Units#PEASANT}.
     *
     * @param units The list of {@link UnitView} instances to filter.
     * @return Zero or more of the {@link UnitView}s in {@code units} that
     * are peasants.
     */
    public List<UnitView> getPeasants(List<UnitView> units)
    {
        return getUnitsByType(units, Units.PEASANT);
    }

    /**
     * Explicit version of {@link #getUnitsByType(List, Units)} that filters
     * by {@link Units#TOWNHALL}.
     *
     * @param units The list of {@link UnitView} instances to filter.
     * @return Zero or more of the {@link UnitView}s in {@code units} that
     * are town halls.
     */
    public List<UnitView> getTownHalls(List<UnitView> units)
    {
        return getUnitsByType(units, Units.TOWNHALL);
    }

    /**
     * Explicit version of {@link #getUnitsByType(Units)} that filters by
     * {@link Units#PEASANT}.
     *
     * @return Zero or more of the {@link UnitView}s in {@code units} that
     * are peasants.
     */
    public List<UnitView> getPeasants()
    {
        return getUnitsByType(Units.PEASANT);
    }

    /**
     * Explicit version of {@link #getUnitByType(Units)} that filters by
     * {@link Units#TOWNHALL}.
     *
     * @return Zero or more of the {@link UnitView}s in {@code units} that
     * are town halls.
     */
    public UnitView getTownHall()
    {
        return getUnitByType(Units.TOWNHALL);
    }

    /**
     * Wrapper method for determining if a {@link UnitView} is carrying cargo.
     *
     * @param unit Unit to test of its carrying cargo.
     * @return {@code True} if the unit is carrying cargo, and {@code False}
     * otherwise.
     */
    public boolean isCarryingCargo(UnitView unit)
    {
        return unit.getCargoAmount() > 0;
    }

    /**
     * Wrapper method for determining if a resource has a nonzero amount.
     *
     * @param resource Resource to test of it still has a remaining amount.
     * @return {@code True} if the resource has a remaining amount, and
     * {@code False} otherwise.
     */
    public boolean hasRemaining(ResourceView resource)
    {
        return resource.getAmountRemaining() > 0;
    }

    /**
     * Attempts to more efficiently return the closest resource node that has
     * remaining resources by first checking if the last visited, closest
     * resource node still has remaining resources. If it does not, then a
     * new closest resource node is searched.
     *
     * @param unit1        Unit that is used a start location.
     * @param townHall     Unit that is used as a destination.
     * @param resourceType Type of resource to collect.
     * @return Resource node with remaining resources that is along the
     * shortest path from a unit to a resource, and
     * back to the town hall.
     */
    public ResourceView findBestResource(UnitView unit1,
                                         UnitView townHall,
                                         ResourceNode.Type resourceType)
    {
        return lastResource(resourceType)
                .orElse(findClosestResource(unit1, townHall, resourceType));
    }

    /**
     * Finds the {@link ResourceView} such that the sum of Euclidean distance
     * from the collecting unit to the resource and the Euclidean distance
     * from the resource to the town hall is a minimum.
     *
     * @param unit     Unit that is used as a start location.
     * @param townHall Unit that is used as a destination.
     * @param type     Type of resource from which to collect.
     * @return The resource with the estimated minimum distance to travel.
     */
    public ResourceView findClosestResource(UnitView unit,
                                            UnitView townHall,
                                            ResourceNode.Type type)
    {
        List<ResourceView> resources = state.getResourceNodes(type);
        ResourceView closest = resources.get(0);
        double minCost = Double.POSITIVE_INFINITY;

        for (ResourceView resource : resources)
        {
            if (hasRemaining(resource))
            {
                double unitToRes = Location.distBetween(unit, resource);
                double resToHall = Location.distBetween(townHall, resource);
                double totalCost = unitToRes + resToHall;
                boolean currHasMore =
                        resource.getAmountRemaining() > closest.getAmountRemaining();
                if (totalCost == minCost && currHasMore)
                {
                    closest = resource;
                    minCost = totalCost;
                }
                if (totalCost < minCost)
                {
                    closest = resource;
                    minCost = totalCost;
                }
            }
        }

        return closest;
    }

    /**
     * Convenience method that determines if a player has enough wood and
     * gold to build a specified unit.
     *
     * @param toBuild Unit to build.
     * @return {@code True} if the player has enough wood and gold, and
     * {@code False} otherwise.
     */
    public boolean hasEnoughGoldAndWood(Units toBuild)
    {
        return hasEnoughGold(toBuild) && hasEnoughWood(toBuild);
    }

    /**
     * Determines if a player has enough gold to build a specified unit.
     *
     * @param toBuild Unit to build.
     * @return {@code True} if the player has enough gold, and {@code False}
     * otherwise.
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
     * @return {@code True} if the player has enough wood, and {@code False}
     * otherwise.
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
     * @return If the type of resource has been visited before, the most
     * recently visited resource node that has a remaining amount. If the type
     * of resource has not been visited, the{@link Optional} will contain
     * {@code null}.
     */
    public Optional<ResourceView> lastResource(ResourceNode.Type resourceType)
    {
        return history.getResourcePickupLogs(getPlayerNum())
                .stream()
                .map(ResourcePickupLog::getNodeID)
                .map(state::getResourceNode)
                .filter(node -> node.getType().equals(resourceType))
                .filter(this::hasRemaining)
                .findFirst();
    }

    public ActionMap depositResources(List<UnitView> units)
    {
        ActionMap actionMap = ActionMap.createActionsMap();
        List<UnitView> availableTownHalls = getTownHalls(units);

        if (canInteractWithResources(units))
        {
            int townHallId = availableTownHalls.get(0).getID();

            for (UnitView peasant : units)
                if (isCarryingCargo(peasant))
                    actionMap.assign(Action.createCompoundDeposit(
                            peasant.getID(),
                            townHallId
                    ));
        }

        return actionMap;
    }

    public boolean canInteractWithResources(List<UnitView> units)
    {
        return getTownHalls(units).size() > 0 && getPeasants(units).size() > 0;
    }

    public ActionMap buildPeasant(List<UnitView> units)
    {
        ActionMap actionMap = ActionMap.createActionsMap();
        List<UnitView> availableTownHalls = getTownHalls(units);

        if (canBuildMorePeasants(availableTownHalls))
        {
            int townHallId = availableTownHalls.get(0).getID();
            int templateId = getTemplate(Units.PEASANT).getID();

            actionMap.assign(Action.createCompoundProduction(
                    townHallId,
                    templateId
            ));

            if (actionMap.isEffective())
                setNumPeasantsBuilt(getNumPeasantsBuilt() + 1);
        }

        return actionMap;
    }

    /**
     * Determines of the state of the game allows the agent to build more
     * peasants.
     *
     * @return {@code True} if the supply cap has not been exceeded, and
     * {@code False} otherwise.
     */
    public boolean canBuildMorePeasants(List<UnitView> townHalls)
    {
        return hasEnoughGold(Units.PEASANT) &&
                getNumPeasantsBuilt() < getNumPeasantsLimit()
                && townHalls.size() > 0;
    }

    public ActionMap buildFarm(List<UnitView> units)
    {
        ActionMap actionMap = ActionMap.createActionsMap();

        if (canBuildMoreFarms(units))
        {
            for (UnitView peasant : units)
            {
                int farmBuilderId = peasant.getID();
                Location where = Location.randomLocation(state);
                int farmId = getTemplate(Units.FARM).getID();

                actionMap.assign(Action.createCompoundBuild(
                        farmBuilderId,
                        farmId,
                        where.getX(),
                        where.getY()
                ));
            }

            if (actionMap.isEffective())
                setNumFarmsBuilt(getNumFarmsBuilt() + 1);
        }

        return actionMap;
    }

    public boolean canBuildMoreFarms(List<UnitView> units)
    {
        return getPeasants(units).size() > 0 &&
                getNumFarmsBuilt() < getNumFarmsLimit() &&
                hasEnoughGoldAndWood(Units.FARM);
    }

    public ActionMap buildFootman(List<UnitView> units)
    {
        ActionMap actionMap = ActionMap.createActionsMap();
        List<UnitView> availableTownHalls = getTownHalls(units);

        if (canBuildMoreFootmen(availableTownHalls))
        {
            int townHallId = availableTownHalls.get(0).getID();

            actionMap.assign(Action.createCompoundProduction(
                    townHallId,
                    getTemplate(Units.FOOTMAN).getID()
            ));

            if (actionMap.isEffective())
                setNumFootmenBuilt(getNumFootmenBuilt() + 1);
        }
        return actionMap;
    }

    /**
     * Determines of the state of the game allows the agent to build more
     * peasants.
     *
     * @return {@code True} if the supply cap has not been exceeded, and
     * {@code False} otherwise.
     */
    public boolean canBuildMoreFootmen(List<UnitView> townHalls)
    {
        return hasEnoughGold(Units.FOOTMAN) &&
                getNumPeasantsBuilt() < getNumFootmenLimit() &&
                townHalls.size() > 0;
    }

    public ActionMap gatherBalancedResources(List<UnitView> units)
    {
        ActionMap gatherActions = ActionMap.createActionsMap();

        if (canInteractWithResources(units))
        {
            UnitView townHall = getTownHall();

            for (UnitView peasant : units)
            {
                int peasantId = peasant.getID();
                int resourceId = 0;

                if (hasLessGoldThanWood())
                {
                    resourceId = findBestResource(
                            peasant,
                            townHall,
                            ResourceNode.Type.GOLD_MINE
                    ).getID();

                } else
                {
                    resourceId = findBestResource(
                            peasant,
                            townHall,
                            ResourceNode.Type.TREE
                    ).getID();
                }
                gatherActions.assign(Action.createCompoundGather(
                        peasantId,
                        resourceId
                ));
            }
        }

        return gatherActions;
    }

    public boolean hasLessGoldThanWood()
    {
        return getResourceAmount(ResourceType.GOLD) <
                getResourceAmount(ResourceType.WOOD);
    }

    public boolean isIncomplete(ActionResult actionResult)
    {
        return actionResult.getFeedback() == ActionFeedback.INCOMPLETE;
    }

    /**
     * Determines if another player has any units.
     *
     * @param enemyPlayer Player to check against for number of units.
     * @return {@code True} if the player has at least 1 unit, and {@code
     * False} otherwise.
     */
    public static boolean noEnemiesExist(PlayerView enemyPlayer)
    {
        return enemyPlayer.getUnits().size() == 0;
    }

    public List<UnitView> getUnoccupiedUnits()
    {
        if (getTurnNumber() == 0)
            return getUnits();

        int prevTurn = getTurnNumber() - 1;
        Set<UnitView> busyUnits = getCommandFeedBackResult(prevTurn)
                .stream()
                .filter(this::isIncomplete)
                .map(this::getActionResultUnit)
                .collect(Collectors.toSet());

        Set<UnitView> allUnits = new HashSet<>(getUnits());
        allUnits.removeAll(busyUnits);

        return new ArrayList<>(allUnits);
    }

    public List<UnitView> getUnits()
    {
        return state.getUnits(getPlayerNum());
    }

    public UnitView getUnit(int unitId)
    {
        return state.getUnit(unitId);
    }

    public List<UnitView> getUnits(List<Integer> unitIds)
    {
        return unitIds.stream().map(this::getUnit).collect(Collectors.toList());
    }

    public List<Integer> getUnitIds(List<UnitView> units)
    {
        return units.stream().map(UnitView::getID).collect(Collectors.toList());
    }

    public List<Integer> getUnitIds()
    {
        return getUnitIds(getUnits());
    }

    public Map<Integer, ActionResult> getCommandFeedback(int step)
    {
        return history.getCommandFeedback(getPlayerNum(), step);
    }

    public List<ActionResult> getCommandFeedBackResult(int step)
    {
        return new ArrayList<>(getCommandFeedback(step).values());
    }

    public UnitView getActionResultUnit(ActionResult actionResult)
    {
        return getUnit(actionResult.getAction().getUnitId());
    }

    public int getResourceAmount(ResourceType type)
    {
        return state.getResourceAmount(getPlayerNum(), type);
    }

    public TemplateView getTemplate(Units unit)
    {
        return state.getTemplate(getPlayerNum(), unit.toString());
    }

    public int getTurnNumber()
    {
        return state.getTurnNumber();
    }

    public int getPlayerNum()
    {
        return playerNum;
    }

    public int getNumFarmsLimit()
    {
        return numFarmsLimit;
    }

    public int getNumFarmsBuilt()
    {
        return numFarmsBuilt;
    }

    public int getNumPeasantsBuilt()
    {
        return numPeasantsBuilt;
    }

    public int getNumPeasantsLimit()
    {
        return numPeasantsLimit;
    }

    public int getNumFootmenBuilt()
    {
        return numFootmenBuilt;
    }

    public int getNumFootmenLimit()
    {
        return numFootmenLimit;
    }

    public void setStateAndHistory(StateView state, HistoryView history)
    {
        setState(state);
        setHistory(history);
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

    public void setNumPeasantsBuilt(int numPeasantsBuilt)
    {
        this.numPeasantsBuilt = numPeasantsBuilt;
    }

    public void setNumFootmenBuilt(int numFootmenBuilt)
    {
        this.numFootmenBuilt = numFootmenBuilt;
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

        public String toString()
        {
            return unit;
        }
    }
}
