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

/**
 * find occurrences of {@code needle} and put the offsets in {@code occurrences}
 * @param buf buffer to search
 * @param off start offset to search for {@code needle}
 * @param to max offset to search to (exclusive)
 * @param needle character to find
 * @param occurrences array that will be filled with offsets where {@code needle} is found
 * @param max max amount of occurrences to find, should be at least the size of
 *        the {@code occurences} array
 * @return amount of times the needle was find, is {@code 0}-{@code max}
 */
public static
int occurrences(char[] buf, int off, int to, char needle, int[] occurrences, int max)
{
	int c = 0;
	while (off < to) {
		if (buf[off] == needle) {
			if (max == c) {
				break;
			}
			occurrences[c++] = off;
		}
		off++;
	}
	return c;
}

/**
 * @param to exclusive
 */
public static
boolean strcmp(char[] a, int offset, int to, char...b)
{
	if (to - offset != b.length) {
		return false;
	}
	for (int i = 0; i < b.length; i++, offset++) {
		if (a[offset] != b[i]) {
			return false;
		}
	}
	return true;
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

/**
 * @return new offset after inserted data
 */
public static
int set(char[] buf, int offset, char...data)
{
	for (int i = 0; i < data.length; i++, offset++) {
		buf[offset] = data[i];
	}
	return offset;
}
}
