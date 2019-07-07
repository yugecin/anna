// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package net.basdon.anna.api;

import java.io.IOException;
import java.util.Properties;

public interface IMod
{
/**
 * @return the mod name, should have a {@code mod_} prefix by convention
 */
String getName();
/**
 * @return version of the mod, freeform
 */
String getVersion();
/**
 * @return short description of what this mod does
 */
String getDescription();
/**
 * Will be called when the stats server has been hit and wants latest and greatest stats. Print
 * stats into passed {@code output}, preferrable each line indented with one space and ending with
 * LF.
 */
void print_stats(IAnna.Output output)
throws IOException;
/**
 * @return default properties for conf, may be {@code null}
 */
default
Properties get_default_conf()
{
	return null;
}
/**
 * Called when this mod is being loaded.
 * @return {@code false} if it's not possible to enable this mod
 */
boolean on_enable(IAnna anna);
/**
 * Called when this mod is being unloaded.
 * Remember to unregister every registered listeners, interrupt threads, ...
 */
void on_disable();
/**
 * Called when the config of this mod is (re)loaded or changed.
 */
default
void config_loaded(Config conf)
{
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
default
void on_message(User user, char[] target, char[] replytarget, char[] message)
{
}
/**
 * Called when a command message is received. May be channel or private.
 * It's a channel message if {@code target == replytarget}.
 *
 * @param user user that sent the command, may (but should not) be {@code null}
 * @param target place where the message was sent to, channel or anna user if PM
 * @param replytarget target to reply to, channel or sending user if PM
 * @param message the raw message (split into {@code cmd} and {@code params})
 * @param cmd cmd that was invoked
 * @param params parameters of the cmd as one single string, may be {@code null}
 * @return {@code true} if the command was handled (this value is currently unused)
 */
default
boolean on_command(User user, char[] target, char[] replytarget,
                   char[] message, char[] cmd, char[] params)
{
	return false;
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
default
void on_action(User user, char[] target, char[] replytarget, char[] action)
{
}
/**
 * Called when anna sends a message. May be channel or private.
 *
 * @param target where the message was sent
 * @param text buffer containing the message that was sent
 * @param offset offset in the buffer where the message starts
 * @param len length of the message
 */
default
void on_selfmessage(char target[], char text[], int offset, int len)
{
}
/**
 * Called when anna sends an action. May be channel or private.
 *
 * @param target where the message was sent
 * @param text buffer containing the message that was sent
 */
default
void on_selfaction(char target[], char text[])
{
}
/**
 * Called when a topic has been changed.
 *
 * @param user user that changed the topic, may be {@code null}
 * @param channel channel that had its topic changed
 * @param topic the new topic
 */
default
void on_topic(User user, char[] channel, char[] topic)
{
}
/**
 * Called when one or more channel modes have been changed.
 *
 * @param chan affecting channel
 * @param user user that invoked the mode change or {@code null}
 * @param changec amount of changes (don't use the array lenghts)
 * @param signs signs of each change, either {@code +} or {@code -}
 * @param modes modes of each change
 * @param types types of each change, either {@code 'u'} for user or {@code 'a'} {@code 'b'}
 *              {@code 'c'} {@code 'd'}, depending on the type of channel mode
 * @param params parameter when {@code type} is user or channel type b or channel type c with
 *               positive sign
 * @param users user when {@code type} is user
 */
default
void on_channelmodechange(Channel chan, User user, int changec, char[] signs, char[] modes,
                          char[] types, char[][] params, ChannelUser[] users)
{
}
/**
 * Called when a user changes their nick.
 *
 * @param user user that changed their nick (this has the old name)
 * @param newnick the new nickname of this user
 */
default
void on_nickchange(User user, char[] newnick)
{
}
/**
 * Called when a user gets kicked from a channel.
 *
 * @param user user that did the kick action, may be {@code null}
 * @param channel channel where the kick happened
 * @param kickeduser the user that was kicked
 * @param msg kick message, may be {@code null}
 */
default
void on_kick(User user, char[] channel, char[] kickeduser, char[] msg)
{
}
/**
 * Called when a user parts a channel.
 *
 * @param user user that part the channel
 * @param channel channel that the user left
 * @param msg part message, may be {@code null}
 */
default
void on_part(User user, char[] channel, char[] msg)
{
}
/**
 * Called when a user quits.
 *
 * @param user user that quit
 * @param msg quit message, may be {@code null}
 */
default
void on_quit(User user, char[] msg)
{
}
/**
 * Called when a user joins a channel.
 *
 * @param user user that joined a channel
 * @param channel the channel they joined
 */
default
void on_join(User user, char[] channel)
{
}
}
