// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package net.basdon.anna.api;

import java.util.ArrayList;

import static net.basdon.anna.api.Util.*;

public abstract class Channel
{
public final char[] name;
public final ArrayList<ChannelUser> userlist;

protected
Channel(char[] name)
{
	this.name = name;
	this.userlist = new ArrayList<>();
}

/**
 * @return found {@link ChannelUser} or {@code null}
 */
public
ChannelUser find_user(char[] nick)
{
	int i = userlist.size();
	while (i-- > 0) {
		ChannelUser usr = this.userlist.get(i);
		if (strcmp(nick, usr.nick)) {
			return usr;
		}
	}
	return null;
}
}