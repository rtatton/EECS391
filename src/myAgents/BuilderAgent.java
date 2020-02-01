package myAgents;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History.HistoryView;
import edu.cwru.sepia.environment.model.state.ResourceNode.ResourceView;
import edu.cwru.sepia.environment.model.state.ResourceNode.Type;
import edu.cwru.sepia.environment.model.state.ResourceType;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import utils.ActionsMap;
import utils.PlayerState;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;


public class BuilderAgent extends Agent
{
    public BuilderAgent(int playerNum)
    {
        super(playerNum);
        System.out.println("Time to build!");
    }

    // No initialization required.
    @Override
    public Map<Integer, Action> initialStep(StateView state, HistoryView history)
    {
        return middleStep(state, history);
    }

    @Override
    public Map<Integer, Action> middleStep(StateView state, HistoryView history)
    {
        PlayerState player = new PlayerState(playernum, state, history);

        //Stores action each unit performs.
        //No changes to current actions -> empty map.
        ActionsMap actions = new ActionsMap(player.getPlayerNum());

        //Filters all units based on the current state and player, and collects all units of the same type into a List.
        UnitView townHall = player.getUnitsByType(PlayerState.Units.TOWNHALL).get(0);
        List<UnitView> peasants = player.getUnitsByType(PlayerState.Units.PEASANT);

        // Current amount of gold and wood in town hall.
        int currGold = player.getResourceAmount(ResourceType.GOLD);
        int currWood = player.getResourceAmount(ResourceType.GOLD);

        // Make more peasants as soon as possible.
        // Gather resources from closest resource nodes that have resources. [eval function done]
        for (UnitView peasant : peasants)
        {
            int peasantId = peasant.getID();

            // For all peasants carrying resources, assign them the action of depositing them at the town hall.
            if (player.isCarryingCargo(peasant))
                actions.assign(Action.createCompoundDeposit(peasantId, townHall.getID()));

                // For all peasants not carrying resources, assign them the action of gathering more wood if the town
                // hall currently contains more gold than wood, and vice versa.
            else if (currGold < currWood)
            {
                ResourceView bestGoldMine = player.findBestResource(peasant, townHall, Type.GOLD_MINE);
                actions.assign(Action.createCompoundGather(peasantId, bestGoldMine.getID()));
            } else
            {
                ResourceView bestTree = player.findBestResource(peasant, townHall, Type.TREE);
                actions.assign(Action.createCompoundGather(peasantId, bestTree.getID()));
            }

        }
        // Build 2 new peasants, given sufficient resources and number of existing peasants.
        if (player.canBuildMorePeasants())
        {
            // Tells SEPIA what type of unit to build.
            int peasantTemplateId = player.getTemplate(PlayerState.Units.PEASANT).getID();
            // Tells town hall to build unit with peasant template ID.
            actions.assign(Action.createCompoundProduction(townHall.getID(), peasantTemplateId));
        }
        return actions.getMap();
    }

    @Override
    public void terminalStep(StateView state, HistoryView history)
    {
        System.out.println("All in a day's work!");
    }

    @Override
    public void loadPlayerData(InputStream inputStream)
    {
    }

    @Override
    public void savePlayerData(OutputStream outputStream)
    {
    }
}
