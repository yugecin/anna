// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package net.basdon.anna.internal;

import java.util.ArrayList;

public class Channel
{
final char[] channel;
final ArrayList<ChannelUser> userlist;

Channel(char[] channel)
{
	this.channel = channel;
	this.userlist = new ArrayList<>();
}
}
