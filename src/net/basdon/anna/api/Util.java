// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package net.basdon.anna.api;

public class Util
{
/**
 * check if buffer starts with a given string, similar to {@link String#startsWith(String)}
 */
public static
boolean startsWith(char[] buf, int buflen, String startsWith)
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

public static
int indexOf(char[] buf, int pos, int buflen, char needle)
{
	for (int i = pos; i < buflen; i++) {
		if (buf[i] == needle) {
			return i;
		}
	}
	return -1;
}

public static
boolean strcmp(@Nullable char[] a, @Nullable char...b)
{
	if (a == null || b == null || a.length != b.length) {
		return false;
	}
	for (int i = 0; i < a.length; i++) {
		if (a[i] != b[i]) {
			return false;
		}
	}
	return true;
}

public static
void set(char[] buf, int offset, char...data)
{
	for (int i = 0; i < data.length; i++) {
		buf[offset + i] = data[i];
	}
}
}
