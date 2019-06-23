// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package net.basdon.anna.api;

import java.io.IOException;

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
 * stats into passed {@code output}, preferrable each line indented with two spaces and ending with
 * LF.
 */
void print_stats(IAnna.Output output)
throws IOException;

/**
 * Called when this mod is being loaded.
 * @return {@code false} if it's not possible to enable this mod
 */
boolean on_enable();
/**
 * Called when this mod is being unloaded.
 * Remember to unregister every registered listeners etc.
 */
void on_disable();
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
 * @param cmd cmd that was invoked
 * @param params parameters of the cmd as one single string
 */
default
void on_command(User user, char[] target, char[] replytarget, char[] cmd, char[] params)
{
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
 * Called when a user's (channel) mode has been changed.
 *
 * @param chan affecting channel
 * @param user user which' mode was changed
 * @param sign the sign, either {@code '+'} or {@code '-'}
 * @param mode the mode that was added or removed
 */
default
void on_usermodechange(Channel chan, ChannelUser user, char sign, char mode)
{
}
}
