// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package annamod;

import net.basdon.anna.api.Channel;
import net.basdon.anna.api.ChannelUser;
import net.basdon.anna.api.IAnna.Output;
import net.basdon.anna.api.IMod;
import net.basdon.anna.api.User;

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
	output.print("  print_stats is working\n");
}

@Override
public
boolean on_enable()
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
		user.toString(),
		new String(target),
		new String(replytarget),
		new String(message)
	);
}

@Override
public
void on_command(User user, char[] target, char[] replytarget, char[] cmd, char[] params)
{
	out.printf(
		"mod_test: on_command user: %s target: %s replytarget: %s cmd: %s params: %s%n",
		user.toString(),
		new String(target),
		new String(replytarget),
		new String(cmd),
		new String(params)
	);
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
		user.toString(),
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
}
