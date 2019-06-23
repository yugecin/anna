// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package net.basdon.anna.api;

import java.io.IOException;
import java.util.Properties;

public interface IAnna
{
/**
 * Load the config file associated with this mod.
 * Config file will be loaded from {@code conf/<modname>.properties}
 *
 * @param requester the mod that wants to load its config
 * @param defaults default settings or {@code null}
 * @return config or {@code null} if config does not exist or an error occured while reading
 *         the file.
 */
Config load_mod_conf(IMod requester, Properties defaults);
/**
 * check if given user is listed as an Anna owner in the config
 */
boolean is_owner(User user);
/**
 * @param target may be {@code null}, msg will not be sent
 */
void privmsg(char[] target, char[] msg);
/**
 * @param target may be {@code null}, msg will not be sent
 */
void action(char[] target, char[] msg);
/**
 * don't use for privmsg, use {@link #privmsg} or {@link action} for that
 *
 * should not include CRLF
 */
void send_raw(char[] buf, int offset, int len);
void log_error(Throwable t, String message);
void log_error(String message);
void log_warn(String message);

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
