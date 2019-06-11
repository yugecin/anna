// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package net.basdon.anna.internal;

import java.util.ArrayList;

import static java.lang.System.arraycopy;
import static net.basdon.anna.api.Util.*;

public class Channel
{
final char[] channel;
final ArrayList<ChannelUser> userlist;

Channel(char[] channel)
{
	this.channel = channel;
	this.userlist = new ArrayList<>();
}

ChannelUser get_or_add_user(char[] nick)
{
	int i = this.userlist.size();
	while (i-- > 0) {
		ChannelUser usr = this.userlist.get(i);
		if (strcmp(nick, usr.nick)) {
			return usr;
		}
	}
	ChannelUser usr = new ChannelUser(nick);
	this.userlist.add(usr);
	return usr;
}

/**
 * inform that mode has been changed
 * @param anna Anna instance
 * @param paramv params from the MODE message (channel should be at index {@code 0}, then mode, ...)
 * @param paramc length of {@code paramv}
 */
void mode_changed(Anna anna, char[][] paramv, int paramc)
{
	boolean need_user_update = false;

	int paramidx = 1;
	char[] change = paramv[paramidx++];
	char addremove = '+';
	for (int i = 0; i < change.length; i++) {
		char c = change[i];
		if (c == '+' || c == '-') {
			addremove = c;
			continue;
		}
		if (array_idx(anna.chanmodes_a, c) != -1) {
			// channel mode list with param
			paramidx++;
		} else if (array_idx(anna.chanmodes_b, c) != -1) {
			// channel mode with param
			paramidx++;
		} else if (array_idx(anna.chanmodes_c, c) != -1) {
			// channel mode with param when set
			if (addremove == '+') {
				paramidx++;
			}
		} else if (array_idx(anna.chanmodes_d, c) != -1) {
			// channel mode without param
		} else if (array_idx(anna.modes, c) != -1) {
			if (paramidx >= paramc) {
				Log.warn("not enough mode params while processing mode " + c);
			} else {
				char[] nick = paramv[paramidx];
				int j = this.userlist.size();
				while (j-- > 0) {
					ChannelUser usr = this.userlist.get(j);
					if (strcmp(nick, usr.nick)) {
						if (addremove == '-') {
							usr.mode_remove(c);
							need_user_update = true;
						} else {
							usr.mode_add(c);
						}
						break;
					}
				}
			}
		} else {
			Log.warn("unknown mode: " + addremove + c);
			paramidx++;
		}
	}

	if (need_user_update) {
		// if a user was +ov, Anna^ could only see +o upon entering the channel. if that
		// user now lost +o, they still have +v but Anna^ does not know, thus request NAMES
		// again
		char[] buf = new char[6 + paramv[0].length];
		set(buf, 0, 'N','A','M','E','S',' ');
		arraycopy(paramv[0], 0, buf, 6, paramv[0].length);
		anna.send_raw(buf, 0, buf.length);
	}
}
}
