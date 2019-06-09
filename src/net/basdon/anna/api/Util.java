// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package net.basdon.anna.api;

public class Util
{
/**
 * check if buffer starts with a given string, similar to {@link String#startsWith(String)}
 */
public static boolean startsWith(char[] buf, int buflen, String startsWith)
{
	char[] chars = startsWith.toCharArray();
	if (buflen < chars.length) {
		return false;
	}
	if (chars.length == 0) {
		return true;
	}
	for (int i = 0; i < chars.length; i++) {
		if (buf[i] != chars[i]) {
			return false;
		}
	}
	return true;
}

public static int indexOf(char[] buf, int pos, int buflen, char needle)
{
	for (int i = pos; i < buflen; i++) {
		if (buf[i] == needle) {
			return i;
		}
	}
	return -1;
}
}
