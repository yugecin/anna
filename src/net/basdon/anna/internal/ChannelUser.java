// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package net.basdon.anna.internal;

public class ChannelUser
{
static int maxmodes;

char[] name;
int modec;
char[] modev;

ChannelUser(char[] name)
{
	this.name = name;
	this.modev = new char[maxmodes];
}

void add_mode(char mode)
{
	for (int i = 0; i < this.modec; i++) {
		if (this.modev[i] == mode) {
			return;
		}
	}
	this.modev[this.modec++] = mode;
}
}
