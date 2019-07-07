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
                    want to log. Don't add the #.

output
------
Logs will be outputted to the configured directory, with a filename
#channelname-YYYY-MM-DD.html non alphanum characters will be replaced
with underscores (_).

Logfiles are saved in a HTML-like format and contain only two lines. The first line is a style tag
to make the text monospaced. The second line contains the entries. Entries are separated by a
<br> tag. Entries starting with *** are meta, normal entries start with timestamp formatted as
[HH:MM:SS]. Most non-ASCII characters are escaped using xml numeric character references and &lt;
and &gt;. Formatting codes are preserved with a <wbr> tag having a class data-ctrl containing the
control code followed by parameters (in the case of a color code). Formatting is applied using span
tags.
