// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package net.basdon.anna.api;

import static java.lang.System.arraycopy;
import static net.basdon.anna.api.Util.*;

public class User
{
public static final User[] EMPTY_ARRAY = new User[0];

/**
 * @return {@code null} if couldn't be parsed
 */
public static
User parse(char[] user, int from, int to)
{
	User u = new User();
	int len;
	int at = indexOf(user, from, to, '@');
	if (at == -1 || at == to - 1) {
		return null;
	}
	int ex = indexOf(user, from, to, '!');
	if (at == ex + 1) {
		return null;
	}
	if (ex == -1) {
		u.nick = new char[len = at - from];
		arraycopy(user, from, u.nick, 0, len);
	} else {
		u.nick = new char[len = ex - from];
		arraycopy(user, from, u.nick, 0, len);
		u.name = new char[len = at - ex - 1];
		arraycopy(user, ex + 1, u.name, 0, len);
	}
	u.host = new char[len = to - at - 1];
	arraycopy(user, at + 1, u.host, 0, len);
	return u;
}

public char[] nick;
/**
 * name or {@code null} if not present
 */
public char[] name;
public char[] host;

public
boolean matches(User o)
{
	return match(this.nick, o.nick) && match(this.name, o.name) && match(this.host, o.host); 
}

private static
boolean match(char[] a, char[] b)
{
	return
		(a == null && b == null) ||
		(a != null && a.length == 1 && a[0] == '*') ||
		strcmp(a, b);
}
}
