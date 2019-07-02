// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package annamod;

import net.basdon.anna.api.*;
import net.basdon.anna.api.IAnna.Output;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.*;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static net.basdon.anna.api.Util.*;

public
class Mod implements IMod
{
private static final Properties defaultconf;

static
{
	defaultconf = new Properties();
	defaultconf.setProperty("timezone", "");
}

private final HashMap<char[], ChannelLogger> loggers = new HashMap<>();

private Calendar calendar;

IAnna anna;

@Override
public
String getName()
{
	return "mod_log";
}

@Override
public
String getVersion()
{
	return "0";
}

@Override
public
String getDescription()
{
	return "logs chats";
}

@Override
public
void print_stats(Output output)
throws IOException
{
}

@Override
public
boolean on_enable(IAnna anna)
{
	this.anna = anna;
	anna.load_mod_conf(this, null);
	return true;
}

@Override
public
void config_loaded(Config conf)
{
	String timezone = conf.getStr("timezone");
	TimeZone tz = TimeZone.getDefault();
	if (timezone != null && !(timezone = timezone.trim()).isEmpty()) {
		tz = TimeZone.getTimeZone(timezone);
	}
	this.calendar = Calendar.getInstance(tz);

	long timestamp = System.currentTimeMillis();
	Set<char[]> now_disabled_channels = this.loggers.keySet();
	for (Map.Entry<Object, Object> e : conf.props.entrySet()) {
		Object key = e.getKey();
		Object val = e.getValue();
		if (key instanceof String && val instanceof String) {
			String k = (String) key;
			if (k.startsWith("channel.") && k.length() > 8) {
				String channel = "#" + k.substring(8);
				char[] chan = channel.toCharArray();
				ChannelLogger cl = this.loggers.get(chan);
				if (cl == null) {
					File dir = new File((String) val);
					if (!dir.exists()) {
						dir.mkdirs();
					}
					if (!dir.isDirectory()) {
						this.anna.log_warn(
							"mod_log: invalid path for " + channel
						);
						continue;
					}
					cl = new ChannelLogger();
					cl.mod = this;
					cl.timestamp = timestamp;
					cl.channel = channel;
					cl.chan = chan;
					cl.directory = dir;
					this.loggers.put(chan, cl);
				} else {
					now_disabled_channels.remove(chan);
				}
			}
		}
	}

	for (char[] chan : now_disabled_channels) {
		ChannelLogger cl = this.loggers.get(chan);
		if (cl != null) {
			cl.close_stream();
			this.loggers.remove(chan);
		}
	}
}

@Override
public
void on_message(User user, char[] target, char[] replytarget, char[] message)
{
}

@Override
public
boolean on_command(User user, char[] target, char[] replytarget, char[] cmd, char[] params)
{
	return false;
}

@Override
public
void on_action(User user, char[] target, char[] replytarget, char[] action)
{
}

@Override
public
void on_selfmessage(char[] target, char[] text, int offset, int len)
{
}

@Override
public
void on_selfaction(char[] target, char[] text)
{
}

@Override
public
void on_topic(User user, char[] channel, char[] topic)
{
}

@Override
public
void on_channelmodechange(Channel chan, int changec, char[] signs, char[] modes,
                          char[] types, char[][] params, ChannelUser[] users)
{
}

@Override
public
void on_nickchange(User user, char[] newnick)
{
}

@Override
public
void on_kick(User user, char[] channel, char[] kickeduser, char[] msg)
{
}

@Override
public
void on_part(User user, char[] channel, char[] msg)
{
}

@Override
public
void on_quit(User user, char[] msg)
{
}

@Override
public
void on_join(User user, char[] channel)
{
}

@Override
public
void on_disable()
{
	for (ChannelLogger cl : this.loggers.values()) {
		cl.close_stream();
	}
}

private
OutputStreamWriter logger(char[] chan)
{
	ChannelLogger cl;
	if ((cl = this.loggers.get(chan)) != null) {
		return cl.get_or_open_stream();
	}
	return null;
}

Calendar calendar()
{
	this.calendar.setTimeInMillis(System.currentTimeMillis());
	return this.calendar;
}

Date time()
{
	return this.calendar().getTime();
}
} /*Mod*/

class ChannelLogger
{
Mod mod;
long timestamp;
int lastday;
char[] chan;
String channel;
File directory;
OutputStreamWriter writer;

OutputStreamWriter get_or_open_stream()
{
	int day = mod.calendar().get(Calendar.DAY_OF_YEAR);
	if (this.writer == null || this.lastday != day) {
		if (this.writer != null) {
			close(this.writer);
			this.writer = null;
		}
		this.lastday = day;
		String filename;
		filename = format("%s-%tY-%<tm-%<td.html", this.channel, mod.time());
		File of = new File(this.directory, filename);
		if (of.isDirectory()) {
			return null;
		}
		try {
			this.writer = new OutputStreamWriter(new FileOutputStream(of), UTF_8);
			String msg;
			msg = format("<em>*** session open: %tH:%<tM:%<tS</em><br/>", mod.time());
			this.writer.write(msg);
		} catch (IOException e) {
			mod.anna.log_warn(e, "cannot create log file for " + channel);
		}
	}
	return this.writer;
}

void close_stream()
{
	if (this.writer != null) {
		try {
			String msg;
			msg = format("<em>*** session close: %tH:%<tM:%<tS</em><br/>", mod.time());
			this.writer.write(msg);
		} catch (IOException e) {
			mod.anna.log_warn(e, "cannot add closing note for " + channel);
		}
		close(this.writer);
	}
}
} /*ChannelLogger*/
