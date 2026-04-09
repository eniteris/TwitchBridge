# TwitchBridge

an IRC library that provides access to Twitch viewer information. Only reliable on channels with less than 500 viewers. Requires LunaLib.

Check LunaLib settings to change the channel name.

This library functions by anonymously logging into the Twitch IRC chat for the channel (using the username `justinfan`, which may be depreciated at any time). The library then asks to see who is in the channel, and tracks when users join or leave the channel.

As this tracks viewers based of the IRC, it can only count logged-in users in the chat room, not necessarily all viewers of the stream. The IRC command `NAMES` is blocked in Twitch IRC (other than on initial join), and thus tracking `JOIN` and `PART` is the only way of keeping the viewer list up to date. Note that Twitch IRC often batches the `JOIN`/`PART` messages, and thus the tracked data can be delayed from realtime changes in viewership.

**Future Work**
- Implementing proper oauth to actually log in properly, so the mod can send messages and/or ban users.
- Track and expose chat messages.
- Track used names and subscriber status.

and a demonstration mod

## Chatter's Crew

where, when docking at a market, you gain or lose crew based off your change in Twitch viewership.

<img width="1838" height="1218" alt="image" src="https://github.com/user-attachments/assets/0a049255-a5b4-4840-920d-5fc601f46260" />

The first time you dock at market after you start/load sets the baseline number. Any changes in viewership will result in crew joining or leaving the next time you dock at a market.

LunaLib settings has toggles for showing total Twitch viewers and the names of the joining/leaving viewers.

**Future Work**
- Raids are converted into marines, not crew.
- Track crew lost to combat/salvaging/selling, and give them names from the viewer list.

<sub>This mod was made with help from AI.</sub>

<sub>CC0. All permissions granted to anyone to remix, modify, etc.</sub>
