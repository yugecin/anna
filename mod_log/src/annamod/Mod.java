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
import static net.basdon.anna.api.Constants.*;

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
LogWriter logger(char[] chan)
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

LogWriter get_or_open_stream()
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
	if (this.writer == null) {
		return null;
	}
	return new LogWriter(this.writer);
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

class LogWriter
{
private static final String[] COLORS = {
	"000", "FFF", "00007F", "009300", "F00", "7F0000", "9C009C",
	"FC7F00", "FF0", "00FC00", "009393", "0FF", "0000FC", "F0F",
	"7F7F7F", "D2D2D2"
};
private static final int
	STYLE_FG = 0,
	STYLE_BG = 1,
	STYLE_BOLD = 2,
	STYLE_UNDERLINE = 3,
	STYLE_ITALIC = 4,
	STYLE_STRIKETHROUGH = 5,
	STYLE_REVERSE = 6;

final OutputStreamWriter writer;

boolean has_span;
String[] styles = new String[7];

LogWriter(OutputStreamWriter writer)
{
	this.writer = writer;
}

void bold()
throws IOException
{
	if (this.styles[STYLE_BOLD] != null) {
		this.styles[STYLE_BOLD] = null;
	} else {
		this.styles[STYLE_BOLD] = "font-weight:bold";
	}
	this.writer.write("<wbr data-ctrl=\"2");
	this.add_formatting();
}

void color(int fg)
throws IOException
{
	if (this.has_span) {
		this.writer.write("</span>");
	}
	if (fg < 0 || 15 < fg) {
		fg = COL_BLACK;
	}
	this.writer.write("<wbr data-ctrl=\"3");
	this.writer.write(COLORMAP[fg]);
	this.writer.write("\">");
	this.styles[STYLE_FG] = "text-color:#" + COLORS[fg] + ";";
	this.add_formatting();
}

void color(int fg, int bg)
throws IOException
{
	if (fg < 0 || 15 < fg) {
		fg = COL_BLACK;
	}
	if (bg < 0 || 15 < bg) {
		this.styles[STYLE_BG] = null;
		this.color(fg);
		return;
	}
	this.writer.write("<wbr data-ctrl=\"3");
	this.writer.write(COLORMAP[fg]);
	this.writer.write(',');
	this.writer.write(COLORMAP[bg]);
	this.writer.write("\">");
	this.styles[STYLE_FG] = "text-color:#" + COLORS[fg];
	this.styles[STYLE_BG] = "background:#" + COLORS[bg];
	this.add_formatting();
}

void italic()
throws IOException
{
	if (this.styles[STYLE_ITALIC] != null) {
		this.styles[STYLE_ITALIC] = null;
	} else {
		this.styles[STYLE_ITALIC] = "font-style:italic";
	}
	this.writer.write("<wbr data-ctrl=\"9");
	this.add_formatting();
}

void strikethrough()
throws IOException
{
	if (this.styles[STYLE_STRIKETHROUGH] != null) {
		this.styles[STYLE_STRIKETHROUGH] = null;
	} else {
		this.styles[STYLE_STRIKETHROUGH] = "text-decoration:line-through";
	}
	this.writer.write("<wbr data-ctrl=\"13");
	this.add_formatting();
}

void reset()
throws IOException
{
	for (int i = 0; i < this.styles.length; i++) {
		this.styles[i] = null;
	}
	this.writer.write("<wbr data-ctrl=\"15");
	this.add_formatting();
}

void underline()
throws IOException
{
	if (this.styles[STYLE_UNDERLINE] != null) {
		this.styles[STYLE_UNDERLINE] = null;
	} else {
		this.styles[STYLE_UNDERLINE] = "text-decoration:underline";
	}
	this.writer.write("<wbr data-ctrl=\"21");
	this.add_formatting();
}

void reverse()
throws IOException
{
	if (this.styles[STYLE_REVERSE] != null) {
		this.styles[STYLE_REVERSE] = null;
	} else {
		this.styles[STYLE_REVERSE] = "font-color:#fff;background:#000";
	}
	this.writer.write("<wbr data-ctrl=\"22");
	this.add_formatting();
}

private
void add_formatting()
throws IOException
{
	out:
	{
		for (int i = 0; i < this.styles.length; i++) {
			if (this.styles[i] != null) {
				break out;
			}
		}
		this.has_span = false;
		return;
	}
	if (this.has_span) {
		this.writer.write("</span>");
	}
	this.writer.write("<span style=\"");
	for (int i = 0; i < this.styles.length; i++) {
		if (this.styles[i] != null) {
			switch (i) {
			// combine underline and line-through when needed
			case STYLE_UNDERLINE: if (this.styles[STYLE_STRIKETHROUGH] != null) {
				this.writer.write("text-decoration:line-through underline;");
				continue;
			}
			case STYLE_STRIKETHROUGH: if (this.styles[STYLE_UNDERLINE] != null) {
				continue;
			}
			// prevent fg and bg if reverse is active
			case STYLE_FG: if (this.styles[STYLE_REVERSE] != null) {
				continue;
			}
			case STYLE_BG: if (this.styles[STYLE_REVERSE] != null) {
				continue;
			}
			}
			this.writer.write(this.styles[i]);
			this.writer.write(';');
		}
	}
	this.writer.write("\">");
	this.has_span = true;
}

void lf()
throws IOException
{
	if (this.has_span) {
		this.writer.write("</span>");
		this.has_span = false;
	}
	this.writer.write("<br>");
}

void append_parse_ctrlcodes(char[] text)
throws IOException
{
}
} /*LogWriter*/
