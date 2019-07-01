mod_log
-------
Logs chats to html files.

configuration
-------------
timezone         -- timezone for dates and timestamps, in format:
                    America/Los_Angeles. Leave empty for autodetect. If this
                    fails, GMT will be set silently (sorry, blame Java for
                    that). See java.util.TimeZone#getAvailableIDs for valid
                    timezones.
channel.channame -- output path for #channame channel, this should be
                    a directory. If it doesn't exist, a directory will
                    be created. Add one of these for every channel you
                    want to log.

output
------
Logs will be outputted to the configured directory, with a filename
#channelname-YYYY-MM-DD.html non alphanum characters will be replaced
with underscores (_).

Logs are saved in a HTML-like format.

