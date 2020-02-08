package action;

import edu.cwru.sepia.action.Action;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Wrapper to store instances of SEPIA Actions. An ActionMap maintains the
 * {@code HashMap<Integer, Action>} underlying data structure that SEPIA uses
 * to access Actions. The advantages of an {@link ActionMap} include being able
 * to group multiple instances of the same type of Action to multiple units and
 * creating/assigning Actions in a more syntactically understandable manner
 * (i.e., with the {@link #assign(Action...)} or{@link #assign(ActionMap...)}
 * methods).
 *
 * @author Ryan Tatton
 */
public class ActionMap
{
    private HashMap<Integer, Action> actionMap;

    private ActionMap(ActionMap map)
    {
        this.actionMap = new HashMap<>(map.getMap());
    }

    private ActionMap()
    {
        this.actionMap = new HashMap<>();
    }

    /**
     * Factory method for instantiating an empty ActionMap.
     *
     * @return A new, empty ActionMap.
     */
    public static ActionMap createActionsMap()
    {
        return new ActionMap();
    }

    /**
     * Safely copies the contents of one ActionMap into a new ActionMap. Used
     * in the sorting methods defined in {@link ActionGroup}
     * {@code sortDescending()} and {@code sortAscending()}.
     *
     * @return New copy of an ActionMap, with its contents.
     */
    public ActionMap copy()
    {
        return new ActionMap(this);
    }

    /**
     * Adds one or more {@link Action}s, to {@link #actionMap}.
     *
     * @param actions One or more {@link Action}s to assign to the unit that
     *                is specified.
     */
    public void assign(Action... actions)
    {
        Arrays
                .stream(actions)
                .forEach(action -> getMap().put(action.getUnitId(), action));
    }

    /**
     * Adds all {@link Action}s of one or more {@link ActionMap}s to this
     * instance's {@link #actionMap}.
     *
     * @param newActions One or more {{@link #actionMap}}s to add.
     */
    public void assign(ActionMap... newActions)
    {
        Arrays
                .stream(newActions)
                .forEach(actions -> getMap().putAll(actions.getMap()));
    }

    public int size()
    {
        return getMap().size();
    }

    public boolean isEffective()
    {
        return getMap().size() > 0;
    }

    public List<Action> getActions()
    {
        return new ArrayList<>(getMap().values());
    }

    public List<Integer> getAssignedUnitIds()
    {
        return new ArrayList<>(getMap().keySet());
    }

    public HashMap<Integer, Action> getMap()
    {
        return actionMap;
    }

    public String toString()
    {
        return "ActionsMap{" + "size=" + getMap().size() + "}";
    }
}