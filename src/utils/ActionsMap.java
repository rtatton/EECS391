package utils;

import edu.cwru.sepia.action.Action;

import java.util.Collection;
import java.util.HashMap;

/**
 * Wrapper to store instances of edu.cwru.sepia.action.Action. Simplifies the mechanism of assigning actions to entities
 * in SEPIA.
 *
 * @author Ryan Tatton
 */
public class ActionsMap
{
    private final int playerNum;
    private HashMap<Integer, Action> actions = new HashMap<>();

    /**
     * @param playerNum Player integer Id.
     */
    public ActionsMap(int playerNum)
    {
        this.playerNum = playerNum;
    }

    /**
     * Given an {@link Action} specifying the unit and the command, add the {@link Action} to the HashMap of actions to
     * be executed.
     * @param action Action to assign to the unit that is specified.
     */
    public void assign(Action action)
    {
        int assignedTo = action.getUnitId();
        actions.put(assignedTo, action);
    }

    public HashMap<Integer, Action> getMap()
    {
        return actions;
    }

    public Collection<Action> getActions()
    {
        return actions.values();
    }

    public java.util.Set<Integer> getAssignedUnits()
    {
        return actions.keySet();
    }

    public int getPlayerNum()
    {
        return playerNum;
    }
}
