package edu.cwru.sepia.agent;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.environment.model.history.History.HistoryView;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import utils.ActionsMap;
import utils.PlayerView;
import utils.PlayerView.Units;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class MyResourceAgent extends Agent
{
    private PlayerView player;

    public MyResourceAgent(int playerNum)
    {
        super(playerNum);
        this.player = new PlayerView(playerNum);
        System.out.println("Time to build!");
    }

    @Override
    public Map<Integer, Action> initialStep(StateView state, HistoryView history)
    {
        getPlayer().setState(state);
        getPlayer().setHistory(history);
        return middleStep(state, history);
    }

    @Override
    public Map<Integer, Action> middleStep(StateView state, HistoryView history)
    {
        ActionsMap actionsMap = new ActionsMap(getPlayerNumber());
        UnitView townHall = getPlayer().getUnitsByType(Units.TOWNHALL).get(0);

        // TODO "ActionTree" that partitions remaining, unassigned agents to another Action.
        // TODO Wrapper class: ActionPlan? Allow task ordering, priority, and capping number of times task is done.

        Map<Boolean, List<UnitView>> cargoStatus = getPlayer().getUnoccupiedUnits()
                .stream()
                .collect(Collectors.partitioningBy(getPlayer()::isCarryingCargo));
        List<UnitView> remaining = cargoStatus.get(false);

        ActionsMap depositResources = getPlayer().depositResources(cargoStatus.get(true), townHall);
        UnitView farmBuilder = remaining.remove(remaining.size() - 1);
        ActionsMap buildFarm = getPlayer().buildFarm(farmBuilder);
        ActionsMap buildPeasant = getPlayer().buildPeasant(townHall);
        ActionsMap gatherResources = getPlayer().gatherBalancedResources(remaining, townHall);

        actionsMap.assign(buildPeasant, depositResources, buildFarm, gatherResources);

        return actionsMap.getMap();
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

    public PlayerView getPlayer()
    {
        return player;
    }

    public void setPlayer(PlayerView player)
    {
        this.player = player;
    }

}
