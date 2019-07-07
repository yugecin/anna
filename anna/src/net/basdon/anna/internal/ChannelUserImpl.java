// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package net.basdon.anna.internal;

import net.basdon.anna.api.ChannelUser;

class ChannelUserImpl extends ChannelUser
{
static int maxmodes;

ChannelUserImpl(char[] nick)
{
	this.nick = nick;
	this.modev = new char[maxmodes];
}

void mode_add(char mode, char[] modes, char[] prefixes)
{
	this.mode_modify(mode, modes, prefixes, true);
}

void mode_remove(char mode, char[] modes, char[] prefixes)
{
	this.mode_modify(mode, modes, prefixes, false);
}

private
void mode_modify(char mode, char[] modes, char[] prefixes, boolean is_add)
{
	boolean[] hasmode = new boolean[maxmodes];
	int j = 0;
	for (int i = 0; i < maxmodes; i++) {
		char m = modes[i];
		hasmode[i] = mode == m && is_add;
		if (j < this.modec && this.modev[j] == m) {
			hasmode[i] = m != mode || is_add;
			j++;
		}
	}
	this.modec = 0;
	this.prefix = 0;
	for (int i = 0; i < maxmodes; i++) {
		if (hasmode[i]) {
			this.modev[this.modec++] = modes[i];
			if (this.prefix == 0) {
				this.prefix = prefixes[i];
			}
		}
	}
}
}