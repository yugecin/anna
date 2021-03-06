// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package net.basdon.anna.api;

import java.io.Flushable;
import java.util.Date;

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

/**
 * @param a may be {@code null}
 * @param b may be {@code null}
 */
public static
boolean strcmp(char[] a, char...b)
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

/**
 * @return idex of {@code needle} in {@code array}, or {@code -1} if not in array
 */
public static
int array_idx(char[] array, char needle)
{
	for (int i = 0; i < array.length; i++) {
		if (needle == array[i]) {
			return i;
		}
	}
	return -1;
}

public static
char[] chars(StringBuilder sb)
{
	int len = sb.length();
	char[] buf = new char[len];
	sb.getChars(0, len, buf, 0);
	return buf;
}

/**
 * Also attempts to flush if ac is {@link Flushable}
 *
 * @param ac may be {@code null}
 */
public static
void close(AutoCloseable ac)
{
	if (ac != null) {
		if (ac instanceof Flushable) {
			try {
				((Flushable) ac).flush();
			} catch (Throwable ignored) {}
		}
		try {
			ac.close();
		} catch (Throwable ignored) {}
	}
}

/**
 * formats time as time string + days/hours/minutes/seconds since that stamp
 * @param millis epoch timestamp
 */
public static
String format_time(long millis)
{
	StringBuilder sb = new StringBuilder();
	sb.append(new Date(millis).toString());
	millis = System.currentTimeMillis() - millis;
	sb.append(" (");
	millis /= 1000;
	boolean y = false;
	long t;
	if (millis > 24 * 3600) {
		t = millis / 3600 / 24;
		sb.append(t).append('d');
		millis %= 24 * 3600;
		y = true;
	}
	if (y || millis > 3600) {
		t = millis / 3600;
		if (t < 10) sb.append('0');
		sb.append(t).append('h');
		millis %= 3600;
		y = true;
	}
	if (y || millis > 60) {
		t = millis / 60;
		if (t < 10) sb.append('0');
		sb.append(t).append('m');
		millis %= 60;
		y = true;
	}
	if (y && millis < 10) sb.append('0');
	sb.append(millis).append('s');
	sb.append(")");
	return sb.toString();
}

/**
 * Makes a mode string from parameters given to {@link IMod#on_channelmodechange}
 */
public static
StringBuilder make_modestr(int changec, char[] signs, char[] modes, char[] types, char[][] params)
{
	StringBuilder sb = new StringBuilder();
	StringBuilder sb2 = new StringBuilder();
	char lastsign = 0;
	for (int i = 0; i < changec; i++) {
		if (signs[i] != lastsign) {
			sb.append(lastsign = signs[i]);
		}
		sb.append(modes[i]);
		char t = types[i];
		if (t != 'd' && (t != 'c' || lastsign == '+')) {
			sb2.append(' ').append(params[i]);
		}
	}
	sb.append(sb2);
	return sb;
}

/**
 * Check if a channel user has {@code wantedmode} or higher permissions.
 *
 * @param user user to check
 * @param modes all modes, use {@link IAnna#get_user_channel_modes()}. These should be sorted from
 *              highest permissions to lowest.
 * @param wantedmode mode to check against (not the prefix)
 * @return {@code true} if given user has {@code wantedmode} or a higher level
 */
public static
boolean has_user_mode_or_higher(ChannelUser user, char[] modes, char wantedmode)
{
	int applicable_modes = 1;
	boolean mode_exists = false;
	for (int i = 0; i < modes.length; i++) {
		if (modes[i] == wantedmode) {
			mode_exists = true;
			break;
		}
		applicable_modes++;
	}
	if (!mode_exists) {
		return false;
	}
	for (int i = 0; i < user.modec; i++) {
		for (int j = 0; j < applicable_modes; j++) {
			if (user.modev[i] == modes[j]) {
				return true;
			}
		}
	}
	return false;
}
}
