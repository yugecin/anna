// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package net.basdon.anna.internal;

public class ChannelUser
{
static int maxmodes;

char[] nick;
int modec;
char[] modev;

ChannelUser(char[] nick)
{
	this.nick = nick;
	this.modev = new char[maxmodes];
}

void mode_add(char mode)
{
	for (int i = 0; i < this.modec; i++) {
		if (this.modev[i] == mode) {
			return;
		}
	}
	this.modev[this.modec++] = mode;
}

void mode_remove(char mode)
{
	for (int i = 0; i < this.modec; i++) {
		if (this.modev[i] == mode) {
			int lastidx = this.modec - 1;
			if (i < lastidx) {
				this.modev[i] = this.modev[lastidx];
			}
			this.modec--;
			return;
		}
	}
}
}
