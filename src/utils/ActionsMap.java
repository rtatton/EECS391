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
     * @param actions One or more {@link Action}s to assign to the unit that is specified.
     */
    public void assign(Action...actions)
    {
        for (Action action : actions)
            getMap().put(action.getUnitId(), action);
    }

    public void assign(ActionsMap...newActions)
    {
        for (ActionsMap actions : newActions)
            getMap().putAll(actions.getMap());
    }

    public void remove(Action...actions)
    {
        for (Action action : actions)
            getMap().remove(action.getUnitId());
    }

    public HashMap<Integer, Action> getMap()
    {
        return actions;
    }

    public Collection<Action> getActions()
    {
        return getMap().values();
    }

    public java.util.Set<Integer> getAssignedUnits()
    {
        return getMap().keySet();
    }

    public int getPlayerNum()
    {
        return playerNum;
    }
}
