package data.scripts.twitch;

import java.util.List;

/**
 * Public API for the Twitch Bridge mod.
 *
 * OTHER MODS: guard calls behind an isModEnabled check:
 *
 *   if (Global.getSettings().getModManager().isModEnabled("twitchBridge")) {
 *       TwitchBridgeAPI api = TwitchBridgeAPI.getInstance();
 *       if (api != null && api.isConnected()) { ... }
 *   }
 *
 * All methods are thread-safe.
 */
public class TwitchBridgeAPI {

    private static TwitchBridgeAPI instance;

    static void init(TwitchIRCClient client) {
        instance = new TwitchBridgeAPI(client);
    }

    static void shutdown() {
        if (instance != null) {
            instance.client.disconnect();
            instance = null;
        }
    }

    public static TwitchBridgeAPI getInstance() {
        return instance;
    }

    private final TwitchIRCClient client;

    private TwitchBridgeAPI(TwitchIRCClient client) {
        this.client = client;
    }

    /**
     * Returns the count of users in the channel, excluding anyone on the blacklist.
     * The count is a running count of joins and leaves, after the initial JOIN NAMES
     * Note: Twitch only sends NAMES reliably for channels under ~500 users.
     * For larger channels the list will not populate.
     */
    public int getViewerCount() {
        return client.fetchNames().size();
    }

    /**
     * Same as getViewerCount() but returns the actual list of usernames,
     * already filtered through the blacklist.
     */
    public List<String> getViewerNames() {
        return client.fetchNames();
    }

/*TODO: viewing, adding, removing names from the blacklist
 * getViewerNames without blacklist
 * */

    /**
     * Returns true if the IRC connection is currently established.
     */
    public boolean isConnected() {
        return client.isConnected();
    }

    /**
     * Returns the channel this client is connected to (without #).
     */
    public String getChannel() {
        return client.getChannel();
    }
}
