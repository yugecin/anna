// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package annamod;

import net.basdon.anna.api.*;
import net.basdon.anna.api.IAnna.Output;

import java.io.IOException;

import static java.lang.System.out;
import static net.basdon.anna.api.Util.*;

public class Mod implements IMod
{
@Override
public
String getName()
{
	return "mod_test";
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
	return "prints all IMod events to console to test things";
}

@Override
public
void print_stats(Output output)
throws IOException
{
	output.print(" print_stats is working\n");
}

@Override
public
boolean on_enable(IAnna anna)
{
	out.println("mod_test: enabled");
	return true;
}

@Override
public
void on_disable()
{
	out.println("mod_test: disabled");
}

@Override
public
void on_message(User user, char[] target, char[] replytarget, char[] message)
{
	out.printf(
		"mod_test: on_message user: %s target: %s replytarget: %s message: %s%n",
		user == null ? "null" : user.toString(),
		new String(target),
		new String(replytarget),
		new String(message)
	);
}

@Override
public
void on_selfmessage(char[] target, char[] text, int offset, int len)
{
	out.printf(
		"mod_test: on_selfmessage target: %s text: %s%n",
		new String(target),
		new String(text, offset, len)
	);
}

@Override
public
void on_selfaction(char[] target, char[] text)
{
	out.printf(
		"mod_test: on_selfaction target: %s text: %s%n",
		new String(target),
		new String(text)
	);
}

@Override
public
boolean on_command(User user, char[] target, char[] replytarget,
                   char[] message, char[] cmd, char[] params)
{
	out.printf(
		"mod_test: on_command user: %s target: %s replytarget: %s message: %s "
		+ "cmd: %s params: %s%n",
		user == null ? "null" : user.toString(),
		new String(target),
		new String(replytarget),
		new String(message),
		new String(cmd),
		params == null ? "null" : new String(params)
	);
	return false;
}

@Override
public
void on_action(User user, char[] target, char[] replytarget, char[] action)
{
	out.printf(
		"mod_test: on_action user: %s target: %s replytarget: %s action: %s%n",
		user.toString(),
		new String(target),
		new String(replytarget),
		new String(action)
	);
}

@Override
public
void on_topic(User user, char[] channel, char[] topic)
{
	out.printf(
		"mod_test: on_message user: %s channel: %s topic: %s%n",
		user == null ? "null" : user.toString(),
		new String(channel),
		new String(topic)
	);
}

@Override
public
void on_channelmodechange(Channel chan, User user, int changec, char[] signs, char[] modes,
                          char[] types, char[][] params, ChannelUser[] users)
{
	for (int i = 0; i < changec; i++) {
		out.printf(
			"mod_test: on_usermodechanged chan:"
			+ "%s user: %s sign: %s mode: %s param: %s%n",
			new String(chan.name),
			user == null ? "null" : new String(user.nick),
			String.valueOf(signs[i]),
			String.valueOf(modes[i]),
			params[i] == null ? "" : new String(params[i])
		);
	}
	out.printf(
		"mod_test: on_usermodechanged chan: %s mode: %s%n",
		new String(chan.name),
		make_modestr(changec, signs, modes, types, params).toString()
	);
}

@Override
public void on_nickchange(User user, char[] oldnick, char[] newnick)
{
	out.printf(
		"mod_test: on_nickchange user: %s oldnick: %s newnick: %s%n",
		user.toString(),
		new String(oldnick),
		new String(newnick)
	);
}

@Override
public void on_kick(User user, char[] channel, char[] kickeduser, char[] msg)
{
	out.printf(
		"mod_test: on_kick user: %s channel: %s kickeduser: %s msg: %s%n",
		user == null ? "null" : user.toString(),
		new String(channel),
		new String(kickeduser),
		msg == null ? "null" : new String(msg)
	);
}

@Override
public void on_part(User user, char[] channel, char[] msg)
{
	out.printf(
		"mod_test: on_part user: %s channel: %s msg: %s%n",
		user == null ? "null" : user.toString(),
		new String(channel),
		msg == null ? "null" : new String(msg)
	);
}

@Override
public void on_quit(User user, char[] msg)
{
	out.printf(
		"mod_test: on_quit user: %s msg: %s%n",
		user.toString(),
		msg == null ? "null" : new String(msg)
	);
}

@Override
public void on_join(User user, char[] channel)
{
	out.printf(
		"mod_test: on_join user: %s channel: %s%n",
		user.toString(),
		new String(channel)
	);
}
}
