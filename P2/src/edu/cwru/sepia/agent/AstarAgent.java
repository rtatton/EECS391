package edu.cwru.sepia.agent;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.environment.model.history.History.HistoryView;
import edu.cwru.sepia.environment.model.state.ResourceNode.ResourceView;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import edu.cwru.sepia.util.Direction;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

public class AstarAgent extends Agent
{
    private static final int NOT_FOUND = -1;
    private Stack<Location> path;
    private int footmanID;
    private int townhallID;
    private int enemyFootmanID;
    private Location nextLoc;
    private long totalPlanTime = 0; // nsecs
    private long currentPlanTime = 0;
    private long totalExecutionTime = 0; //nsecs

    /**
     * Wrapper to store instances of SEPIA Actions. An ActionMap maintains the
     * {@code HashMap<Integer, Action>} underlying data structure that SEPIA
     * uses to access Actions. The advantages of an {@link ActionMap} include
     * being able to group multiple instances of the same type of Action to
     * multiple units and creating/assigning Actions in a more syntactically
     * understandable manner (i.e., with the {@link #assign(Action...)}.
     *
     * @author Ryan Tatton
     * @since P1
     */
    class ActionMap
    {
        private HashMap<Integer, Action> actionMap;

        private ActionMap()
        {
            this.actionMap = new HashMap<>();
        }

        /**
         * Adds one or more {@link Action}s, to {@link #actionMap}.
         *
         * @param actions One or more {@link Action}s to assign to the unit that
         *                is specified.
         */
        public void assign(Action... actions)
        {
            for (Action action : actions)
                getMap().put(action.getUnitId(), action);
        }

        public void attackTownhall(int attacker, int townhall)
        {
            this.assignPrimitiveAttack(attacker, townhall);
        }

        public void move(int mover, Direction direction)
        {
            this.assignPrimitiveMove(mover, direction);
        }

        public void assignPrimitiveAttack(int attacker, int target)
        {
            this.assign(Action.createPrimitiveAttack(attacker, target));
        }

        public void assignPrimitiveMove(int mover, Direction direction)
        {
            this.assign(Action.createPrimitiveMove(mover, direction));
        }

        public HashMap<Integer, Action> getMap()
        {
            return actionMap;
        }
    }

    /**
     * Factory method for instantiating an empty ActionMap.
     *
     * @return A new, empty ActionMap.
     */
    public ActionMap createActionMap()
    {
        return new ActionMap();
    }

    /**
     * Utility class that provides a simpler interface to access the location
     * of a {@link UnitView} or {@link ResourceView}.
     *
     * @author Ryan Tatton
     * @since P1
     */
    class Location
    {
        private final int X;
        private final int Y;
        private final int cost;
        private final Location cameFrom;

        private Location(ResourceView resource, Location cameFrom, int cost)
        {
            this.X = resource.getXPosition();
            this.Y = resource.getYPosition();
            this.cameFrom = cameFrom;
            this.cost = cost;
        }

        private Location(UnitView unit, Location cameFrom, int cost)
        {
            this.X = unit.getXPosition();
            this.Y = unit.getYPosition();
            this.cameFrom = cameFrom;
            this.cost = cost;
        }

        private Location(int X, int Y, Location cameFrom, int cost)
        {
            this.X = X;
            this.Y = Y;
            this.cameFrom = cameFrom;
            this.cost = cost;
        }

        public Location minus(Location location)
        {
            return locate(
                    this.getX() - location.getX(),
                    this.getY() - location.getY()
            );
        }

        public Location absMinus(Location location)
        {
            return locate(
                    Math.abs(this.getX() - location.getX()),
                    Math.abs(this.getY() - location.getY())
            );
        }

        public boolean equals(Location loc)
        {
            return this.getX() == loc.getX() && this.getY() == loc.getY();
        }

        public int getX()
        {
            return X;
        }

        public int getY()
        {
            return Y;
        }

        public Location getCameFrom()
        {
            return cameFrom;
        }

        public int getCost()
        {
            return cost;
        }
    }

    /**
     * @param unit {@link UnitView} to locate.
     * @return A {@link Location} instance that contains the X and Y
     * positions of the {@link UnitView}.
     */
    public Location locate(UnitView unit)
    {
        return new Location(unit, null, 0);
    }

    /**
     * @param resource {@link ResourceView} to locate.
     * @return A {@link Location} instance that contains the X and Y
     * positions of the {@link ResourceView}.
     */
    public Location locate(ResourceView resource)
    {
        return new Location(resource, null, 0);
    }

    public Location locate(int X, int Y)
    {
        return new Location(X, Y, null, 0);
    }

    public <T> boolean isNull(T item)
    {
        return item == null;
    }

    public AstarAgent(int playernum)
    {
        super(playernum);

        System.out.println("Constructed AstarAgent");
    }

    @Override
    public Map<Integer, Action> initialStep(StateView state,
                                            HistoryView history)
    {
        List<Integer> unitIDs = state.getUnitIds(getPlayerNumber());
        if (unitIDs.isEmpty())
        {
            System.err.println("No units found!");
            return null;
        }

        setFootmanID(unitIDs.get(0));
        if (!isFootman(state))
        {
            System.err.println("Footman unit not found");
            return null;
        }

        int enemyPlayerNum = findEnemyPlayerNum(state);
        if (enemyPlayerNum == NOT_FOUND)
        {
            System.err.println("Failed to get enemy playernumber");
            return null;
        }

        List<Integer> enemyUnitIDs = state.getUnitIds(enemyPlayerNum);
        if (enemyUnitIDs.isEmpty())
        {
            System.err.println("Failed to find enemy units");
            return null;
        }

        findTownhallAndEnemy(state, enemyUnitIDs);
        if (getTownhallID() == NOT_FOUND)
        {
            System.err.println("Error: Couldn't find townhall");
            return null;
        }

        long startTime = System.nanoTime();
        setPath(findPath(state));
        updateTotalPlanTime(System.nanoTime() - startTime);

        return middleStep(state, history);
    }

    public int findEnemyPlayerNum(StateView state)
    {
        Integer[] playerNums = state.getPlayerNumbers();
        int enemyPlayerNum = NOT_FOUND;
        for (Integer playerNum : playerNums)
        {
            if (playerNum != getPlayerNumber())
            {
                enemyPlayerNum = playerNum;
                break;
            }
        }
        return enemyPlayerNum;
    }

    public void findTownhallAndEnemy(StateView state, List<Integer> enemyUnits)
    {

        setTownhallID(NOT_FOUND);
        setEnemyFootmanID(NOT_FOUND);

        for (Integer unitID : enemyUnits)
        {
            UnitView tempUnit = state.getUnit(unitID);
            if (isTownhall(tempUnit))
                setTownhallID(unitID);
            else if (isFootman(tempUnit))
                setEnemyFootmanID(unitID);
            else
                System.err.println("Unknown unit type");
        }

    }

    public boolean isFootman(StateView state)
    {
        return Units.FOOTMAN.equals(getFootman(state));
    }

    public boolean isFootman(UnitView unit)
    {
        return Units.FOOTMAN.equals(unit);
    }

    public boolean isTownhall(UnitView unit)
    {
        return Units.TOWNHALL.equals(unit);
    }

    @Override
    public Map<Integer, Action> middleStep(StateView state, HistoryView history)
    {
        long startTime = System.nanoTime();
        resetCurrentPlanTime();

        ActionMap actionMap = createActionMap();

        if (shouldReplanPath(state, history, getPath()))
            timeAndUpdatePathReplan(state);

        Location footmanLoc = locate(getFootman(state));

        if (pathExists() && nextLocExists() && footmanNotAtNextLoc(footmanLoc))
        {
            // start moving to the next step in the path
            setNextLoc(getPath().pop());
            int xLoc = getNextLoc().getX();
            int yLoc = getNextLoc().getY();
            System.out.println("Moving to (" + xLoc + ", " + yLoc + ")");
        }

        //
        if (nextLocExists() && footmanNotAtNextLoc(footmanLoc))
        {
            Location diff = getNextLoc().minus(footmanLoc);
            Direction nextDir = getNextDirection(diff.getX(), diff.getY());
            actionMap.move(getFootmanID(), nextDir);
        } else
        {
            UnitView townHall = getTownhall(state);
            if (townHallWasDestroyed(townHall))
            {
                terminalStep(state, history);
                return actionMap.getMap();
            }
            if (isTooFarFromTownhall(footmanLoc, locate(townHall)))
            {
                System.err.println("Invalid plan. Cannot attack townhall");
                long diffFromStart = System.nanoTime() - startTime;
                long diffFromCurrent = diffFromStart - getCurrentPlanTime();
                updateTotalExecutionTime(diffFromCurrent);
                return actionMap.getMap();
            } else
            {
                System.out.println("Attacking TownHall");
                actionMap.attackTownhall(getFootmanID(), getTownhallID());
            }
        }
        long diffFromStart = System.nanoTime() - startTime;
        updateTotalExecutionTime(diffFromStart - getCurrentPlanTime());
        return actionMap.getMap();
    }

    public boolean isTooFarFromTownhall(Location footman, Location townhall)
    {
        Location absoluteDiff = footman.absMinus(townhall);
        return absoluteDiff.getX() > 1 || absoluteDiff.getY() > 1;
    }

    public boolean townHallWasDestroyed(UnitView townHall)
    {
        return isNull(townHall);
    }

    // TODO Current plan time setting might cause bugs
    public void timeAndUpdatePathReplan(StateView state)
    {
        long planStartTime = System.nanoTime();
        setPath(findPath(state));
        setCurrentPlanTime(System.nanoTime() - planStartTime);
        updateTotalPlanTime(getCurrentPlanTime());
    }

    public boolean pathExists()
    {
        return !getPath().isEmpty();
    }

    public boolean nextLocExists()
    {
        return isNull(getNextLoc());
    }

    public boolean footmanNotAtNextLoc(Location footmanLoc)
    {
        return footmanLoc.equals(getNextLoc());
    }

    @Override
    public void terminalStep(StateView state, HistoryView history)
    {
        System.out.println("Total turns: " + state.getTurnNumber());
        System.out.println("Total planning time: " + totalPlanTime / 1e9);
        System.out.println("Total execution time: " + totalExecutionTime / 1e9);
        System.out.println("Total time: " + (totalExecutionTime + totalPlanTime) / 1e9);
    }

    @Override
    public void savePlayerData(OutputStream os)
    {

    }

    @Override
    public void loadPlayerData(InputStream is)
    {

    }

    /**
     * You will implement this method.
     * <p>
     * This method should return true when the path needs to be replanned
     * and false otherwise. This will be necessary on the dynamic map
     * where the
     * footman will move to block your unit.
     * <p>
     * You can check the position of the enemy footman with the following
     * code:
     * state.getUnit(enemyFootmanID).getXPosition() or .getYPosition().
     * <p>
     * There are more examples of getting the positions of objects in SEPIA
     * in the findPath method.
     *
     * @param state
     * @param history
     * @param currentPath
     * @return
     */
    private boolean shouldReplanPath(StateView state,
                                     HistoryView history,
                                     Stack<Location> currentPath)
    {
        return false;
    }

    /**
     * This method is implemented for you. You should look at it to see
     * examples of
     * how to find units and resources in Sepia.
     *
     * @param state
     * @return
     */
    private Stack<Location> findPath(StateView state)
    {
        Location footmanLoc = null;
        if (!isNull(getEnemyFootmanID() != NOT_FOUND))
            footmanLoc = locate(getEnemyFootman(state));

        List<Integer> resourceIDs = state.getAllResourceIds();
        // TODO Define hash function
        Set<Location> resourceLocations = new HashSet<>();
        for (Integer resourceID : resourceIDs)
            resourceLocations.add(locate(state.getResourceNode(resourceID)));

        Location startLoc = locate(getFootman(state));
        Location goalLoc = locate(getTownhall(state));

        return AstarSearch(
                startLoc,
                goalLoc,
                state.getXExtent(),
                state.getYExtent(),
                footmanLoc,
                resourceLocations
        );
    }

    /**
     * This is the method you will implement for the assignment. Your
     * implementation will use the A* algorithm to compute the optimum path
     * from the start position to a position adjacent to the goal position.
     * <p>
     * Therefore your you need to find some possible adjacent steps which are
     * in range and are not trees or the enemy footman.
     * Hint: Set<MapLocation> resourceLocations contains the locations of trees
     * <p>
     * You will return a Stack of positions with the top of the stack being
     * the first space to move to and the bottom of the stack being the last
     * space to move to. If there is no path to the townhall then return null
     * from the method and the agent will print a message
     * and do nothing. The code to execute the plan is provided for you in the
     * middleStep method.
     * <p>
     * As an example consider the following simple map
     * <p>
     * F - - - -
     * x x x - x
     * H - - - -
     * <p>
     * F is the footman
     * H is the townhall
     * x's are occupied spaces
     * <p>
     * xExtent would be 5 for this map with valid X coordinates in the range
     * of [0, 4]
     * x=0 is the left most column and x=4 is the right most column
     * <p>
     * yExtent would be 3 for this map with valid Y coordinates in the range
     * of [0, 2]
     * y=0 is the top most row and y=2 is the bottom most row
     * <p>
     * resourceLocations would be {(0,1), (1,1), (2,1), (4,1)}
     * <p>
     * The path would be
     * <p>
     * (1,0)
     * (2,0)
     * (3,1)
     * (2,2)
     * (1,2)
     * <p>
     * Notice how the initial footman position and the townhall position are
     * not included in the path stack
     *
     * @param start             Starting position of the footman
     * @param goal              MapLocation of the townhall
     * @param xExtent           Width of the map
     * @param yExtent           Height of the map
     * @param resourceLocations Set of positions occupied by resources
     * @return Stack of positions with top of stack being first move in plan
     */
    // TODO Implement A*
    private Stack<Location> AstarSearch(Location start,
                                        Location goal,
                                        int xExtent,
                                        int yExtent,
                                        Location enemyFootmanLoc,
                                        Set<Location> resourceLocations)
    {
        // return an empty path
        return new Stack<Location>();
    }

    /**
     * Primitive actions take a direction (e.g. Direction.NORTH, Direction
     * .NORTHEAST, etc)
     * This converts the difference between the current position and the
     * desired position to a direction.
     *
     * @param xDiff Integer equal to 1, 0 or -1
     * @param yDiff Integer equal to 1, 0 or -1
     * @return A Direction instance (e.g. SOUTHWEST) or null in the case of
     * error
     */
    private Direction getNextDirection(int xDiff, int yDiff)
    {

        // figure out the direction the footman needs to move in
        if (xDiff == 1 && yDiff == 1)
        {
            return Direction.SOUTHEAST;
        } else if (xDiff == 1 && yDiff == 0)
        {
            return Direction.EAST;
        } else if (xDiff == 1 && yDiff == -1)
        {
            return Direction.NORTHEAST;
        } else if (xDiff == 0 && yDiff == 1)
        {
            return Direction.SOUTH;
        } else if (xDiff == 0 && yDiff == -1)
        {
            return Direction.NORTH;
        } else if (xDiff == -1 && yDiff == 1)
        {
            return Direction.SOUTHWEST;
        } else if (xDiff == -1 && yDiff == 0)
        {
            return Direction.WEST;
        } else if (xDiff == -1 && yDiff == -1)
        {
            return Direction.NORTHWEST;
        }

        System.err.println("Invalid path. Could not determine direction");
        return null;
    }

    /**
     * Units corresponding to those available in the configuration file.
     */
    public enum Units
    {
        FOOTMAN("Footman"),
        TOWNHALL("Townhall");

        private final String unit;

        Units(final String unit)
        {
            this.unit = unit;
        }

        public String toString()
        {
            return unit;
        }

        public boolean equals(UnitView unit)
        {
            return unit
                    .getTemplateView()
                    .getName()
                    .equalsIgnoreCase(this.toString());
        }
    }

    public Stack<Location> getPath()
    {
        return path;
    }

    public void setPath(Stack<Location> path)
    {
        this.path = path;
    }

    public UnitView getEnemyFootman(StateView state)
    {
        return state.getUnit(getEnemyFootmanID());
    }

    public UnitView getFootman(StateView state)
    {
        return state.getUnit(getFootmanID());
    }

    public int getFootmanID()
    {
        return footmanID;
    }

    public void setFootmanID(int footmanID)
    {
        this.footmanID = footmanID;
    }

    public UnitView getTownhall(StateView state)
    {
        return state.getUnit(getTownhallID());
    }

    public int getTownhallID()
    {
        return townhallID;
    }

    public void setTownhallID(int townhallID)
    {
        this.townhallID = townhallID;
    }

    public int getEnemyFootmanID()
    {
        return enemyFootmanID;
    }

    public void setEnemyFootmanID(int enemyFootmanID)
    {
        this.enemyFootmanID = enemyFootmanID;
    }

    public Location getNextLoc()
    {
        return nextLoc;
    }

    public void setNextLoc(Location nextLoc)
    {
        this.nextLoc = nextLoc;
    }

    public long getTotalPlanTime()
    {
        return totalPlanTime;
    }

    public long getCurrentPlanTime()
    {
        return currentPlanTime;
    }

    public long getTotalExecutionTime()
    {
        return totalExecutionTime;
    }

    public void updateTotalPlanTime(long amountToUpdate)
    {
        setTotalPlanTime(getTotalPlanTime() + amountToUpdate);
    }

    public void setTotalPlanTime(long totalPlanTime)
    {
        this.totalPlanTime = totalPlanTime;
    }

    public void resetCurrentPlanTime()
    {
        setCurrentPlanTime(0);
    }

    public void setCurrentPlanTime(long currentPlanTime)
    {
        this.currentPlanTime = currentPlanTime;
    }

    public void updateTotalExecutionTime(long amountToUpdate)
    {
        setTotalExecutionTime(getTotalExecutionTime() + amountToUpdate);
    }

    public void setTotalExecutionTime(long totalExecutionTime)
    {
        this.totalExecutionTime = totalExecutionTime;
    }
}
