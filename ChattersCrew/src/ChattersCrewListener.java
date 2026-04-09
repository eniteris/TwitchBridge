package data.scripts;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import data.scripts.twitch.TwitchBridgeAPI;
import lunalib.lunaSettings.LunaSettings;

import java.awt.Color;
import java.util.List;
import java.util.ArrayList;

class ListUtils {
    public static <T> List<T> getDifference(List<T> listA, List<T> listB) {
        if (listA == null || listB == null) {
            return new ArrayList<>();
        }

        List<T> result = new ArrayList<>();
        for (T item : listA) {
            if (!listB.contains(item)) {
                result.add(item);
            }
        }
        return result;
    }
}

public class ChattersCrewListener extends BaseCampaignEventListener {
    private int lastViewerCount = -1; // -1 means no previous count yet
    private List<String> lastViewerList;
    int currentCount = -1;
    private Boolean showViewers = LunaSettings.getBoolean("chattersCrew", "chattersCrew_showViewerCount");
    private Boolean showNames = LunaSettings.getBoolean("chattersCrew", "chattersCrew_showNames");


    public ChattersCrewListener(boolean perma) {
        super(perma);
    }

    @Override
    public void reportPlayerOpenedMarket(MarketAPI market) {
		if (market.getSize() < 3) return;
		
        Global.getSector().addTransientScript(new EveryFrameScript() {
            private boolean done = false;

            @Override
            public boolean isDone() { return done; }

            @Override
            public boolean runWhilePaused() { return true; }

            @Override
            public void advance(float amount) {
                InteractionDialogAPI dialog = Global.getSector()
                        .getCampaignUI()
                        .getCurrentInteractionDialog();
                if (dialog == null) { done = true; return; }

                if (!Global.getSettings().getModManager().isModEnabled("twitchBridge")) {
                    dialog.getTextPanel().addPara("TwitchBridge not installed.", new Color(255, 100, 100));
                    done = true;
                    return;
                }

                TwitchBridgeAPI api = TwitchBridgeAPI.getInstance();
                if (api == null || !api.isConnected()) {
                    dialog.getTextPanel().addPara("Twitch not connected.", new Color(255, 100, 100));
                    done = true;
                    return;
                }

    			List<String> currentViewerList = api.getViewerNames();
                int currentCount = currentViewerList.size();

                if (lastViewerCount == -1) {
                    // First dock — just record, no crew change
                    lastViewerList = currentViewerList;
                    lastViewerCount = currentCount;
					if(showViewers) {
						dialog.getTextPanel().setFontSmallInsignia();
						dialog.getTextPanel().addPara(
								"Twitch viewers: " + currentCount + " (initial dock)",
								new Color(100, 220, 255));
						dialog.getTextPanel().setFontInsignia();
					}
						done = true;
                    return;
                }

				
                int diff = currentCount - lastViewerCount;
    			List<String> joiners = ListUtils.getDifference(currentViewerList,lastViewerList);
    			List<String> leavers = ListUtils.getDifference(lastViewerList,currentViewerList);
                lastViewerCount = currentCount;

                if (diff == 0) {
					if(showViewers) {
						dialog.getTextPanel().setFontSmallInsignia();
						dialog.getTextPanel().addPara(
								"Twitch viewers: " + currentCount + " (no change)",
								new Color(100, 220, 255));
						dialog.getTextPanel().setFontInsignia();
						done = true;
					}
                    return;
                }

                // Clamp crew removal so we don't go below 0
                CargoAPI cargo = Global.getSector().getPlayerFleet().getCargo();
                int currentCrew = (int) cargo.getCrew();
                int crewChange = diff;
                if (crewChange < 0 && currentCrew + crewChange < 0) {
                    crewChange = -currentCrew;
                }

				if(crewChange > 0) {
	                cargo.addCrew(crewChange);
				} else {
	                cargo.removeCrew(-crewChange);
				}

                String sign = crewChange > 0 ? "+" : "";
                Color color = crewChange > 0 ? new Color(50, 220, 50) : new Color(220, 50, 50);
                String noun = Math.abs(crewChange) == 1 ? " crew member has" : " crew members have";
                String verb = crewChange > 0 ? " joined" : " left";
                dialog.getTextPanel().addPara(
                        Math.abs(crewChange) + noun + verb + ".",
                        color);
				if(showViewers) {
					dialog.getTextPanel().setFontSmallInsignia();
					dialog.getTextPanel().addPara(
							"Twitch viewers: " + currentCount, new Color(100,220,255));
					dialog.getTextPanel().setFontInsignia();
				}
				if(showNames) {
					dialog.getTextPanel().setFontSmallInsignia();
					for (String item : joiners) {
						dialog.getTextPanel().addPara(
								item + " has joined.", new Color(50,220,50));
					}
					for (String item : leavers) {
						dialog.getTextPanel().addPara(
								item + " has left.", new Color(220,50,50));
					}
					dialog.getTextPanel().setFontInsignia();
				}

                done = true;
            }
        });
    }
}
