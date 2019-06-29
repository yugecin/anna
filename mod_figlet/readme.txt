mod_figlet
----------
Based on FIGlet, a program that creates large characters out of ordinary
screen characters.

<@robin_be> &figlet Hello, world!
<Anna^>  _   _      _ _                               _     _ _
<Anna^> | | | | ___| | | ___      __      _____  _ __| | __| | |
<Anna^> | |_| |/ _ \ | |/ _ \     \ \ /\ / / _ \| '__| |/ _` | |
<Anna^> |  _  |  __/ | | (_) |     \ V  V / (_) | |  | | (_| |_|
<Anna^> |_| |_|\___|_|_|\___( )     \_/\_/ \___/|_|  |_|\__,_(_)
<Anna^>                     |/

commands
--------
&figlet <string>
&figletgay <string>

The figletgay command does the same, but adds rainbow-y colors, much
like TOIlet --gay does.

configuration
-------------
command.delay         -- (ms) how long users must wait between performing
                         figlet commands (count starts after all lines
                         have been sent) (10000)
undelayed.line.amount -- amount of lines that will be sent without delay (3)
line.delay            -- (ms) delay between lines after
                         undelayed.line.amount of lines has been send (450)
noflood.message       -- message that will be sent when someone invokes a
                         figlet command while the delay is still active,
                         leave empty for no message (don't make me flood)

As with all Anna^ mods, config file is in the config folder after its
first run.

---
figletfont.txt is made using the 'Standard' FIGlet font
by Glenn Chappell & Ian Chai 3/93 -- based on .sig of Frank Sheeran