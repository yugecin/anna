// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package annamod;

import net.basdon.anna.api.*;
import net.basdon.anna.api.IAnna.Output;

import static java.lang.System.out;

import java.io.IOException;

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
boolean on_command(User user, char[] target, char[] replytarget, char[] cmd, char[] params)
{
	out.printf(
		"mod_test: on_command user: %s target: %s replytarget: %s cmd: %s params: %s%n",
		user == null ? "null" : user.toString(),
		new String(target),
		new String(replytarget),
		new String(cmd),
		new String(params)
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
void on_usermodechange(Channel chan, ChannelUser user, char sign, char mode)
{
	out.printf(
		"mod_test: on_usermodechanged chan: %s user: %s sign: %s mode: %s%n",
		new String(chan.name),
		new String(user.nick),
		String.valueOf(sign),
		String.valueOf(mode)
	);
}

@Override
public void on_nickchange(User user, char[] newnick)
{
	out.printf(
		"mod_test: on_nickchange user: %s newnick: %s%n",
		user.toString(),
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
