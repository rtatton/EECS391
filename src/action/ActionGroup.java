package action;

import utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Organizer for multiple {@link ActionMap}s. Each group is able to be
 * selected from either at random, sequentially, in ascending order (by
 * length) or in descending order (by length). One or more {@link ActionMap}s
 * are scheduled together by the {@link ActionPlan}, which determines the
 * groupings of Actions to be executed.
 *
 * @author Ryan Tatton
 */
public class ActionGroup
{
    private List<ActionMap> actionGroup;
    private SelectionType selectionType;

    private ActionGroup(SelectionType selectionType,
                        List<ActionMap> actionGroup)
    {
        this.actionGroup = actionGroup;
        this.selectionType = selectionType;
    }

    private ActionGroup(SelectionType selectionType)
    {
        this(
                selectionType,
                new ArrayList<>()
        );
    }

    private ActionGroup(ActionMap... actionMaps)
    {
        this(
                SelectionType.SEQUENTIAL,
                new ArrayList<>(Arrays.asList(actionMaps))
        );
    }

    private ActionGroup(SelectionType selectionType,
                        ActionMap... actionMaps)
    {
        this(
                selectionType,
                new ArrayList<>(Arrays.asList(actionMaps))
        );
    }

    private ActionGroup(ActionGroup group)
    {
        this.actionGroup = new ArrayList<>(group.getActionGroup());
        this.selectionType = group.getSelectionType();
    }

    private ActionGroup()
    {
        this(
                SelectionType.SEQUENTIAL,
                new ArrayList<>()
        );
    }

    public static ActionGroup createActionGroup(SelectionType selectionType,
                                                List<ActionMap> actionGroup)
    {
        return new ActionGroup(selectionType, actionGroup);
    }

    public static ActionGroup createActionGroup(SelectionType selectionType)
    {
        return new ActionGroup(selectionType);
    }

    public static ActionGroup createActionGroup(ActionMap... actionMaps)
    {
        return new ActionGroup(actionMaps);
    }

    public static ActionGroup createActionGroup(SelectionType selectionType,
                                                ActionMap... actionMaps)
    {
        return new ActionGroup(selectionType, actionMaps);
    }

    public static ActionGroup createActionGroup(ActionGroup group)
    {
        return new ActionGroup(group);
    }

    public static ActionGroup createActionGroup()
    {
        return new ActionGroup();
    }


    private void addToGroup(ActionMap... actionMaps)
    {
        getActionGroup().addAll(Arrays.asList(actionMaps));
    }

    private ActionGroup copy()
    {
        return new ActionGroup(this);
    }

    public List<ActionMap> select()
    {
        switch (getSelectionType())
        {
            case RANDOM:
                return selectRandom();
            case ASCENDING_SIZE:
                return selectAscendingSize();
            case DESCENDING_SIZE:
                return selectDescendingSize();
            default:
                return selectSequential();
        }
    }

    private List<ActionMap> selectSequential()
    {
        return getActionGroup();
    }

    private List<ActionMap> selectRandom()
    {
        ArrayList<ActionMap> random = new ArrayList<>();

        if (getActionGroup().size() != 0)
        {
            int randomInt = Utils.randomInt(0, getActionGroup().size() - 1);

            random.add(getActionGroup().get(randomInt));
        }

        return random;
    }

    private List<ActionMap> selectAscendingSize()
    {
        return copy().sortAscending();
    }

    private List<ActionMap> selectDescendingSize()
    {
        return copy().sortDescending();
    }

    private List<ActionMap> sortDescending()
    {
        return getActionGroup()
                .stream()
                .sorted(Comparator.comparing(ActionMap::size))
                .collect(Collectors.toList());
    }

    private List<ActionMap> sortAscending()
    {
        return getActionGroup()
                .stream()
                .sorted(Comparator.comparing(ActionMap::size).reversed())
                .collect(Collectors.toList());
    }

    public List<ActionMap> getActionGroup()
    {
        return actionGroup;
    }

    public SelectionType getSelectionType()
    {
        return selectionType;
    }

    public String toString()
    {
        int num = getActionGroup().size();
        String type = getSelectionType().toString();
        return "ActionGroup{" + "size=" + num + ", selectionType=" + type + "}";
    }

    public enum SelectionType
    {
        ASCENDING_SIZE("Descending Size"),
        DESCENDING_SIZE("Descending Size"),
        RANDOM("Random"),
        SEQUENTIAL("Sequential");

        private final String selection;

        SelectionType(final String selection)
        {
            this.selection = selection;
        }

        public String toString()
        {
            return selection;
        }
    }
}
