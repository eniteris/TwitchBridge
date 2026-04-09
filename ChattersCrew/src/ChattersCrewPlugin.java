package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;

/**
 * Entry point for the Chatters Crew mod.
 *
 * Registered in mod_info.json under "modPlugin".
 * Adds the ChattersCrewListener to the sector on every game load so that
 * docking events are captured whether the player starts a new game or
 * continues an existing save.
 */
public class ChattersCrewPlugin extends BaseModPlugin {

    @Override
    public void onGameLoad(boolean newGame) {
        // Register our listener.
        // 'false' = not permanent; we re-add it here every load instead.
        Global.getSector().addTransientListener(new ChattersCrewListener(false));
    }

    @Override
    public void onNewGame() {
        // Also hook in for brand-new campaigns (onGameLoad is NOT called for these)
        Global.getSector().addTransientListener(new ChattersCrewListener(false));
    }
}
