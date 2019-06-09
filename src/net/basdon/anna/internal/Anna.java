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
	default_config.put("bot.messages.quit",
	                   "only in Sweden can a dance rap song about IRC hit #1 on the charts");
	default_config.put("bot.messages.part",
	                   "Never miss a golden opportunity to keep your mouth shut");
	default_config.put("owners", "robin_be!*@cyber.space");
}

private final Config conf;

private char command_prefix;
private User[] owners;

Output writer;

Anna(Config conf)
{
	this.conf = conf;

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
}

@Override
public
Config load_mod_conf(IMod requester, Properties defaults)
{
	return ConfigImpl.load(requester.getName(), defaults);
}

/**
 * when connected with a server
 */
void established()
{
	String chan = this.conf.getStr("debug.channel");
	if (chan != null && chan.length() > 0 && chan.charAt(0) == '#') {
		this.send_raw("JOIN " + chan);
	}
}

void dispatch_message(Message msg)
{
	if (strcmp(msg.cmd, CMD_PRIVMSG) && msg.paramc == 2 && msg.prefix != null) {
		User user = User.parse(msg.prefix, 1, msg.prefix.length);
		char[] target = msg.paramv[0];
		char[] message = msg.paramv[1];
		boolean is_channel_message = message.length > 0 && message[0] == '#';
		if (message[1] == this.command_prefix) {
			this.handle_command(user, target, is_channel_message, message);
		} else {
			this.handle_message(user, target, is_channel_message, message);
		}
	}
}

void handle_command(@Nullable User user, char[] target, boolean is_channel_message, char[] message)
{
	char[] cmd;
	char[] params = null;
	int len;

	int space = indexOf(message, 2, message.length, ' ');
	if (space == -1) {
		space = message.length;
	}

	cmd = new char[len = space - 2];
	arraycopy(message, 2, cmd, 0, len);

	while (space < message.length && message[space] == ' ') {
		space++;
	}
	if (space < message.length) {
		params = new char[len = message.length - space];
		arraycopy(message, space, params, 0, len);
	}

	if (strcmp(cmd, 's','a','y') && params != null && is_owner(user)) {
		char[] buf = new char[8 + params.length];
		set(buf, 0, 'P','R','I','V','M','S','G',' ');
		arraycopy(params, 0, buf, 8, params.length);
		send_raw(buf, 0, buf.length);
	}
}

void handle_message(@Nullable User user, char[] target, boolean is_channel_message, char[] message)
{
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

interface Output
{
void print(String msg)
throws IOException;

void print(char[] buf, int offset, int len)
throws IOException;
}
}
