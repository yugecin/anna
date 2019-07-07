// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package net.basdon.anna.internal;

import net.basdon.anna.api.Channel;
import net.basdon.anna.api.ChannelUser;
import net.basdon.anna.api.User;

import static java.lang.System.arraycopy;
import static net.basdon.anna.api.Util.*;

class ChannelImpl extends Channel
{
ChannelImpl(char[] name)
{
	super(name);
}

ChannelUserImpl get_or_add_user(char[] nick)
{
	int i = this.userlist.size();
	while (i-- > 0) {
		ChannelUser usr = this.userlist.get(i);
		if (strcmp(nick, usr.nick)) {
			return (ChannelUserImpl) usr;
		}
	}
	ChannelUser usr = new ChannelUserImpl(nick);
	this.userlist.add(usr);
	return (ChannelUserImpl) usr;
}

/**
 * inform that mode has been changed
 * @param anna Anna instance
 * @param user user that did the mode change or {@code null}
 * @param paramv params from the MODE message (channel should be at index {@code 0}, then mode, ...)
 * @param paramc length of {@code paramv}
 */
void mode_changed(Anna anna, User user, char[][] paramv, int paramc)
{
	boolean need_user_update = false;

	char[] modes = anna.get_user_channel_modes();
	char[] prefixes = anna.get_user_channel_prefixes();
	char[] chan = paramv[0];
	char[] change = paramv[1];
	int paramidx = 2;
	char sign = '+';
	Anna.BufferedChanModeChange umc;
	umc = new Anna.BufferedChanModeChange(this, user, change.length - 1);
	for (int i = 0; i < change.length; i++) {
		char c = change[i];
		if (c == '+' || c == '-') {
			sign = c;
			continue;
		}
		umc.signs[umc.changec] = sign;
		umc.modes[umc.changec] = c;
		if (array_idx(anna.chanmodes_a, c) != -1) {
			// channel mode list with param
			umc.params[umc.changec] = paramv[paramidx];
			umc.types[umc.changec] = 'a';
			umc.changec++;
		} else if (array_idx(anna.chanmodes_b, c) != -1) {
			// channel mode with param
			umc.params[umc.changec] = paramv[paramidx];
			umc.types[umc.changec] = 'b';
			umc.changec++;
		} else if (array_idx(anna.chanmodes_c, c) != -1) {
			// channel mode with param when set
			if (sign == '+') {
				umc.params[umc.changec] = paramv[paramidx];
			}
			umc.types[umc.changec] = 'c';
			umc.changec++;
		} else if (array_idx(anna.chanmodes_d, c) != -1) {
			// channel mode without param
			umc.types[umc.changec] = 'd';
			umc.changec++;
			paramidx--; // (revert the ++ after this if)
		} else if (array_idx(anna.modes, c) != -1) {
			if (paramidx >= paramc) {
				Log.warn("not enough mode params while processing mode " + c);
			} else {
				char[] nick = paramv[paramidx];
				int j = this.userlist.size();
				while (j-- > 0) {
					ChannelUserImpl usr;
					usr = (ChannelUserImpl) this.userlist.get(j);
					if (strcmp(nick, usr.nick)) {
						if (sign == '-') {
							usr.mode_remove(c, modes, prefixes);
							need_user_update = true;
						} else {
							usr.mode_add(c, modes, prefixes);
						}
						umc.types[umc.changec] = 'u';
						umc.users[umc.changec] = usr;
						umc.params[umc.changec] = nick;
						umc.changec++;
						break;
					}
				}
			}
		} else {
			Log.warn("unknown mode: " + sign + c);
		}
		paramidx++;
	}

	if (need_user_update) {
		// if a user was +ov, Anna^ could only see +o upon entering the channel. if that
		// user now lost +o, they still have +v but Anna^ does not know, thus request NAMES
		// again
		char[] buf = new char[6 + chan.length];
		set(buf, 0, 'N','A','M','E','S',' ');
		arraycopy(chan, 0, buf, 6, chan.length);
		anna.send_raw(buf, 0, buf.length);

		umc.shedule(anna);
	} else {
		umc.dispatch(anna);
	}
}
}
