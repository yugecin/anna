// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package net.basdon.anna.internal;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.basdon.anna.api.*;

import static java.lang.System.arraycopy;
import static net.basdon.anna.api.Constants.*;
import static net.basdon.anna.api.Util.*;

class Anna implements IAnna
{
static final Properties default_config;
static final ClassLoader parent_loader = IMod.class.getClassLoader();
static final Random rng = new Random();

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
	default_config.put("mods.enabled", "");
	default_config.put("channels", "");
	default_config.put("messages.restart", "I'll be back");
	default_config.put("messages.quit",
	                   "only in Sweden can a dance rap song about IRC hit #1 on the charts");
	default_config.put("messages.part",
	                   "Never miss a golden opportunity to keep your mouth shut");
	default_config.put("owners", "robin_be!*@cyber.space");
}

private final ArrayList<IMod> mods;
private final HashMap<IMod, File> modfile;
private final ArrayList<char[]> raw_buffer;
private final HashMap<String, ConfigImpl> modconfigs;
private final ArrayList<Channel> joined_channels;
private final LinkedList<BufferedChanModeChange> chanmode_updates;

private boolean connection_state;
private long time_start, time_connect, time_disconnect;
private int disconnects;
private int mod_loadcount, mod_unloadcount;
private StatsServer stats_server;
private char command_prefix;
private User[] owners;
private User me;
private char[] debugchan;
private char[] newnick;
private boolean buffer_raw;

/**
 * {@link https://tools.ietf.org/html/draft-brocklesby-irc-isupport-03#section-3.14}
 */
char[] prefixes, modes;
/**
 * {@link https://tools.ietf.org/html/draft-brocklesby-irc-isupport-03#section-3.3}
 */
char[] chanmodes_a, chanmodes_b, chanmodes_c, chanmodes_d;

ConfigImpl conf;

Output writer;

Anna(ConfigImpl conf)
{
	this.time_start = this.time_disconnect = System.currentTimeMillis();
	this.conf = conf;
	this.mods = new ArrayList<>();
	this.modfile = new HashMap<>();
	this.raw_buffer = new ArrayList<>(50);
	this.modconfigs = new HashMap<>();
	this.joined_channels = new ArrayList<>();
	this.chanmode_updates = new LinkedList<>();
	this.buffer_raw = true;

	me = new User();
	me.nick = conf.getStr("bot.nick").toCharArray();
	me.name = conf.getStr("bot.user").toCharArray();
	me.host = new char[] { '*' };

	this.config_loaded();

	// remove old tmp mod jars
	File[] files = new File(".").getAbsoluteFile().listFiles();
	for (File f : files) {
		if (f.getName().startsWith(".tmp-mod_")) {
			if (!f.delete()) {
				Log.warn("could not remove leftover tmp mod jar " + f.getName());
			}
		}
	}

	String enabledmods = conf.getStr("mods.enabled");
	if (enabledmods != null) {
		for (String m : enabledmods.split(String.valueOf(','))) {
			this.mod_load(m.toCharArray(), this.debugchan);
		}
	}
}

private
void config_loaded()
{
	int stats_port = -1;
	if (conf.getBool("stats.enable")) {
		stats_port = conf.getInt("stats.port");
	}
	this.ensure_stats_server(stats_port);

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

	if (!this.connection_state) {
		me.nick = conf.getStr("bot.nick").toCharArray();
	} else {
		// TODO: same for user? info? (if that's even possible?)
		this.newnick = conf.getStr("bot.nick").toCharArray();
		if (strcmp(this.newnick, me.nick)) {
			this.newnick = null;
		} else {
			char[] buf = new char[5 + this.newnick.length];
			set(buf, 0, 'N','I','C','K',' ');
			arraycopy(this.newnick, 0, buf, 5, this.newnick.length);
			this.send_raw(buf, 0, buf.length);
		}
	}

	String debugchan = conf.getStr("debug.channel");
	if (debugchan != null && debugchan.length() > 0 && debugchan.charAt(0) == '#') {
		this.debugchan = debugchan.toCharArray();
	} else {
		this.debugchan = "#anna".toCharArray();
	}

	if (this.connection_state) {
		this.join_channels();
	}
}

/**
 * makes sure the stat server is running or not running
 *
 * @param port port that stats server should have (can be {@code -1} to ensure that it doesn't run)
 */
private
void ensure_stats_server(int port)
{
	if (port <= 1024 || 65500 <= port) {
		Log.error("stats.port must be between 1024 and 66500");
		port = -1;
	}
	if (port == -1) {
		if (this.stats_server != null) {
			this.stats_server.interrupt();
		}
		return;
	}
	if (this.stats_server != null &&
		this.stats_server.isAlive() &&
		!this.stats_server.isInterrupted())
	{
		if (this.stats_server.port == port) {
			return;
		}
		this.stats_server.interrupt();
	}
	this.stats_server = new StatsServer(this, port);
	this.stats_server.start();
}

@Override
public
boolean load_mod_conf(IMod requester)
{
	String modname = requester.getName();
	ConfigImpl conf = ConfigImpl.load(modname, requester.get_default_conf());
	if (conf == null) {
		this.modconfigs.remove(modname);
		return false;
	}
	this.modconfigs.put(modname, conf);
	requester.config_loaded(conf);
	return true;
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
		out.print(" active channels and users:\n");
		int i = this.joined_channels.size();
		while (i-- > 0) {
			out.print("  ");
			Channel chan = this.joined_channels.get(i);
			out.print(chan.name, 0, chan.name.length);
			out.print(":");
			int j = chan.userlist.size();
			while (j-- > 0) {
				out.print(" ");
				ChannelUser usr = chan.userlist.get(j);
				if (usr.prefix != 0) {
					out.print(String.valueOf(usr.prefix));
				}
				out.print(usr.nick, 0, usr.nick.length);
			}
			out.print("\n");
		}
		out.print(" mods load/unload counts: ");
		out.print(this.mod_loadcount + "/" + this.mod_unloadcount + "\n");
		out.print(" mods loaded now: " + this.mods.size() + "\n");
		out.print(" enabled mods: ");
		String enabled = this.conf.getStr("mods.enabled");
		if (enabled != null) {
			out.print(enabled);
		}
		out.print("\n");
		out.print("\n");
		for (IMod mod : this.mods) {
			out.print(mod.getName() + "\n");
			final IOException[] ex = { null };
			boolean res = mod_invoke(mod, "print_stats", () -> {
				try {
					mod.print_stats(out);
				} catch (IOException e) {
					ex[0] = e;
				}
			});
			if (ex[0] != null) {
				throw ex[0];
			}
			if (!res) {
				out.print("\n-anna: something broke\n");
			}
			out.print("\n");
		}
	}
}

void connecting()
{
	this.joined_channels.clear();
	this.chanmode_updates.clear();
	this.prefixes = PREFIXES_DEFAULT;
	this.modes = MODES_DEFAULT;
	this.chanmodes_a = this.chanmodes_b = this.chanmodes_c = this.chanmodes_d = EMPTY_CHAR_ARR;
	ChannelUserImpl.maxmodes = this.modes.length;
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

		// PREFIX=(qaohv)~&@%+
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
					ChannelUserImpl.maxmodes = this.modes.length;
				}
			}
		}

		// CHANMODES=beI,kLf,l,psmntirzMQNRTOVKDdGPZSCc
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
	this.join_channels();
	this.buffer_raw = false;
	for (char[] line : this.raw_buffer) {
		this.send_raw(line, 0, line.length);
	}
	this.raw_buffer.clear();
}

void disconnected()
{
	this.raw_buffer.clear();
	this.buffer_raw = true;
	this.writer = null;
	this.connection_state = false;
	this.disconnects++;
}

private
void join_channels()
{
	String channels = this.conf.getStr("channels");
	if (channels == null) {
		return;
	}
	char[] c = channels.toCharArray();
	int from = 0;
	char[] raw = new char[100];
	set(raw, 0, 'J','O','I','N',' ');
	do
	{
		int to = indexOf(c, from + 1, c.length, ',');
		if (to == -1) {
			to = c.length;
		}
		int len = to - from;
		if (0 < len && len + 5 < raw.length) {
			arraycopy(c, from, raw, 5, len);
			this.send_raw(raw, 0, 5 + len);
		}
		from = to + 1;
	} while (from < c.length);
}

void dispatch_message(Message msg)
{
	// buffer everything that will get sent until the message is completely handled
	this.buffer_raw = true;
	this.dispatch_message0(msg);
	this.buffer_raw = false;
	for (char[] line : this.raw_buffer) {
		this.send_raw(line, 0, line.length);
	}
	this.raw_buffer.clear();
}

private
void dispatch_message0(Message msg)
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
			ChannelImpl chan = this.channel_find(target);
			if (chan != null) {
				if (msg.paramc > 1) {
					chan.mode_changed(this, user, msg.paramv, msg.paramc);
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
	if (strcmp(msg.cmd, CMD_QUIT)) {
		handle_quit(user, msg.paramv[0]);
		return;
	}

	// <- :mib!*@* PART #anna :he
	// <- :mib!*@* PART #anna
	if (strcmp(msg.cmd, CMD_PART) && user != null && msg.paramc > 0) {
		handle_part(user, msg.paramv[0], msg.paramv[1]);
		return;
	}

	// <- :Anna^!Anna@* KICK #anna robin_be :bai
	// <- :Anna^!Anna@* KICK #anna robin_be
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
		ChannelImpl chan = this.channel_find(msg.paramv[msg.paramc - 2]);
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
			ChannelUserImpl usr = chan.get_or_add_user(nick);
			if (mode != 0) {
				usr.mode_add(mode, this.modes, this.prefixes);
			}
			off = nextoffset + 1;
		}
		return;
	}

	// <- :server 366 Anna^ #anna :End of /NAMES list.
	if (msg.cmdnum == RPL_ENDOFNAMES && msg.trailing_param) {
		char[] chan = msg.paramv[msg.paramc - 2];
		if (!this.chanmode_updates.isEmpty()) {
			Iterator<BufferedChanModeChange> iter = this.chanmode_updates.iterator();
			while (iter.hasNext()) {
				BufferedChanModeChange change = iter.next();
				if (strcmp(chan, change.chan.name)) {
					change.dispatch(this);
					iter.remove();
				}
			}
		}
		return;
	}

	// <- :server 433 anna robin_be :Nickname is already in use.
	if (msg.cmdnum == ERR_NICKNAMEINUSE) {
		if (this.newnick != null) {
			this.log_warn("nickname is already in use");
			this.newnick = null;
		} else {
			Log.warn("got ERR_NICKNAMEINUSE but no new nick pending?");
		}
		return;
	}
}

/**
 * Called when a user joins a channel.
 *
 * @param user user that joined a channel
 * @param channel the channel they joined
 */
void handle_join(User user, char[] channel)
{
	if (strcmp(user.nick, me.nick)) {
		this.channel_unregister(channel);
		ChannelImpl chan = new ChannelImpl(channel);
		this.joined_channels.add(chan);
	} else {
		// this is not executed when anna joins,
		// since a namelist will take care of channel users when anna joins a channel
		ChannelImpl chan = this.channel_find(channel);
		if (chan != null) {
			chan.userlist.add(new ChannelUserImpl(user.nick));
		}
	}

	this.mods_invoke("join", m -> m.on_join(user, channel));
}

/**
 * Called when a user quits.
 *
 * @param user user that quit
 * @param msg quit message, may be {@code null}
 */
void handle_quit(User user, char[] msg)
{
	this.mods_invoke("quit", m -> m.on_quit(user, msg));
	if (user != null) {
		for (Channel chan : this.joined_channels) {
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
}

/**
 * Called when a user parts a channel.
 *
 * @param user user that part the channel
 * @param channel channel that the user left
 * @param msg part message, may be {@code null}
 */
void handle_part(User user, char[] channel, char[] msg)
{
	this.mods_invoke("part", m -> m.on_part(user, channel, msg));
	this.channel_remove_user(user.nick, channel);

	if (strcmp(user.nick, me.nick)) {
		this.channel_unregister(channel);
	}
}

/**
 * Called when a user gets kicked from a channel.
 *
 * @param user user that did the kick action, may be {@code null}
 * @param channel channel where the kick happened
 * @param kickeduser the user that was kicked
 * @param msg kick message, may be {@code null}
 */
void handle_kick(User user, char[] channel, char[] kickeduser, char[] msg)
{
	this.mods_invoke("kick", m -> m.on_kick(user, channel, kickeduser, msg));
	this.channel_remove_user(kickeduser, channel);

	if (strcmp(user.nick, me.nick)) {
		this.channel_unregister(channel);
	}
}

/**
 * Called when a user changes their nick.
 *
 * @param user user that changed their nick (this has the old name)
 * @param newnick the new nickname of this user
 */
void handle_nick(User user, char[] newnick)
{
	char[] oldnick = user.nick;
	if (strcmp(me.nick, user.nick)) {
		me.nick = newnick;
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
	this.mods_invoke("nickchange", m -> m.on_nickchange(user, oldnick, newnick));
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

	final char[] p = params;
	this.mods_invoke("command", m -> m.on_command(user, target, replytarget, message, cmd, p));

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
		ChannelImpl chan = this.channel_find(params);
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
			if (usr.prefix != 0) {
				sb.append(usr.prefix);
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
		return;
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
		return;
	}

	if (strcmp(cmd, 'u','n','l','o','a','d','m','o','d') && is_owner(user)) {
		if (params == null) {
			this.privmsg(replytarget, "specify mod_name".toCharArray());
			return;
		}
		mod_unload(params, replytarget);
		return;
	}

	if (strcmp(cmd, 'r','e','l','o','a','d','m','o','d') && is_owner(user)) {
		if (params == null) {
			this.privmsg(replytarget, "specify mod_name".toCharArray());
			return;
		}
		mod_unload(params, replytarget);
		mod_load(params, replytarget);
		return;
	}

	if (strcmp(cmd, 'e','n','a','b','l','e','m','o','d') && is_owner(user)) {
		if (params == null) {
			this.privmsg(replytarget, "specify mod_name".toCharArray());
			return;
		}
		String modname = new String(params);
		String enabledmods = this.conf.getStr("mods.enabled");
		boolean enabled = false;
		for (String m : enabledmods.split(String.valueOf(','))) {
			if (modname.equals(m)) {
				this.privmsg(replytarget, "mod is already enabled".toCharArray());
				enabled = true;
				break;
			}
		}
		if (!enabled) {
			if (enabledmods.isEmpty()) {
				enabledmods = modname;
			} else {
				enabledmods += ',' + modname;
			}
			this.conf.props.setProperty("mods.enabled", enabledmods);
			this.conf.save();
			// don't need to reload because these changes are ignored anyways
		}
		for (IMod m : this.mods) {
			if (modname.equals(m.getName())) {
				return;
			}
		}
		this.mod_load(params, replytarget);
		return;
	}

	if (strcmp(cmd, 'd','i','s','a','b','l','e','m','o','d') && is_owner(user)) {
		if (params == null) {
			this.privmsg(replytarget, "specify mod_name".toCharArray());
			return;
		}
		String modname = new String(params);
		String enabled = this.conf.getStr("mods.enabled");
		String[] enabledmods;
		if (enabled == null) {
			enabledmods = new String[0];
		} else {
			enabledmods = enabled.split(String.valueOf(','));
		}
		boolean found = false;
		StringBuilder sb = new StringBuilder();
		for (String m : enabledmods) {
			if (!modname.equals(m)) {
				if (sb.length() > 0) {
					sb.append(',');
				}
				sb.append(m);
			} else {
				found = true;
			}
		}
		if (found) {
			this.conf.props.setProperty("mods.enabled", sb.toString());
			this.conf.save();
			// don't need to reload because these changes are ignored anyways
		} else {
			this.privmsg(replytarget, "mod was not enabled".toCharArray());
		}
		for (IMod m : this.mods) {
			if (modname.equals(m.getName())) {
				this.mod_unload(params, replytarget);
				return;
			}
		}
		return;
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

	boolean addconf = false;
	if (is_owner(user) && (
			(strcmp(cmd, 'c','o','n','f')) ||
			(strcmp(cmd, 'c','o','n','f','a','d','d') && (addconf = true))
		))
	{
		int space, space2;
		if (params == null ||
			(space = indexOf(params, 0, params.length, ' ')) == -1 ||
			space == params.length - 1 ||
			(space2 = indexOf(params, space + 1, params.length, ' ')) != -1 &&
			space2 == space + 1)
		{
			this.privmsg(replytarget, "conf modname setting".toCharArray());
			return;
		}
		String modname = new String(params, 0, space);
		ConfigImpl conf;
		if ("anna".equals(modname)) {
			conf = this.conf;
			// prevent sensitive conf exposure (nickserv passwords, ..)
		} else {
			if ((conf = this.modconfigs.get(modname)) == null) {
				this.privmsg(replytarget, "no conf for that mod".toCharArray());
				return;
			}
		}
		int to = space2 == -1 ? params.length : space2;
		String key = new String(params, space + 1, to - space - 1);
		String val = conf.props.getProperty(key);
		if (val == null && !addconf) {
			this.privmsg(replytarget, "unknown prop".toCharArray());
			return;
		}
		if (space2 != -1) {
			key = new String(params, space + 1, space2 - space - 1);
			if (space2 == params.length - 1) {
				val = "";
			} else {
				val = new String(params, space2 + 1, params.length - space2 - 1);
			}
			conf.props.setProperty(key, val);
			if (!conf.save()) {
				this.privmsg(replytarget, "failed to save conf".toCharArray());
			}
			if (conf == this.conf) {
				this.config_loaded();
			} else {
				for (IMod m : this.mods) {
					if (modname.equals(m.getName())) {
						m.config_loaded(conf);
						break;
					}
				}
			}
		}
		this.privmsg(replytarget, (key + ": " + val).toCharArray());
		return;
	}

	if (strcmp(cmd, 'r','e','l','o','a','d','c','o','n','f') && is_owner(user)) {
		if (params == null || params.length == 0) {
			this.privmsg(replytarget, "reloadconf modname".toCharArray());
			return;
		}
		success:
		{
			fail:
			{
				ConfigImpl conf;
				if (strcmp(params, 'a','n','n','a')) {
					conf = ConfigImpl.load("anna", default_config);
					if (conf != null) {
						this.conf = conf;
						this.config_loaded();
						break success;
					}
				} else {
					String modname = new String(params, 0, params.length);
					for (IMod m : this.mods) {
						if (modname.equals(m.getName())) {
							if (this.load_mod_conf(m)) {
								break success;
							}
							break fail;
						}
					}
					this.privmsg(replytarget, "no such mod".toCharArray());
					return;
				}
			}
			this.privmsg(replytarget, "failed to load conf".toCharArray());
			return;
		}
		this.privmsg(replytarget, "reloaded".toCharArray());
		return;
	}

	if (strcmp(cmd, 'm','o','d','s')) {
		StringBuilder sb = new StringBuilder("loaded mods:");
		for (IMod mod : this.mods) {
			sb.append(' ').append(mod.getName());
		}
		this.privmsg(replytarget, chars(sb));
		return;
	}
}

/**
 * Called when a message is received (commands excluded). May be channel or private.
 * It's a channel message if {@code target == replytarget}.
 *
 * @param user user that sent the message, may be {@code null}
 * @param target place where the message was sent to, channel or anna user if PM
 * @param replytarget target to reply to, channel or sending user if PM
 * @param message message that was sent
 */
void handle_message(User user, char[] target, char[] replytarget, char[] message)
{
	this.mods_invoke("message", m -> m.on_message(user, target, replytarget, message));
}

/**
 * Called when an action message is received. May be channel or private.
 * It's a channel message if {@code target == replytarget}.
 *
 * @param user user that sent the action, may be {@code null}
 * @param target place where the action was sent to, channel or anna user if PM
 * @param replytarget target to reply to, channel or sending user if PM
 * @param action action that was sent
 */
void handle_action(User user, char[] target, char[] replytarget, char[] action)
{
	this.mods_invoke("action", m -> m.on_action(user, target, replytarget, action));
}

/**
 * Called when a topic has been changed.
 *
 * @param user user that changed the topic, may be {@code null}
 * @param channel channel that had its topic changed
 * @param topic the new topic
 */
void handle_topic(User user, char[] channel, char[] topic)
{
	this.mods_invoke("topic", m -> m.on_topic(user, channel, topic));
}

/**
 * Called when one or more channel modes have been changed.
 *
 * @param chan affecting channel
 * @param user user that invoked the mode change
 * @param changec amount of changes (don't use the array lenghts)
 * @param signs signs of each change, either {@code +} or {@code -}
 * @param modes modes of each change
 * @param types types of each change, either {@code 'u'} for user or {@code 'a'} {@code 'b'}
 *              {@code 'c'} {@code 'd'}, depending on the type of channel mode
 * @param params parameter when {@code type} is user or channel type a or channel type b or channel
 *               type c with positive sign
 * @param users user when {@code type} is user
 */
void handle_channelmodechange(ChannelImpl chan, User user, int changec, char[] signs, char[] modes,
                              char[] types, char[][] params, ChannelUser[] users)
{
	this.mods_invoke("channelmodechange", m ->
		m.on_channelmodechange(chan, user, changec, signs, modes, types, params, users)
	);
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
	this.modconfigs.remove(modnamestr);
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
	for (IMod m : this.mods) {
		if (modnamestr.equals(m.getName())) {
			this.privmsg(replytarget, "mod is already loaded!".toCharArray());
			return;
		}
	}
	File modfile = new File(modnamestr + ".jar").getAbsoluteFile();
	if (!modfile.exists() || !modfile.isFile()) {
		this.privmsg(replytarget, (modnamestr + ".jar not found").toCharArray());
		return;
	}
	String rand = String.valueOf((rng.nextInt() + 100000) & 0xffffff);
	Path parent = modfile.toPath().getParent();
	if (parent == null) {
		this.privmsg(replytarget, "failed to get parent dir".toCharArray());
		return;
	}
	Path np = parent.resolve(".tmp-" + modnamestr + "-" + rand);
	try {
		Files.copy(modfile.toPath(), np, StandardCopyOption.REPLACE_EXISTING);
	} catch (IOException e) {
		this.privmsg(replytarget, "failed to create copy".toCharArray());
		return;
	}
	File nf = np.toFile();
	try {
		URLClassLoader cl = null;
		try {
			URL[] urls = { np.toUri().toURL() };
			cl = new URLClassLoader(urls, parent_loader);
			Object instance = Class.forName("annamod.Mod", true, cl).newInstance();
			IMod m;
			try {
				m = (IMod) instance;
			} catch (ClassCastException ignored) {
				m = ((IModLoader) instance).load();
			}
			IMod mod = m;
			if (!modnamestr.equals(mod.getName())) {
				this.privmsg(
					replytarget,
					("name mismatch (" + mod.getName() + ")").toCharArray()
				);
				return;
			}
			Supplier<Boolean> f = () -> Boolean.valueOf(mod.on_enable(this, replytarget));
			if (!Boolean.TRUE.equals(mod_invoke(mod, "enable", f))) {
				this.privmsg(replytarget, "mod failed to enable".toCharArray());
				return;
			}
			cl = null; // null to prevent close in finally block
			this.mods.add(mod);
			this.modfile.put(mod, nf);
			String msg = "loaded " + mod.getName() + " v" + mod.getVersion();
			Log.info(msg);
			this.privmsg(replytarget, msg.toCharArray());
			this.mod_loadcount++;
			nf.deleteOnExit();
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
			if (cl != null && !nf.delete()) {
				nf.deleteOnExit();
			}
		}
	} catch (Throwable t) {
		Log.error("failed to load mod " + modnamestr, t);
		nf.delete();
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
		File modfile = this.modfile.get(mod);
		this.modfile.remove(mod);
		mod_invoke(mod, "disable", mod::on_disable);
		ClassLoader cl = mod.getClass().getClassLoader();
		if (cl instanceof URLClassLoader) {
			mod = null; // remove last (hopefully) reference before closing
			close((URLClassLoader) cl);
		} else {
			Log.warn("mod " + mod.getName() + " classloader is not URLClassLoader");
		}
		if (!modfile.delete()) {
			Log.warn("could not remove temp mod file " + modfile.getName());
		}
		this.mod_unloadcount++;
	}
}

/**
 * @return {@code false} if something broke
 */
boolean mod_invoke(IMod mod, String target, Runnable invoker)
{
	try {
		invoker.run();
		return true;
	} catch (Throwable t) {
		Log.error("issue while invoking " + target + " on mod " + mod.getName(), t);
		return false;
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
ChannelImpl channel_find(char[] channel)
{
	return (ChannelImpl) find_channel(channel);
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

	ChannelImpl chan = this.channel_find(channel);
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
User get_anna_user()
{
	return this.me;
}

@Override
public
ArrayList<Channel> get_joined_channels()
{
	return this.joined_channels;
}

@Override
public Channel find_channel(char[] channel)
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

@Override
public
ChannelUser find_user(char[] channel, char[] nick)
{
	int i = this.joined_channels.size();
	while (i-- > 0) {
		Channel chan = this.joined_channels.get(i);
		if (strcmp(channel, chan.name)) {
			i = chan.userlist.size();
			while (i-- > 0) {
				ChannelUser usr = chan.userlist.get(i);
				if (strcmp(nick, usr.nick)) {
					return usr;
				}
			}
			return null;
		}
	}
	return null;
}

@Override
public
char[] get_user_channel_modes()
{
	return this.modes;
}

@Override
public
char[] get_user_channel_prefixes()
{
	return this.prefixes;
}

@Override
public
boolean is_owner(User user)
{
	if (user == null) {
		return false;
	}
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
	this.privmsg(target, text, 0, text.length);
}

public
void privmsg(char[] target, char[] text, int offset, int len)
{
	if (target == null) {
		return;
	}
	char[] buf = new char[8 + target.length + 2 + len];
	int off = 0;
	off = set(buf, off, "PRIVMSG ".toCharArray());
	arraycopy(target, 0, buf, off, target.length);
	off += target.length;
	buf[off++] = ' ';
	buf[off++] = ':';
	arraycopy(text, offset, buf, off, len);
	send_raw(buf, 0, buf.length);
	this.mods_invoke("selfmessage", m -> m.on_selfmessage(target, text, offset, len));
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
	this.mods_invoke("selfaction", m -> m.on_selfaction(target, text));
}

@Override
public
void join(char[] channel)
{
	if (this.find_channel(channel) == null) {
		char[] buf = new char[5 + channel.length];
		set(buf, 0, 'J','O','I','N',' ');
		arraycopy(channel, 0, buf, 5, channel.length);
		this.send_raw(buf, 0, buf.length);
	}
}

@Override
public
void send_raw(char[] buf, int offset, int len)
{
	if (this.buffer_raw) {
		if (offset == 0 && len == buf.length) {
			this.raw_buffer.add(buf);
		} else {
			char[] trimmed = new char[len];
			arraycopy(buf, offset, trimmed, 0, len);
			this.raw_buffer.add(trimmed);
		}
		return;
	}
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
void log_warn(Throwable t, String message)
{
	Log.warn(message, t);
	this.privmsg(this.debugchan, ("warn(+t): " + message).toCharArray());
}

@Override
public
void log_warn(String message)
{
	Log.warn(message);
	this.privmsg(this.debugchan, ("warn: " + message).toCharArray());
}

@Override
public
void sync_exec(Runnable func)
{
	synchronized (Anna.lock) {
		func.run();
	}
}

/**
 * stores a batch of channel mode changes
 *
 * these may be dispatched to {@link Anna#handle_usermodechange} directly, or buffered until after
 * receiving RPL_ENDOFNAMES for the matching channel
 */
static
class BufferedChanModeChange
{
ChannelImpl chan;
User user;
/**
 * amount of elements in {@code users}, {@code signs}, {@code modes}, {@code types}
 */
int changec;
char[] signs;
char[] modes;
char[] types;
char[][] params;
ChannelUser[] users;

BufferedChanModeChange(ChannelImpl chan, User user, int maxc)
{
	this.chan = chan;
	this.user = user;
	this.users = new ChannelUser[maxc];
	this.signs = new char[maxc];
	this.modes = new char[maxc];
	this.types = new char[maxc];
	this.params = new char[maxc][];
}

void dispatch(Anna anna)
{
	if (changec > 0) {
		anna.handle_channelmodechange(chan, user, changec, signs, modes,
		                              types, params, users);
	}
}

void schedule(Anna anna)
{
	anna.chanmode_updates.add(this);
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
} /*Anna*/
