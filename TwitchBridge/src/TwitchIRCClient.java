package data.scripts.twitch;

import com.fs.starfarer.api.Global;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Anonymous, read-only IRC connection to a Twitch channel.
 *
 * Uses the justinfan trick: NICK justinfan<random> with any password is
 * accepted by Twitch for anonymous read-only access with no OAuth required.
 *
 * Viewer list is maintained by:
 *   1. Parsing the 353 NAMES reply sent automatically on JOIN
 *   2. Adding users when JOIN messages arrive
 *   3. Removing users when PART messages arrive
 *
 * fetchNames() returns the current cached list — since NAMES is not supported
 *
 * IMPORTANT: connect() must be called from a trusted mod plugin context
 * (e.g. onGameLoad). Starsector's security manager blocks writing I/O on
 * threads it considers scripts. The socket and streams are opened on the
 * calling thread, then handed off to a background thread for reading.
 */
public class TwitchIRCClient {

    private static final String IRC_HOST = "irc.chat.twitch.tv";
    private static final int    IRC_PORT = 6667;

    private final String       channel;
    private final List<String> blacklist;
    private String             selfNick;

    private final AtomicBoolean running   = new AtomicBoolean(false);
    private final AtomicBoolean connected = new AtomicBoolean(false);

    private Thread         ircThread;
    private PrintWriter    writer;
    private BufferedReader reader;
    private Socket         socket;

    // Viewer list — maintained by 353/JOIN/PART, read by fetchNames()
    private final Set<String> viewers = new LinkedHashSet<String>();
    private final Object      viewerLock = new Object();

    public TwitchIRCClient(String channel, List<String> blacklist) {
        this.channel   = channel.toLowerCase().replaceFirst("^#", "");
        this.blacklist = new ArrayList<String>();
        for (String b : blacklist) {
            this.blacklist.add(b.toLowerCase());
        }
    }

    // -------------------------------------------------------------------------
    // Lifecycle — call from trusted thread only
    // -------------------------------------------------------------------------

    public synchronized void connect() throws IOException {
        if (running.get()) return;

        log("Opening socket...");
        socket = new Socket(IRC_HOST, IRC_PORT);

        InputStream  is = socket.getInputStream();
        OutputStream os = socket.getOutputStream();
        reader = new BufferedReader(new InputStreamReader(is));
        writer = new PrintWriter(os, true);
        log("Streams opened. Sending handshake...");

        selfNick = "justinfan" + (10000 + new Random().nextInt(89999));
        writer.println("PASS blah");
        writer.println("NICK " + selfNick);
        writer.println("CAP REQ :twitch.tv/membership");
        writer.println("JOIN #" + channel);

        // Read initial server responses on this thread until we see the 366
        // (end of NAMES) that Twitch sends automatically after JOIN.
        // This seeds the viewer list before handing off to the read thread.
        log("Waiting for join confirmation...");
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            if (reader.ready()) {
                String line = reader.readLine();
                log("INIT: " + line);
                handleLine(line);
                if (line.contains(" 366 ")) {
                    // End of initial NAMES list — we're fully joined
                    break;
                }
            }
        }

        connected.set(true);
        running.set(true);
        log("Connected to #" + channel + " as " + selfNick
                + ". Initial viewers: " + viewers.size());

        ircThread = new Thread(new Runnable() {
            public void run() { readLoop(); }
        }, "TwitchBridge-IRC");
        ircThread.setDaemon(true);
        ircThread.start();
    }

    public synchronized void disconnect() {
        running.set(false);
        connected.set(false);
        if (ircThread != null) ircThread.interrupt();
        PrintWriter w = writer;
        if (w != null) {
            w.println("QUIT");
            w.flush();
        }
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            // ignore on shutdown
        }
        log("Disconnected.");
    }

    // -------------------------------------------------------------------------
    // Read loop — background thread, only reads
    // -------------------------------------------------------------------------

    private void readLoop() {
        try {
            String line;
            while (running.get() && (line = reader.readLine()) != null) {
                handleLine(line);
            }
        } catch (IOException e) {
            if (running.get()) {
                log("Read loop ended: " + e.getMessage());
            }
        } finally {
            connected.set(false);
        }
    }

    // -------------------------------------------------------------------------
    // Line handler — called from both init loop and read loop
    // -------------------------------------------------------------------------

    private void handleLine(String line) {
        // Keepalive
        if (line.startsWith("PING")) {
            writer.println("PONG " + line.substring(5));
            return;
        }

        // 353 — NAMES chunk: ":server 353 nick = #channel :user1 user2 ..."
        if (line.contains(" 353 ")) {
            int colonIdx = line.lastIndexOf(':');
            if (colonIdx != -1) {
                String[] names = line.substring(colonIdx + 1).trim().split(" ");
                synchronized (viewerLock) {
                    for (String name : names) {
                        String n = name.toLowerCase().trim();
                        if (!n.isEmpty()) viewers.add(n);
                    }
                }
            }
            return;
        }

        // JOIN — user entered the channel
        // Format: ":nick!nick@nick.tmi.twitch.tv JOIN #channel"
        if (line.contains(" JOIN #")) {
            String nick = parseNick(line);
            if (nick != null) {
                synchronized (viewerLock) {
                    viewers.add(nick);
                }
            }
            return;
        }

        // PART — user left the channel
        // Format: ":nick!nick@nick.tmi.twitch.tv PART #channel"
        if (line.contains(" PART #")) {
            String nick = parseNick(line);
            if (nick != null) {
                synchronized (viewerLock) {
                    viewers.remove(nick);
                }
            }
            return;
        }

        // RECONNECT — server wants us to reconnect
        if (line.contains("RECONNECT")) {
            log("Server requested reconnect.");
            connected.set(false);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Parses the nick from a line like:
     * ":nick!nick@nick.tmi.twitch.tv JOIN #channel"
     */
    private String parseNick(String line) {
        if (!line.startsWith(":")) return null;
        int bangIdx = line.indexOf('!');
        if (bangIdx == -1) return null;
        return line.substring(1, bangIdx).toLowerCase();
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns the current cached viewer list, filtered through the blacklist
     * and excluding the bot's own nick. Thread-safe.
     */
    public List<String> fetchNames() {
        synchronized (viewerLock) {
            List<String> result = new ArrayList<String>();
            for (String name : viewers) {
                if (!blacklist.contains(name) && !name.equals(selfNick)) {
                    result.add(name);
                }
            }
            return Collections.unmodifiableList(result);
        }
    }

    public boolean isConnected() { return connected.get(); }
    public String  getChannel()  { return channel; }

    private void log(String msg) {
        Global.getLogger(TwitchIRCClient.class).info("[TwitchBridge] " + msg);
    }
}
