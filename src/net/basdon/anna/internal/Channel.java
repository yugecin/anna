// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package net.basdon.anna.internal;

import java.util.ArrayList;

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

/**
 * inform that mode has been changed
 * @param anna Anna instance
 * @param paramv params from the MODE message
 * @param paramc length of {@code paramv}
 * @param paramidx start idx in {@code paramv} where the modestring starts, followed by the params
 */
void mode_changed(Anna anna, char[][] paramv, int paramc, int paramidx)
{
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
}
}
