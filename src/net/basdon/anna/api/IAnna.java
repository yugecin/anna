// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package net.basdon.anna.api;

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
boolean is_owner(@Nullable User user);
void privmsg(char[] target, char[] msg);
void action(char[] target, char[] msg);
/**
 * should not include CRLF
 */
void send_raw(String msg);
/**
 * should not include CRLF
 */
void send_raw(char[] buf, int offset, int len);
}
