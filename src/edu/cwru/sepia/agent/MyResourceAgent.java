package edu.cwru.sepia.agent;

import action.ActionPlan;
import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.environment.model.history.History.HistoryView;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import playerView.PlayerView;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;


public class MyResourceAgent extends Agent
{
    private PlayerView player;

    public MyResourceAgent(int playerNum)
    {
        super(playerNum);
        this.player = PlayerView.createPlayer(playerNum);
    }

    @Override
    public Map<Integer, Action> initialStep(StateView state,
                                            HistoryView history)
    {
        return middleStep(state, history);
    }

    @Override
    public Map<Integer, Action> middleStep(StateView state,
                                           HistoryView history)
    {
        getPlayer().setStateAndHistory(state, history);

        List<UnitView> availableUnits = getPlayer().getUnoccupiedUnits();
        ActionPlan actionPlan = ActionPlan.createActionPlan();

        actionPlan.scheduleDescendingSize(
                getPlayer().buildPeasant(availableUnits),
                getPlayer().buildFarm(availableUnits),
                getPlayer().buildBarracks(availableUnits),
                getPlayer().buildFootman(availableUnits),
                getPlayer().gatherBalancedResources(availableUnits),
                getPlayer().depositResources(availableUnits)
        );

        actionPlan.addUnitsToSet(availableUnits);
        actionPlan.createPlan();

        return actionPlan.getMap();
    }

    @Override
    public void terminalStep(StateView state, HistoryView history)
    {
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
