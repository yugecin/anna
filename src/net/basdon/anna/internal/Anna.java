// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package net.basdon.anna.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import net.basdon.anna.api.Config;
import net.basdon.anna.api.IAnna;
import net.basdon.anna.api.IMod;
import net.basdon.anna.api.Message;
import net.basdon.anna.api.Nullable;
import net.basdon.anna.api.User;

import static java.lang.System.arraycopy;
import static net.basdon.anna.api.Constants.*;
import static net.basdon.anna.api.Util.*;

class Anna implements IAnna
{
static final Properties default_config;

/**
 * {@link https://tools.ietf.org/html/draft-brocklesby-irc-isupport-03#section-3.14}
 */
static final char[]
	PREFIXES_DEFAULT = { '@', '+' },
	MODES_DEFAULT = { 'o', 'v' };

static
{
	default_config = new Properties();
	default_config.put("server.host", "irc.example.com");
	default_config.put("server.port", "6667");
	default_config.put("connection.retrytimeoutseconds", "2");
	default_config.put("commands.prefix", "&");
	default_config.put("debug.channel", "#anna");
	default_config.put("debug.print.incoming", "false");
	default_config.put("debug.print.outgoing", "false");
	default_config.put("bot.nick", "Anna^");
	default_config.put("bot.user", "Anna");
	default_config.put("bot.userinfo", "github.com/yugecin/anna");
	default_config.put("messages.restart", "I'll be back");
	default_config.put("messages.quit",
	                   "only in Sweden can a dance rap song about IRC hit #1 on the charts");
	default_config.put("messages.part",
	                   "Never miss a golden opportunity to keep your mouth shut");
	default_config.put("owners", "robin_be!*@cyber.space");
}

private final ArrayList<Channel> joined_channels;

private char command_prefix;
private User[] owners;
private User me;

/**
 * {@link https://tools.ietf.org/html/draft-brocklesby-irc-isupport-03#section-3.14}
 */
char[] prefixes, modes;
/**
 * {@link https://tools.ietf.org/html/draft-brocklesby-irc-isupport-03#section-3.3}
 */
char[] chanmodes_a, chanmodes_b, chanmodes_c, chanmodes_d;

final Config conf;

Output writer;

Anna(Config conf)
{
	this.conf = conf;
	this.joined_channels = new ArrayList<>();

	String cmdprefix = conf.getStr("commands.prefix");
	if (cmdprefix != null && cmdprefix.length() == 1) {
		this.command_prefix = cmdprefix.charAt(0);
	} else {
		this.command_prefix = '&';
	}

	String owners = conf.getStr("owners");
	if (owners != null) {
		char[] ownrs = owners.toCharArray();
		ArrayList<User> ownerlist = new ArrayList<>();
		int comma_pos = -1;
		for (;;) {
			int last_pos = comma_pos + 1;
			comma_pos = indexOf(ownrs, last_pos, ownrs.length, ',');
			if (comma_pos == -1) {
				comma_pos = ownrs.length;
			}
			User u = User.parse(ownrs, last_pos, comma_pos);
			if (u != null) {
				ownerlist.add(u);
			}
			if (comma_pos == ownrs.length) {
				break;
			}
			comma_pos++;
		}
		this.owners = ownerlist.toArray(User.EMPTY_ARRAY);
	} else {
		this.owners = User.EMPTY_ARRAY;
	}

	me = new User();
	me.nick = conf.getStr("bot.nick").toCharArray();
	me.name = conf.getStr("bot.user").toCharArray();
	me.host = new char[] { '*' };
}

@Override
public
Config load_mod_conf(IMod requester, Properties defaults)
{
	return ConfigImpl.load(requester.getName(), defaults);
}

void connecting()
{
	this.joined_channels.clear();
	this.prefixes = PREFIXES_DEFAULT;
	this.modes = MODES_DEFAULT;
	this.chanmodes_a = this.chanmodes_b = this.chanmodes_c = this.chanmodes_d = EMPTY_CHAR_ARR;
	ChannelUser.maxmodes = this.modes.length;
}

/**
 * {@link https://tools.ietf.org/html/draft-brocklesby-irc-isupport-03}
 */
void isupport(int paramc, char[][] paramv)
{
	while (paramc-- > 0) {
		char[] p = paramv[paramc];
		int eq = indexOf(p, 0, p.length, '=');
		if (eq == -1) {
			continue;
		}

		if (strcmp(p, 0, eq, 'P','R','E','F','I','X')) {
			int par = indexOf(p, eq, p.length, ')');
			if (par != -1 && p[eq + 1] == '(') {
				int modestart = eq + 2;
				int prefixstart = par + 1;
				int modecount = par - eq - 2;
				int prefixcount = p.length - par - 1;
				if (modecount == prefixcount) {
					this.modes = new char[modecount];
					this.prefixes = new char[modecount];
					for (int i = 0; i < modecount; i++) {
						this.modes[i] = p[modestart + i];
						this.prefixes[i] = p[prefixstart + i];
					}
					ChannelUser.maxmodes = this.modes.length;
				}
			}
		}

		if (strcmp(p, 0, eq, 'C','H','A','N','M','O','D','E','S')) {
			int[] commas = new int[3];
			if (occurrences(p, eq, p.length, ',', commas, 3) == 3) {
				int lastcomma = indexOf(p, commas[2] + 1, p.length, ',');
				if (lastcomma == -1) {
					lastcomma = p.length;
				}
				this.chanmodes_a = new char[commas[0] - eq - 1];
				this.chanmodes_b = new char[commas[1] - commas[0] - 1];
				this.chanmodes_c = new char[commas[2] - commas[1] - 1];
				this.chanmodes_d = new char[lastcomma - commas[2] - 1];
				for (int i = eq + 1, j = 0; i < commas[0]; i++, j++) {
					this.chanmodes_a[j] = p[i];
				}
				for (int i = commas[0] + 1, j = 0; i < commas[1]; i++, j++) {
					this.chanmodes_b[j] = p[i];
				}
				for (int i = commas[1] + 1, j = 0; i < commas[2]; i++, j++) {
					this.chanmodes_c[j] = p[i];
				}
				for (int i = commas[2] + 1, j = 0; i < lastcomma; i++, j++) {
					this.chanmodes_d[j] = p[i];
				}
			}
		}
	}
}

void connected(Output writer)
{
	this.writer = writer;
	String chan = this.conf.getStr("debug.channel");
	if (chan != null && chan.length() > 0 && chan.charAt(0) == '#') {
		this.send_raw("JOIN " + chan);
	}
}

void disconnected()
{
	this.writer = null;
}

void dispatch_message(Message msg)
{
	User user = null;
	if (msg.prefix != null) {
		user = User.parse(msg.prefix, 0, msg.prefix.length);
	}

	// TODO: all these msg.prefix != null checks should be user != null?

	// <- :mib!*@* PRIVMSG #anna :ttt
	if (strcmp(msg.cmd, CMD_PRIVMSG) && msg.paramc == 2 && msg.prefix != null) {
		char[] target = msg.paramv[0];
		char[] message = msg.paramv[1];
		boolean is_channel_message = target[0] == '#';
		if (message[0] == this.command_prefix) {
			this.handle_command(user, target, is_channel_message, message);
		}
		this.handle_message(user, target, is_channel_message, message);
		return;
	}

	// <- :robin_be!*@* MODE #anna +tv robin_be
	// <- :robin_be!*@* MODE #anna +v mib
	// <- :robin_be!*@* MODE #anna +o-v mib mib
	// <- :robin_be!*@* MODE #anna -o mib
	// <- :robin_be!*@* MODE #anna +ov mib mib
	// <- :robin_be!*@* MODE #anna -t
	if (strcmp(msg.cmd, CMD_MODE) && msg.prefix != null && msg.paramc > 0) {
		char[] target = msg.paramv[0];
		if (target[0] == '#') {
			Channel chan = this.channel_find(target);
			if (chan != null) {
				if (msg.paramc > 1) {
					chan.mode_changed(this, msg.paramv, msg.paramc, 1);
				} else {
					Log.warn("MODE for channel but not enough params");
				}
			} else {
				Log.warn("received MODE for unknown channel");
			}
		}
		return;
	}

	// <- :mib!*@* JOIN :#anna
	if (strcmp(msg.cmd, CMD_JOIN) && msg.prefix != null && msg.paramc > 0) {
		handle_join(user, msg.paramv[0]);
		return;
	}

	// <- :mib!*@* QUIT :Quit: http://www.mibbit.com ajax IRC Client
	if (strcmp(msg.cmd, CMD_QUIT) && msg.prefix != null && msg.paramc > 0) {
		handle_quit(user, msg.paramv[0], msg.paramv[1]);
		return;
	}

	// <- :mib!*@* PART #anna :he
	// <- :mib!*@* PART #anna
	if (strcmp(msg.cmd, CMD_PART) && msg.prefix != null && msg.paramc > 0) {
		handle_part(user, msg.paramv[0], msg.paramv[1]);
		return;
	}

	// <- :robin_be!*@* TOPIC #anna :topic
	if (strcmp(msg.cmd, CMD_TOPIC) && msg.prefix != null && msg.paramc == 2) {
		handle_topic(user, msg.paramv[0], msg.paramv[1]);
		return;
	}

	// <- :mib!*@* NICK :mib78
	if (strcmp(msg.cmd, CMD_NICK) && msg.prefix != null && msg.paramc == 1) {
		handle_nick(user, msg.paramv[0]);
		return;
	}

	if (msg.cmdnum == RPL_NAMREPLY  && msg.trailing_param) {
		Channel chan = this.channel_find(msg.paramv[msg.paramc - 2]);
		if (chan == null) {
			Log.warn("got NAMES reply for channel that is not registered");
			return;
		}
		char[] users = msg.paramv[msg.paramc - 1];
		int[] spaces = new int[users.length / 2 + 2];
		int spacecount = occurrences(users, 0, users.length, ' ', spaces, spaces.length);
		if (spaces[spacecount - 1] != users.length - 1) {
			spaces[spacecount++] = users.length;
		}
		int off = 0;
		for (int i = 0; i < spacecount; i++) {
			char[] name;
			int nextoffset = spaces[i];
			char mode;
			int prefix_idx = array_idx(this.prefixes, users[off]);
			if (prefix_idx == -1) {
				mode = 0;
			} else {
				off++;
				mode = this.modes[prefix_idx];
			}
			int len = nextoffset - off;
			name = new char[len];
			arraycopy(users, off, name, 0, len);
			ChannelUser usr = new ChannelUser(name);
			if (mode != 0) {
				usr.mode_add(mode);
			}
			chan.userlist.add(usr);
			off = nextoffset + 1;
		}
		return;
	}
}

void handle_join(@Nullable User user, char[] channel)
{
	if (user == null) {
		return;
	}

	if (strcmp(user.nick, me.nick)) {
		this.channel_unregister(channel);
		Channel chan = new Channel(channel);
		this.joined_channels.add(chan);
		// do not add own user, namelist will handle that
		return;
	}

	Channel chan = this.channel_find(channel);
	if (chan != null) {
		chan.userlist.add(new ChannelUser(user.nick));
	}
}

void handle_quit(@Nullable User user, char[] channel, @Nullable char[] msg)
{
}

void handle_part(@Nullable User user, char[] channel, @Nullable char[] msg)
{
	if (user == null) {
		return;
	}

	if (strcmp(user.nick, me.nick)) {
		this.channel_unregister(channel);
		return;
	}

	Channel chan = this.channel_find(channel);
	if (chan != null) {
		int i = chan.userlist.size();
		while (i-- > 0) {
			ChannelUser u = chan.userlist.get(i);
			if (strcmp(user.nick, u.nick)) {
				chan.userlist.remove(i);
				break;
			}
		}
	}
}

void handle_nick(@Nullable User user, char[] newnick)
{
	if (user == null) {
		return;
	}
	int i = this.joined_channels.size();
	while (i-- > 0) {
		Channel chan = this.joined_channels.get(i);
		int j = chan.userlist.size();
		while (j-- > 0) {
			ChannelUser usr = chan.userlist.get(j);
			if (strcmp(user.nick, usr.nick)) {
				usr.nick = newnick;
				break;
			}
		}
	}
}

void handle_command(@Nullable User user, char[] target, boolean is_channel_message, char[] message)
{
	char[] cmd;
	char[] params = null;

	{
		int len;

		// start at idx 1 to skip command prefix
		int space = indexOf(message, 1, message.length, ' ');
		if (space == -1) {
			space = message.length;
		}

		// len - 1 to skip command prefix
		cmd = new char[len = space - 1];
		arraycopy(message, 1, cmd, 0, len);

		while (space < message.length && message[space] == ' ') {
			space++;
		}
		if (space < message.length) {
			params = new char[len = message.length - space];
			arraycopy(message, space, params, 0, len);
		}
	}

	if (strcmp(cmd, 'r','a','w') && params != null && is_owner(user)) {
		send_raw(params, 0, params.length);
	}

	if (strcmp(cmd, 's','a','y') && params != null && is_owner(user)) {
		int space = indexOf(params, 0, params.length, ' ');
		if (space == -1 || space == params.length - 1) {
			this.privmsg(target, "usage: say <target> <message>".toCharArray());
		} else {
			char[] saytarget = new char[space];
			char[] saytext = new char[params.length - space - 1];
			arraycopy(params, 0, saytarget, 0, space);
			arraycopy(params, space + 1, saytext, 0, saytext.length);
			this.privmsg(saytarget, saytext);
		}
	}

	if (strcmp(cmd, 'a','c','t','i','o','n') && params != null && is_owner(user)) {
		int space = indexOf(params, 0, params.length, ' ');
		if (space == -1 || space == params.length - 1) {
			this.privmsg(target, "usage: action <target> <message>".toCharArray());
		} else {
			char[] actiontarget = new char[space];
			char[] actiontext = new char[params.length - space - 1];
			arraycopy(params, 0, actiontarget, 0, space);
			arraycopy(params, space + 1, actiontext, 0, actiontext.length);
			this.action(actiontarget, actiontext);
		}
	}

	if (strcmp(cmd, 'd','i','e') && is_owner(user)) {
		throw new QuitException();
	}

	if (strcmp(cmd, 'r','e','s','t','a','r','t') && is_owner(user)) {
		throw new RestartException();
	}

	if (strcmp(cmd, 'c','h','a','n','n','e','l','s') && is_owner(user)) {
		StringBuilder sb = new StringBuilder(500).append("I am in:");
		int i = this.joined_channels.size();
		while (i-- > 0) {
			sb.append(' ');
			sb.append(this.joined_channels.get(i).channel);
		}
		this.privmsg(target, chars(sb));
		return;
	}

	if (strcmp(cmd, 'u','s','e','r','s') && params != null && is_owner(user)) {
		Channel chan = this.channel_find(params);
		if (chan == null) {
			this.privmsg(target, "I'm not in that channel".toCharArray());
			return;
		}
		StringBuilder sb = new StringBuilder(500);
		sb.append("userlist of ").append(params).append(':');
		int i = chan.userlist.size();
		while (i-- > 0) {
			sb.append(' ');
			ChannelUser usr = chan.userlist.get(i);
			int prefixidx = this.modes.length;
			int modei = usr.modec;
			while (modei-- > 0) {
				int idx = array_idx(this.modes, usr.modev[modei]);
				if (idx != -1 && idx < prefixidx) {
					prefixidx = idx;
				}
			}
			if (prefixidx != this.modes.length) {
				sb.append(this.prefixes[prefixidx]);
			}
			sb.append(usr.nick);
			sb.append('Z'); // to not nickalert everyone
		}
		this.privmsg(target, chars(sb));
		return;
	}
}

void handle_message(@Nullable User user, char[] target, boolean is_channel_message, char[] message)
{
}

void handle_topic(@Nullable User user, char[] channel, char[] topic)
{
}

@Nullable
Channel channel_find(char[] channel)
{
	int i = this.joined_channels.size();
	while (i-- > 0) {
		Channel chan = this.joined_channels.get(i);
		if (strcmp(channel, chan.channel)) {
			return chan;
		}
	}
	return null;
}

void channel_unregister(char[] channel)
{
	int i = this.joined_channels.size();
	while (i-- > 0) {
		if (strcmp(channel, this.joined_channels.get(i).channel)) {
			this.joined_channels.remove(i);
			return;
		}
	}
}

@Override
public
boolean is_owner(@Nullable User user)
{
	for (User o : this.owners) {
		if (o.matches(user)) {
			return true;
		}
	}
	return false;
}

@Override
public
void privmsg(char[] target, char[] text)
{
	char[] buf = new char[8 + target.length + 2 + text.length];
	int off = 0;
	off = set(buf, off, "PRIVMSG ".toCharArray());
	arraycopy(target, 0, buf, off, target.length);
	off += target.length;
	buf[off++] = ' ';
	buf[off++] = ':';
	arraycopy(text, 0, buf, off, text.length);
	send_raw(buf, 0, buf.length);
}

@Override
public
void action(char[] target, char[] text)
{
	char[] buf = new char[8 + target.length + 10 + text.length + 1];
	int off = 0;
	off = set(buf, off, "PRIVMSG ".toCharArray());
	arraycopy(target, 0, buf, off, target.length);
	off += target.length;
	off = set(buf, off, ' ',':',(char) 1,'A','C','T','I','O','N',' ');
	arraycopy(text, 0, buf, off, text.length);
	off += text.length;
	buf[off] = 1;
	send_raw(buf, 0, buf.length);
}

@Override
public
void send_raw(String msg)
{
	char[] chars = msg.toCharArray();
	this.send_raw(chars, 0, chars.length);
}

@Override
public
void send_raw(char[] buf, int offset, int len)
{
	if (this.writer != null) {
		try {
			char[] msg = new char[len + 2];
			arraycopy(buf, offset, msg, 0, len);
			msg[len] = '\r';
			msg[len + 1] = '\n';
			this.writer.print(msg, 0, msg.length);
		} catch (IOException e) {
			Log.error("could not send message from anna", e);
		}
	}
}

class QuitException extends RuntimeException
{
}

class RestartException extends RuntimeException
{
}

interface Output
{
void print(String msg)
throws IOException;

void print(char[] buf, int offset, int len)
throws IOException;
}
}
