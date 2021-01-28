// Copyright 2019-2021 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package annamod;

import net.basdon.anna.api.*;
import net.basdon.anna.api.IAnna.Output;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static net.basdon.anna.api.Util.*;
import static net.basdon.anna.api.Constants.*;

public
class Mod implements IModLoader
{
@Override
public
IMod load()
{
	// while pointless in production, using a different class instead of just implementing IMod
	// gives the advantage that the classname is (hopefully) unique, which makes sure that
	// debugging is possible (because there might be many annamod.Mod class instances loaded).
	return new ModLog();
}
}

class ModLog implements IMod
{
private static final Properties defaultconf;

static
{
	defaultconf = new Properties();
	defaultconf.setProperty("timezone", "");
}

private final HashMap<String, ChannelLogger> loggers = new HashMap<>();

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
	return "2";
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
	for (ChannelLogger l : this.loggers.values()) {
		output.print(" ");
		output.print(l.chan, 0, l.chan.length);
		output.print(": lastday: ");
		output.print(String.valueOf(l.lastday));
		output.print(", lines: ");
		output.print(String.valueOf(l.lineswritten));
		output.print(", stream: ");
		if (l.writer != null) {
			output.print("open");
		} else {
			output.print("closed");
		}
		output.print("\n");
	}
}

@Override
public
Properties get_default_conf()
{
	return defaultconf;
}

@Override
public
boolean on_enable(IAnna anna, char[] replytarget)
{
	this.anna = anna;
	anna.load_mod_conf(this);
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

	Long timestamp = new Long(System.currentTimeMillis());
	for (ChannelLogger cl : this.loggers.values()) {
		cl.should_disable = true;
	}
	for (Map.Entry<Object, Object> e : conf.props.entrySet()) {
		Object key = e.getKey();
		Object val = e.getValue();
		if (key instanceof String && val instanceof String) {
			String k = (String) key;
			if (k.startsWith("channel.") && k.length() > 8) {
				String channel = "#" + k.substring(8);
				char[] chan = channel.toCharArray();
				ChannelLogger cl = this.loggers.get(channel);
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
					this.loggers.put(channel, cl);
					cl.get_or_open_stream(); // write 'session open' msg
				} else {
					cl.should_disable = false;
				}
			}
		}
	}

	Iterator<ChannelLogger> chans = this.loggers.values().iterator();
	while (chans.hasNext()) {
		ChannelLogger cl = chans.next();
		if (cl.should_disable) {
			cl.close_stream();
			chans.remove();
		}
	}
}

@Override
public
void on_message(User user, char[] target, char[] replytarget, char[] message)
{
	char[] nick = user == null ? null : user.nick;
	this.log_standard_message(target, nick, COL_BLACK, COL_WHITE, message, 0, message.length);
}

@Override
public
boolean on_command(User user, char[] target, char[] replytarget,
                   char[] message, char[] cmd, char[] params)
{
	char[] nick = user == null ? null : user.nick;
	this.log_standard_message(target, nick, COL_BLACK, COL_WHITE, message, 0, message.length);
	return false;
}

@Override
public
void on_action(User user, char[] target, char[] replytarget, char[] action)
{
	char[] nick = user == null ? null : user.nick;
	this.log_action_message(target, nick, action, 0, action.length);
}

@Override
public
void on_selfmessage(char[] target, char[] text, int offset, int len)
{
	char[] nick = this.anna.get_anna_user().nick;
	this.log_standard_message(target, nick, COL_BLACK, COL_WHITE, text, offset, len);
}

@Override
public
void on_selfaction(char[] target, char[] text, int offset, int len)
{
	char[] nick = this.anna.get_anna_user().nick;
	this.log_action_message(target, nick, text, offset, len);
}

@Override
public
void on_topic(User user, char[] channel, char[] topic)
{
	LogWriter lw = this.logger(channel);
	if (lw != null) {
		try {
			lw.color(COL_LIGHTBLUE);
			lw.timestamp(this.time());
			if (user != null) {
				lw.writer.write("*** ");
				lw.append_escape(user.nick, 0, user.nick.length);
				lw.writer.write(" changes topic to '");
			} else {
				lw.writer.write("*** topic has been changed to '");
			}
			lw.append_parse_ctrlcodes(topic, 0, topic.length);
			lw.writer.write('\'');
			lw.lf();
		} catch (IOException ignored) {}
	}
}

@Override
public
void on_channelmodechange(Channel chan, User user, int changec, char[] signs, char[] modes,
                          char[] types, char[][] params, ChannelUser[] users)
{
	LogWriter lw = this.logger(chan.name);
	if (lw != null) {
		try {
			lw.color(COL_TEAL);
			lw.timestamp(this.time());
			if (user != null) {
				lw.writer.write("*** ");
				lw.append_escape(user.nick, 0, user.nick.length);
				lw.writer.write(" sets mode: ");
			} else {
				lw.writer.write("*** channel mode changed: ");
			}
			char[] c = chars(make_modestr(changec, signs, modes, types, params));
			lw.append_escape(c, 0, c.length);
			lw.lf();
		} catch (IOException ignored) {}
	}
}

@Override
public
void on_nickchange(User user, char[] oldnick, char[] newnick)
{
	ArrayList<Channel> chans = this.anna.get_joined_channels();
	int i = chans.size();
	while (i-- > 0) {
		Channel chan = chans.get(i);
		ChannelUser usr = chan.find_user(newnick);
		LogWriter lw;
		if (usr != null && (lw = this.logger(chan.name)) != null) {
			try {
				lw.color(COL_PINK);
				lw.timestamp(this.time());
				lw.writer.write("*** ");
				lw.append_escape(oldnick, 0, oldnick.length);
				lw.writer.write(" is now known as ");
				lw.append_escape(newnick, 0, newnick.length);
				lw.lf();
			} catch (IOException ignored) {}
		}
	}
}

@Override
public
void on_kick(User user, char[] channel, char[] kickeduser, char[] msg)
{
	LogWriter lw = this.logger(channel);
	if (lw != null) {
		try {
			lw.color(COL_RED);
			lw.timestamp(this.time());
			lw.writer.write("*** ");
			lw.append_escape(kickeduser, 0, kickeduser.length);
			if (user != null) {
				lw.writer.write(" was kicked by ");
				lw.append_escape(user.nick, 0, user.nick.length);
			} else {
				lw.writer.write(" was kicked");
			}
			if (msg != null) {
				lw.writer.write(" (");
				lw.append_parse_ctrlcodes(msg, 0, msg.length);
				lw.reset();
				lw.color(COL_RED);
				lw.writer.write(')');
			}
			lw.lf();
		} catch (IOException ignored) {}
	}
}

@Override
public
void on_part(User user, char[] channel, char[] msg)
{
	this.log_joinpartquit(channel, user.nick, "Part", COL_LIGHTGREY, msg);
}

@Override
public
void on_quit(User user, char[] msg)
{
	ArrayList<Channel> chans = this.anna.get_joined_channels();
	int i = chans.size();
	while (i-- > 0) {
		Channel chan = chans.get(i);
		ChannelUser usr = chan.find_user(user.nick);
		if (usr != null) {
			this.log_joinpartquit(chan.name, user.nick, "Quit", COL_LIGHTGREY, msg);
		}
	}
}

@Override
public
void on_join(User user, char[] channel)
{
	this.log_joinpartquit(channel, user.nick, "Join", COL_GREY, null);
}

@Override
public
void on_disable()
{
	for (ChannelLogger cl : this.loggers.values()) {
		cl.close_stream();
	}
}

/**
 * @param nick may be {@code null}
 */
private
void log_standard_message(char[] target, char[] nick, int fg, int bg,
                          char[] message, int off, int len)
{
	LogWriter lw = this.logger(target);
	if (lw != null) {
		try {
			if (bg != COL_WHITE) {
				lw.color(fg, bg);
			} else if (fg != COL_BLACK) {
				lw.color(fg);
			}
			lw.timestamp(this.time());
			if (nick != null) {
				lw.writer.write("&lt;");
				ChannelUser usr = this.anna.find_user(target, nick);
				if (usr != null && usr.prefix != 0) {
					lw.writer.write("&#");
					lw.writer.write(String.valueOf((int) usr.prefix));
					lw.writer.write(";");
				}
				lw.append_escape(nick, 0, nick.length);
				lw.writer.write("&gt; ");
			}
			lw.append_parse_ctrlcodes(message, off, len);
			lw.lf();
		} catch (IOException ignored) {}
	}
}

/**
 * @param nick may be {@code null}
 */
private
void log_action_message(char[] target, char[] nick, char[] action, int off, int len)
{
	LogWriter lw = this.logger(target);
	if (lw != null) {
		try {
			lw.color(COL_PINK);
			lw.timestamp(this.time());
			lw.writer.write("* ");
			if (nick != null) {
				ChannelUser usr = this.anna.find_user(target, nick);
				if (usr != null && usr.prefix != 0) {
					lw.writer.write("&#");
					lw.writer.write(String.valueOf((int) usr.prefix));
					lw.writer.write(";");
				}
				lw.append_escape(nick, 0, nick.length);
				lw.writer.write(' ');
			}
			lw.append_parse_ctrlcodes(action, 0, len);
			lw.lf();
		} catch (IOException ignored) {}
	}
}

private
void log_joinpartquit(char[] channel, char[] nick, String type, int color, char[] msg)
{
	LogWriter lw = this.logger(channel);
	if (lw != null) {
		try {
			lw.color(color);
			lw.timestamp(this.time());
			lw.writer.write("*** ");
			lw.writer.write(type);
			lw.writer.write("s: ");
			lw.append_escape(nick, 0, nick.length);
			if (msg != null) {
				lw.writer.write(" (");
				lw.append_parse_ctrlcodes(msg, 0, msg.length);
				lw.reset();
				lw.color(color);
				lw.writer.write(')');
			}
			lw.lf();
		} catch (IOException ignored) {}
	}
}

private
LogWriter logger(char[] chan)
{
	ChannelLogger cl;
	if ((cl = this.loggers.get(new String(chan))) != null) {
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
boolean should_disable;
ModLog mod;
Long timestamp;
int lastday;
char[] chan;
String channel;
File directory;
OutputStreamWriter writer;
int lineswritten;

LogWriter get_or_open_stream()
{
	int day = mod.calendar().get(Calendar.DAY_OF_YEAR);
	boolean wasclosed;
	if ((wasclosed = this.writer == null) || this.lastday != day) {
		if (!wasclosed) {
			String link = this.filename(mod.time()); // no need to do +1 since it's now
			try {
				this.writer.write("\n<p><a href=\"");
				this.writer.write(link.replace("#", "%23"));
				this.writer.write("\">");
				this.writer.write(link);
				this.writer.write(" &gt;</a></p>");
			} catch (IOException e) {
				mod.anna.log_warn(e, "cannot create log file for " + channel);
			}
			close(this.writer);
			this.writer = null;
		}
		this.lastday = day;
		String filename = this.filename(mod.time());
		File of = new File(this.directory, filename);
		if (of.isDirectory()) {
			return null;
		}
		try {
			boolean existed = of.exists();
			this.writer = new OutputStreamWriter(new FileOutputStream(of, true), UTF_8);
			if (!existed) {
				this.writer.write("<style>html{font-family:monospace}</style>\n");
				Instant instant = mod.time().toInstant().minus(1, ChronoUnit.DAYS);
				String link = this.filename(Date.from(instant));
				this.writer.write("<p><a href=\"");
				this.writer.write(link.replace("#", "%23"));
				this.writer.write("\">&lt; ");
				this.writer.write(link);
				this.writer.write("</a></p>\n");
			}
			if (wasclosed) {
				String msg = format(
					"<em>*** session open: %tH:%<tM:%<tS</em><br/>",
					this.timestamp
				);
				this.writer.write(msg);
			}
		} catch (IOException e) {
			mod.anna.log_warn(e, "cannot create log file for " + channel);
		}
	}
	if (this.writer == null) {
		return null;
	}
	return new LogWriter(this);
}

String filename(Date date)
{
	return format("%s-%tY-%<tm-%<td.html", this.channel, date);
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
	"FFF", "000", "00007F", "009300", "F00", "7F0000", "9C009C",
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

final ChannelLogger logger;
final OutputStreamWriter writer;

boolean has_span;
String[] styles = new String[7];

LogWriter(ChannelLogger logger)
{
	this.logger = logger;
	this.writer = logger.writer;
}

void bold()
throws IOException
{
	if (this.styles[STYLE_BOLD] != null) {
		this.styles[STYLE_BOLD] = null;
	} else {
		this.styles[STYLE_BOLD] = "font-weight:bold";
	}
	this.writer.write("<wbr data-ctrl=\"2\">");
	this.add_formatting();
}

void color(int fg)
throws IOException
{
	if (fg < 0 || 15 < fg) {
		fg = COL_BLACK;
	}
	this.writer.write("<wbr data-ctrl=\"3");
	this.writer.write(COLORMAP[fg]);
	this.writer.write("\">");
	this.styles[STYLE_FG] = "color:#" + COLORS[fg];
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
	this.styles[STYLE_FG] = "color:#" + COLORS[fg];
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
	this.writer.write("<wbr data-ctrl=\"9\">");
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
	this.writer.write("<wbr data-ctrl=\"13\">");
	this.add_formatting();
}

void reset()
throws IOException
{
	for (int i = 0; i < this.styles.length; i++) {
		this.styles[i] = null;
	}
	this.writer.write("<wbr data-ctrl=\"15\">");
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
	this.writer.write("<wbr data-ctrl=\"21\">");
	this.add_formatting();
}

void reverse()
throws IOException
{
	if (this.styles[STYLE_REVERSE] != null) {
		this.styles[STYLE_REVERSE] = null;
	} else {
		this.styles[STYLE_REVERSE] = "color:#FFF;background:#000";
	}
	this.writer.write("<wbr data-ctrl=\"22\">");
	this.add_formatting();
}

private
void add_formatting()
throws IOException
{
	if (this.has_span) {
		this.writer.write("</span>");
		this.has_span = false;
	}
	out:
	{
		for (int i = 0; i < this.styles.length; i++) {
			if (this.styles[i] != null) {
				break out;
			}
		}
		return;
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

void timestamp(Date time)
throws IOException
{
	this.writer.write(format("[%tH:%<tM:%<tS] ", time));
}

void lf()
throws IOException
{
	if (this.has_span) {
		this.writer.write("</span>");
		this.has_span = false;
	}
	for (int i = 0; i < this.styles.length; i++) {
		this.styles[i] = null;
	}
	this.writer.write("<br>");
	this.writer.flush();
	this.logger.lineswritten++;
}

void append_parse_ctrlcodes(char[] text, int off, int len)
throws IOException
{
	int color_parsing = 0, fg = 0, bg = 0;
	len += off;
	while (off < len) {
		char c = text[off++];
		switch (color_parsing)
		{
		case 1:
			if ('0' <= c && c <= '9') {
				fg = c - '0';
				color_parsing = 2;
				continue;
			}
			break;
		case 2:
			if (c == ',') {
				color_parsing = 4;
				continue;
			}
			if ('0' <= c && c <= '9') {
				fg = fg * 10 + c - '0';
				color_parsing = 3;
				continue;
			}
			this.color(fg);
			break;
		case 3:
			if (c == ',') {
				color_parsing = 4;
				continue;
			}
			this.color(fg);
			break;
		case 4:
			if ('0' <= c && c <= '9') {
				bg = c - '0';
				color_parsing = 5;
				continue;
			}
			this.color(fg);
			this.writer.write(',');
			break;
		case 5:
			if ('0' <= c && c <= '9') {
				bg = bg * 10 + c - '0';
				color_parsing = 0;
				this.color(fg, bg);
				continue;
			}
			this.color(fg, bg);
			break;
		}
		color_parsing = 0;
		switch (c)
		{
		case CTRL_BOLD:
			this.bold();
			continue;
		case CTRL_COLOR:
			color_parsing = 1;
			continue;
		case CTRL_ITALIC:
			this.italic();
			continue;
		case CTRL_STRIKETHROUGH:
			this.strikethrough();
			continue;
		case CTRL_RESET:
			this.reset();
			continue;
		case CTRL_UNDERLINE:
		case CTRL_UNDERLINE2:
			this.underline();
			continue;
		case CTRL_REVERSE:
			this.reverse();
			continue;
		}
		if (c < ' ' || c == '&' || c == '<' || c == '>' || c > '~') {
			this.writer.write("&#" + String.valueOf((int) c) + ";");
		} else {
			this.writer.write(c);
		}
	}
}

void append_escape(char[] text, int off, int len)
throws IOException
{
	len += off;
	while (off < len) {
		char c = text[off++];
		if (c < ' ' || c == '&' || c == '<' || c == '>' || c > '~') {
			this.writer.write("&#" + String.valueOf((int) c) + ";");
		} else {
			this.writer.write(c);
		}
	}
}
} /*LogWriter*/
