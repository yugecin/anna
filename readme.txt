anna (stylized Anna^)
---------------------
Modular IRC mod. Named after Anna^, from the 2006 BASSHUNTER song
named Boten Anna. https://youtu.be/RYQUsp-jxDQ

basics
------
java -jar anna.jar

Config files can be found as properties file in the config folder in
the working directory.

When running for the first time, the default config file will be made
(together with the config folder if necessary) and anna will exit,
prompting you to change the defaults.

mods
----
Modules (mods) should be placed as jar files in the same working
directory, following the name mod_xxx.jar. A mod should have a class
annamod.Mod that implements net.basdon.anna.api.IMod. For an example,
check mod_test or any of the other default mods.

commands
--------
Default command prefix is &

Most of these need owner privileges (see config file).

raw <string>             -- send a raw message
say <target> <string>    -- send a message to a user or channel
action <target> <string> -- send an action message to a user or channel
die                      -- exit
restart                  -- restart (does not actually restart, just
                            reconnects)
channels                 -- print all channels the bot is in
users <channel>          -- print all users in the channel
stats                    -- check status of the stats server
killstats                -- kill the stats server
loadmod mod_<modname>    -- load a mod
unloadmod mod_<modname>  -- unload a mod
reloadmod mod_<modname>  -- reload a mod
enablemod mod_<modname>  -- enables a mod, meaning the mod will be
                            loaded whenever anna starts. If the mod
                            is not loaded now, it will be loaded. To
                            not load the mod now but do add it to the
                            startup, append the mod_modname to the end
                            of the conf setting mods.enabled
disablemod mod_<modname> -- analogue to enablemod
modinfo mod_<modname>    -- show info about a (loaded) mod
conf <modname> <setting> -- view or change a configuration setting,
     [newvalue]             use 'anna' as modname for core settings.
                            Newvalue may can be an empty string, in
                            that case the last character after the
                            setting must be a single space. Changing
                            a setting will immediately save the conf
                            file and notice the mod that settings
                            have been changed.

stats
-----
Anna^ has a built-in stats/info server by means of a very primitive
HTTP server running on port 7755. This can be changed in the config.

---
http://qdb.us/296934
<Indogutsu> Is that that Basshunter song?
<Slime_Master> Yeah
<Indogutsu> ...only in Sweden can a dance rap song about IRC hit #1 on
            the charts.
