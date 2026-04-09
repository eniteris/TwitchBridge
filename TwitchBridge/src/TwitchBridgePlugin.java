package data.scripts.twitch;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import lunalib.lunaSettings.LunaSettings;

/**
 * Entry point for Twitch Bridge.
 * Reads data/config/twitchbridge_settings.json and starts the IRC client.
 */
public class TwitchBridgePlugin extends BaseModPlugin {

    private static final String SETTINGS_PATH = "./twitchbridge_settings.json";

    private TwitchIRCClient client;

    private void startClient() {
        stopClient();

        try {
			String channel = LunaSettings.getString("twitchBridge", "twitchBridge_channel");
			if (channel == null || channel.trim().isEmpty() || channel.equals("your_channel_name")) {
				log("Channel not configured — not connecting.");
				return;
			}

			String blacklistRaw = LunaSettings.getString("twitchBridge", "twitchBridge_blacklist");
			List<String> blacklist = new ArrayList<String>();
			if (blacklistRaw != null && !blacklistRaw.trim().isEmpty()) {
				for (String b : blacklistRaw.split(",")) {
					blacklist.add(b.trim().toLowerCase());
				}
			}

            client = new TwitchIRCClient(channel, blacklist);
            TwitchBridgeAPI.init(client);
            client.connect();

        } catch (Exception e) {
            log("Failed to load settings: " + e.getMessage());
        }
    }

    private void stopClient() {
        TwitchBridgeAPI.shutdown();
        client = null;
    }

    @Override
    public void onGameLoad(boolean newGame) {
        startClient();
    }

    @Override
    public void onNewGame() {
        startClient();
    }

    private void log(String msg) {
        Global.getLogger(TwitchBridgePlugin.class).info("[TwitchBridge] " + msg);
    }
}
