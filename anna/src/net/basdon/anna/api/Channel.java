// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package net.basdon.anna.api;

import java.util.ArrayList;

public abstract class Channel
{
public final char[] name;
public final ArrayList<ChannelUser> userlist;

protected Channel(char[] name)
{
	this.name = name;
	this.userlist = new ArrayList<>();
}
}