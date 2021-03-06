// Copyright 2019-2021 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package net.basdon.anna.api;

import java.io.IOException;
import java.util.ArrayList;

public interface IAnna
{
/**
 * @return the loaded anna conf
 */
Config get_anna_conf();
/**
 * Load the config file associated with this mod.
 * Config file will be loaded from {@code conf/<modname>.properties}
 * If the config was loaded successfully, {@link IMod#config_loaded} will be called.
 * Default properties are read from {@link IMod#get_default_conf}
 *
 * @param requester the mod that wants to load its config
 * @return {@code false} if an error occured while reading the file or the config did not
 *         exist and defaults could not be written into a new config file.
 */
boolean load_mod_conf(IMod requester);
/**
 * @return {@linke User} representing anna (not a copy, don't edit values)
 */
User get_anna_user();
/**
 * @return a list of all joined channels (not a copy, don't edit this list)
 */
ArrayList<Channel> get_joined_channels();
/**
 * @return found channel or {@code null}
 */
Channel find_channel(char[] channel);
/**
 * @return found user or {@code null}
 */
ChannelUser find_user(char[] channel, char[] nick);
/**
 * @return array of all known user channel modes (not a copy, don't edit this array)
 */
char[] get_user_channel_modes();
/**
 * @return array of all known user channel mode prefixes (not a copy, don't edit this array)
 */
char[] get_user_channel_prefixes();
/**
 * check if given user is listed as an Anna owner in the config
 * @param user may be {@code null}
 */
boolean is_owner(User user);
/**
 * @param target may be {@code null}, msg will not be sent
 */
void privmsg(char[] target, char[] msg);
/**
 * @param target may be {@code null}, msg will not be sent
 */
void privmsg(char[] target, char[] msg, int offset, int len);
/**
 * @param target may be {@code null}, msg will not be sent
 */
void action(char[] target, char[] msg);
/**
 * @param target may be {@code null}, msg will not be sent
 */
void action(char[] target, char[] msg, int offset, int len);
/**
 * @param target may be {@code null}, msg will not be sent
 */
void notice(char[] target, char[] msg);
/**
 * @param target may be {@code null}, msg will not be sent
 */
void notice(char[] target, char[] msg, int offset, int len);
/**
 * has no effect when the channel is already joined
 * @param channel channel to join
 */
void join(char[] channel);
/**
 * don't use for privmsg, use {@link #privmsg} or {@link action} for that
 *
 * should not include CRLF
 */
void send_raw(char[] buf, int offset, int len);
void log_error(Throwable t, String message);
void log_error(String message);
void log_warn(Throwable t, String message);
void log_warn(String message);
/**
 * Run given function on the main thread. Use this when trying to do anna stuff from a non-main
 * thread.
 */
void sync_exec(Runnable func);

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
}
