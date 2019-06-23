// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package net.basdon.anna.internal;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.basdon.anna.api.Config;
import net.basdon.anna.api.IAnna;
import net.basdon.anna.api.IMod;
import net.basdon.anna.api.Message;
import net.basdon.anna.api.User;

import static java.lang.System.arraycopy;
import static net.basdon.anna.api.Constants.*;
import static net.basdon.anna.api.Util.*;

class Anna implements IAnna
{
static final Properties default_config;
static final ClassLoader parent_loader = IMod.class.getClassLoader();

/**
 * {@link https://tools.ietf.org/html/draft-brocklesby-irc-isupport-03#section-3.14}
 */
static final char[]
	PREFIXES_DEFAULT = { '@', '+' },
	MODES_DEFAULT = { 'o', 'v' };

/**
 * lock for synchronizing: anna, statsserver, jobs
 */
static Object lock = new Object();

static
{
	default_config = new Properties();
	default_config.put("server.host", "irc.example.com");
	default_config.put("server.port", "6667");
	default_config.put("connection.retrytimeoutseconds", "2");
	default_config.put("commands.prefix", "&");
	default_config.put("stats.enable", "true");
	default_config.put("stats.port", "7755");
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

private final ArrayList<IMod> mods;
private final ArrayList<Channel> joined_channels;
private final LinkedList<BufferedUserModeChange> usermode_updates;

private boolean connection_state;
private long time_start, time_connect, time_disconnect;
private int disconnects;
private StatsServer stats_server;
private char command_prefix;
private User[] owners;
private User me;
private char[] debugchan;

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
	this.time_start = this.time_disconnect = System.currentTimeMillis();
	this.conf = conf;
	this.mods = new ArrayList<>();
	this.joined_channels = new ArrayList<>();
	this.usermode_updates = new LinkedList<>();

	if (conf.getBool("stats.enable")) {
		int stats_server_port = conf.getInt("stats.port");
		if (1024 < stats_server_port && stats_server_port < 65500) {
			this.stats_server = new StatsServer(this, stats_server_port);
			this.stats_server.start();
		} else {
			Log.error("stats.port must be between 1024 and 66500");
		}
	}

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

	String debugchan = conf.getStr("debug.channel");
	if (debugchan != null && debugchan.length() > 0 && debugchan.charAt(0) == '#') {
		this.debugchan = debugchan.toCharArray();
	} else {
		this.debugchan = "#anna".toCharArray();
	}
}

@Override
public
Config load_mod_conf(IMod requester, Properties defaults)
{
	return ConfigImpl.load(requester.getName(), defaults);
}

void print_stats(Output out)
throws IOException
{
	synchronized (lock) {
		out.print("Anna\n");
		out.print(" boot: " + format_time(this.time_start) + "\n");
		if (this.connection_state) {
			out.print(" connection: connected since "
			          + format_time(time_connect) + "\n");
		} else {
			out.print(" connection: disconnected since "
			          + format_time(time_disconnect) + "\n");
		}
		out.print(" disconnects since boot: " + disconnects + "\n");
		out.print(" bytes recv: " + Main.recv + "\n");
		out.print(" bytes sent: " + Main.sent + "\n");
		out.print("\n");
	}
}

void connecting()
{
	this.joined_channels.clear();
	this.usermode_updates.clear();
	this.prefixes = PREFIXES_DEFAULT;
	this.modes = MODES_DEFAULT;
	this.chanmodes_a = this.chanmodes_b = this.chanmodes_c = this.chanmodes_d = EMPTY_CHAR_ARR;
	ChannelUser.maxmodes = this.modes.length;
}

void shutdown()
{
	Iterator<IMod> iter = this.mods.iterator();
	while (iter.hasNext()) {
		IMod mod = iter.next();
		iter.remove();
		IMod[] mods = { mod };
		mod = null; // remove last (hopefully) reference
		mod_disable(mods);
	}
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
	char[] buf = new char[5 + this.debugchan.length];
	set(buf, 0, 'J','O','I','N',' ');
	arraycopy(this.debugchan, 0, buf, 5, this.debugchan.length);
	this.send_raw(buf, 0, buf.length);
	this.connection_state = true;
	this.time_connect = System.currentTimeMillis();
}

void disconnected()
{
	this.writer = null;
	this.connection_state = false;
	this.disconnects++;
}

void dispatch_message(Message msg)
{
	User user = null;
	if (msg.prefix != null) {
		user = User.parse(msg.prefix, 0, msg.prefix.length);
	}

	// <- :mib!*@* PRIVMSG #anna :ttt
	if (strcmp(msg.cmd, CMD_PRIVMSG) && msg.prefix != null && msg.paramc == 2) {
		char[] target = msg.paramv[0];
		char[] message = msg.paramv[1];
		char[] replytarget;
		if (strcmp(target, me.nick)) {
			replytarget = user == null ? null : user.nick;
		} else {
			replytarget = target;
		}

		if (strcmp(message, 0, 8, '\1','A','C','T','I','O','N',' ') &&
			message[message.length - 1] == '\1')
		{
			char[] action = new char[message.length - 9];
			arraycopy(message, 8, action, 0, action.length);
			this.handle_action(user, target, replytarget, action);
			return;
		}

		if (message[0] == this.command_prefix) {
			this.handle_command(user, target, replytarget, message);
			return;
		}

		this.handle_message(user, target, replytarget, message);
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
					chan.mode_changed(this, msg.paramv, msg.paramc);
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
	if (strcmp(msg.cmd, CMD_JOIN) && user != null && msg.paramc > 0) {
		handle_join(user, msg.paramv[0]);
		return;
	}

	// <- :mib!*@* QUIT :Quit: http://www.mibbit.com ajax IRC Client
	if (strcmp(msg.cmd, CMD_QUIT) && user != null && msg.paramc > 0) {
		handle_quit(user, msg.paramv[0], msg.paramv[1]);
		return;
	}

	// <- :mib!*@* PART #anna :he
	// <- :mib!*@* PART #anna
	if (strcmp(msg.cmd, CMD_PART) && user != null && msg.paramc > 0) {
		handle_part(user, msg.paramv[0], msg.paramv[1]);
		return;
	}

	// <- :Anna^!Anna@EXP-93BCB88D.access.telenet.be KICK #anna robin_be :bai
	// <- :Anna^!Anna@EXP-93BCB88D.access.telenet.be KICK #anna robin_be
	if (strcmp(msg.cmd, CMD_KICK) && user != null && msg.paramc > 1) {
		handle_kick(user, msg.paramv[0], msg.paramv[1], msg.paramv[2]);
		return;
	}

	// <- :robin_be!*@* TOPIC #anna :topic
	if (strcmp(msg.cmd, CMD_TOPIC) && msg.paramc == 2) {
		handle_topic(user, msg.paramv[0], msg.paramv[1]);
		return;
	}

	// <- :mib!*@* NICK :mib78
	if (strcmp(msg.cmd, CMD_NICK) && user != null && msg.paramc == 1) {
		handle_nick(user, msg.paramv[0]);
		return;
	}

	// <- :server 353 Anna^ = #anna :Anna^ @robin_be
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
			char[] nick;
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
			nick = new char[len];
			arraycopy(users, off, nick, 0, len);
			ChannelUser usr = chan.get_or_add_user(nick);
			if (mode != 0) {
				usr.mode_add(mode);
			}
			off = nextoffset + 1;
		}
		return;
	}

	// <- :server 366 Anna^ #anna :End of /NAMES list.
	if (msg.cmdnum == RPL_ENDOFNAMES && msg.trailing_param) {
		char[] chan = msg.paramv[msg.paramc - 2];
		if (!this.usermode_updates.isEmpty()) {
			Iterator<BufferedUserModeChange> iter = this.usermode_updates.iterator();
			while (iter.hasNext()) {
				BufferedUserModeChange change = iter.next();
				if (strcmp(chan, change.chan.name)) {
					change.dispatch(this);
					iter.remove();
				}
			}
		}
	}
}

void handle_join(User user, char[] channel)
{
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

/**
 * @param msg quit msg or {@code null}
 */
void handle_quit(User user, char[] channel, char[] msg)
{
}

/**
 * @param msg part msg or {@code null}
 */
void handle_part(User user, char[] channel, char[] msg)
{
	this.channel_remove_user(user.nick, channel);
}

/**
 * @param msg kick msg or {@code null}
 */
void handle_kick(User user, char[] channel, char[] kickeduser, char[] msg)
{
	this.channel_remove_user(kickeduser, channel);
}

void handle_nick(User user, char[] newnick)
{
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

/**
 * it's a channel message if {@code target == replytarget}
 * @param user user that sent the command or {@code null}
 * @param target target message was sent to, either a channel or anna user
 * @param replytarget target to send a reply to, either a channel or user that send the command, may
 *        be {@code null}
 */
void handle_command(User user, char[] target, char[] replytarget, char[] message)
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
		return;
	}

	if (strcmp(cmd, 's','a','y') && params != null && is_owner(user)) {
		int space = indexOf(params, 0, params.length, ' ');
		if (space == -1 || space == params.length - 1) {
			this.privmsg(replytarget, "usage: say <target> <message>".toCharArray());
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
			this.privmsg(replytarget, "usage: action <target> <message>".toCharArray());
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
			sb.append(this.joined_channels.get(i).name);
		}
		this.privmsg(replytarget, chars(sb));
		return;
	}

	if (strcmp(cmd, 'u','s','e','r','s') && params != null && is_owner(user)) {
		Channel chan = this.channel_find(params);
		if (chan == null) {
			this.privmsg(replytarget, "I'm not in that channel".toCharArray());
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
		this.privmsg(replytarget, chars(sb));
		return;
	}

	if (strcmp(cmd, 's','t','a','t','s') && is_owner(user)) {
		if (this.stats_server == null) {
			this.privmsg(replytarget, "stats server was not started".toCharArray());
			return;
		}
		if (this.stats_server.isAlive()) {
			this.privmsg(replytarget, "stats server seems to be running".toCharArray());
			return;
		}
		this.stats_server = this.stats_server.create_new();
		this.stats_server.start();
		this.privmsg(replytarget, "stats server was dead, is restarted".toCharArray());
	}

	if (strcmp(cmd, 'k','i','l','l','s','t','a','t','s') && is_owner(user)) {
		if (this.stats_server == null) {
			this.privmsg(replytarget, "stats server was not started".toCharArray());
			return;
		}
		if (this.stats_server.isAlive()) {
			this.stats_server.interrupt();
			this.privmsg(replytarget, "stats server interrupted".toCharArray());
			return;
		}
		this.privmsg(replytarget, "stats server is dead".toCharArray());
		return;
	}

	if (strcmp(cmd, 'l','o','a','d','m','o','d') && is_owner(user)) {
		if (params == null) {
			this.privmsg(replytarget, "specify mod_name".toCharArray());
			return;
		}
		mod_load(params, replytarget);
	}

	if (strcmp(cmd, 'u','n','l','o','a','d','m','o','d') && is_owner(user)) {
		if (params == null) {
			this.privmsg(replytarget, "specify mod_name".toCharArray());
			return;
		}
		mod_unload(params, replytarget);
	}

	if (strcmp(cmd, 'r','e','l','o','a','d','m','o','d') && is_owner(user)) {
		if (params == null) {
			this.privmsg(replytarget, "specify mod_name".toCharArray());
			return;
		}
		mod_unload(params, replytarget);
		mod_load(params, replytarget);
	}

	if (strcmp(cmd, 'm','o','d','i','n','f','o') && is_owner(user)) {
		if (params == null) {
			this.privmsg(replytarget, "specify mod_name".toCharArray());
			return;
		}
		String modname = new String(params);
		for (IMod mod : this.mods) {
			if (modname.equals(mod.getName())) {
				String m;
				m = modname + " v" + mod.getVersion() + ": " + mod.getDescription();
				this.privmsg(replytarget, m.toCharArray());
				return;
			}
		}
		this.privmsg(replytarget, "mod not loaded".toCharArray());
		return;
	}
}

/**
 * it's a channel message if {@code target == replytarget}
 * @param user user that sent the message or {@code null}
 * @param target target message was sent to, either a channel or anna user
 * @param replytarget target to send a reply to, either a channel or user that send the command
 */
void handle_message(User user, char[] target, char[] replytarget, char[] message)
{
}

/**
 * it's a channel message if {@code target == replytarget}
 * @param user user that sent the action or {@code null}
 * @param target target message was sent to, either a channel or anna user
 * @param replytarget target to send a reply to, either a channel or user that send the command
 */
void handle_action(User user, char[] target, char[] replytarget, char[] action)
{
}

void handle_topic(User user, char[] channel, char[] topic)
{
}

void handle_usermodechange(Channel chan, ChannelUser user, char sign, char mode)
{
}

/**
 * @param replytarget target to send messages to, may be {@null}
 */
private
void mod_unload(char[] modname, char[] replytarget)
{
	String modnamestr = new String(modname);
	IMod mod = null;
	Iterator<IMod> iter = this.mods.iterator();
	while (iter.hasNext()) {
		IMod m = iter.next();
		if (modnamestr.equals(m.getName())) {
			mod = m;
			iter.remove();
			break;
		}
	}
	if (mod == null) {
		this.privmsg(replytarget, "mod is not loaded".toCharArray());
		return;
	}
	IMod[] mods = { mod };
	mod = null; // remove last (hopefully) reference
	mod_disable(mods);
	this.privmsg(replytarget, "unloaded".toCharArray());
}

/**
 * @param replytarget target to send messages to, may be {@null}
 */
private
void mod_load(char[] modname, char[] replytarget)
{
	String modnamestr = new String(modname);
	File modfile = new File(modnamestr + ".jar");
	if (!modfile.exists() || !modfile.isFile()) {
		this.privmsg(replytarget, (modnamestr + ".jar not found").toCharArray());
		return;
	}
	try {
		URLClassLoader cl = null;
		try {
			URL[] urls = { modfile.toPath().toUri().toURL() };
			cl = new URLClassLoader(urls, parent_loader);
			Class<?> modclass = Class.forName("annamod.Mod", true, cl);
			IMod mod = (IMod) modclass.newInstance();
			if (!modnamestr.equals(mod.getName())) {
				this.privmsg(
					replytarget,
					("name mismatch (" + mod.getName() + ")").toCharArray()
				);
				return;
			}
			if (!Boolean.TRUE.equals(mod_invoke(mod, "enable", mod::onEnable))) {
				this.privmsg(replytarget, "mod failed to enable".toCharArray());
				return;
			}
			cl = null; // null to prevent close in finally block
			this.mods.add(mod);
			String msg = "loaded " + mod.getName() + " v" + mod.getVersion();
			this.privmsg(replytarget, msg.toCharArray());
		} catch (MalformedURLException e) {
			this.privmsg(replytarget, "failed, check log".toCharArray());
			throw e;
		} catch (ClassNotFoundException e) {
			this.privmsg(replytarget, "invalid mod".toCharArray());
			throw e;
		} catch (ReflectiveOperationException e) {
			this.privmsg(replytarget, "could not instantiate mod".toCharArray());
			throw e;
		} catch (ClassCastException e) {
			this.privmsg(replytarget, "not an IMod".toCharArray());
			throw e;
		} catch (Throwable t) {
			this.privmsg(replytarget, "uncaught error, check log".toCharArray());
			throw t;
		} finally {
			close(cl);
		}
	} catch (Throwable t) {
		Log.error("failed to load mod " + modnamestr, t);
	}
}

/**
 * @param mods array of {@link IMod} to disable. Passed as an array to be able te remove local
 *        variable references.
 */
void mod_disable(IMod[] mods)
{
	for (int i = 0; i < mods.length; i++) {
		IMod mod = mods[i];
		mods[i] = null;
		mod_invoke(mod, "disable", mod::onDisable);
		ClassLoader cl = mod.getClass().getClassLoader();
		if (cl instanceof URLClassLoader) {
			mod = null; // remove last (hopefully) reference before closing
			close((URLClassLoader) cl);
		} else {
			Log.warn("mod " + mod.getName() + " classloader is not URLClassLoader");
		}
	}
}

void mod_invoke(IMod mod, String target, Runnable invoker)
{
	try {
		invoker.run();
	} catch (Throwable t) {
		Log.error("issue while invoking " + target + " on mod " + mod.getName(), t);
	}
}

<T>
T mod_invoke(IMod mod, String target, Supplier<T> invoker)
{
	try {
		return invoker.get();
	} catch (Throwable t) {
		Log.error("issue while invoking " + target + " on mod " + mod.getName(), t);
	}
	return null;
}

void mods_invoke(String target, Consumer<IMod> invoker)
{
	for (IMod mod : this.mods) {
		try {
			invoker.accept(mod);
		} catch (Throwable t) {
			Log.error("issue while invoking " + target + " on mod " + mod.getName(), t);
		}
	}
}

/**
 * @return channel or {@code null}
 */
Channel channel_find(char[] channel)
{
	int i = this.joined_channels.size();
	while (i-- > 0) {
		Channel chan = this.joined_channels.get(i);
		if (strcmp(channel, chan.name)) {
			return chan;
		}
	}
	return null;
}

void channel_unregister(char[] channel)
{
	int i = this.joined_channels.size();
	while (i-- > 0) {
		if (strcmp(channel, this.joined_channels.get(i).name)) {
			this.joined_channels.remove(i);
			return;
		}
	}
}

void channel_remove_user(char[] user, char[] channel)
{
	if (strcmp(user, me.nick)) {
		this.channel_unregister(channel);
		return;
	}

	Channel chan = this.channel_find(channel);
	if (chan != null) {
		int i = chan.userlist.size();
		while (i-- > 0) {
			ChannelUser u = chan.userlist.get(i);
			if (strcmp(user, u.nick)) {
				chan.userlist.remove(i);
				break;
			}
		}
	}
}

@Override
public
boolean is_owner(User user)
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
	if (target == null) {
		return;
	}
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
	if (target == null) {
		return;
	}
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

@Override
public
void log_error(Throwable t, String message)
{
	Log.error(message, t);
	this.privmsg(this.debugchan, ("err(+t): " + message).toCharArray());
}

@Override
public
void log_error(String message)
{
	Log.error(message);
	this.privmsg(this.debugchan, ("err: " + message).toCharArray());
}

@Override
public
void log_warn(String message)
{
	Log.warn(message);
	this.privmsg(this.debugchan, ("warn: " + message).toCharArray());
}

/**
 * stores a batch of channel mode changes to users
 *
 * these may be dispatched to {@link Anna#handle_usermodechange} directly, or buffered until after
 * receiving RPL_ENDOFNAMES for the matching channel
 */
static
class BufferedUserModeChange
{
Channel chan;
/**
 * amount of elements in {@code userv}, {@code signs} and {@code modes}
 */
int userc;
ChannelUser[] userv;
char[] signs;
char[] modes;

BufferedUserModeChange(Channel chan, int maxc)
{
	this.chan = chan;
	this.userv = new ChannelUser[maxc];
	this.signs = new char[maxc];
	this.modes = new char[maxc];
}

void dispatch(Anna anna)
{
	while (userc-- > 0) {
		anna.handle_usermodechange(chan, userv[userc], signs[userc], modes[userc]);
	}
}

void shedule(Anna anna)
{
	anna.usermode_updates.add(this);
}
} /*BufferedUserModeChange*/

static
class QuitException extends RuntimeException
{
} /*QuitException*/

static
class RestartException extends RuntimeException
{
} /*RestartException*/

interface Output
{
default
void print(String msg)
throws IOException
{
	char[] chars = msg.toCharArray();
	this.print(chars, 0, chars.length);
}

void print(char[] buf, int offset, int len)
throws IOException;
} /*Output*/
} /*Anna*/
