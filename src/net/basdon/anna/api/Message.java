// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package net.basdon.anna.api;

import net.basdon.anna.internal.Log;

import static java.lang.System.arraycopy;
import static net.basdon.anna.api.Util.*;

/**
 * an IRC message
 * {@link https://tools.ietf.org/html/rfc1459#section-2.3}
 */
public class Message
{
public char[] prefix;
/**
 * command number or {@code 0} if it was not a three digit number
 */
public int cmdnum;
public char[] cmd;
public int paramc;
public char[][] paramv = new char[15][];

/**
 * @param buf buffer
 * @param buflen length of buffer, should be at least {@code 1}
 * @return an instance of {@link Message} or {@code null} if the message could not be parsed
 */
public static
Message parse(char[] buf, int buflen)
{
	Message msg = new Message();

	int parsepos = 0;
	char c;

	// prefix
	if (buf[0] == ':') {
		int space_pos = indexOf(buf, 0, buflen, ' ');
		if (space_pos != -1) {
			msg.prefix = new char[space_pos];
			arraycopy(buf, 0, msg.prefix, 0, space_pos);
			parsepos = space_pos;
			while (buf[parsepos] == ' ') {
				parsepos++;
				if (parsepos >= buflen) {
					Log.warn("unexpected end of message: "
					         + new String(buf, 0, buflen));
					return null;
				}
			}
		}
	}

	// command
	int cmdstart = parsepos;
	int cmdlen = 0;
	boolean canbenum = true;
	while ((c = buf[parsepos]) != ' ') {
		parsepos++;
		if (parsepos >= buflen) {
			Log.warn("unexpected end of message: " + new String(buf, 0, buflen));
			return null;
		}
		cmdlen++;
		if (c < '0' || '9' < c || cmdlen > 3) {
			canbenum = false;
			msg.cmdnum = 0;
		} else if (canbenum) {
			switch (cmdlen) {
			case 1: msg.cmdnum = (c - '0') * 100; break;
			case 2: msg.cmdnum += (c - '0') * 10; break;
			case 3: msg.cmdnum += (c - '0'); break;
			}
		}
	}
	msg.cmd = new char[cmdlen];
	arraycopy(buf, cmdstart, msg.cmd, 0, cmdlen);

	// parameters
	int paramstart = -1;
	for (;;) {
		boolean isend;
		if ((isend = parsepos >= buflen) || (c = buf[parsepos]) == ' ') {
			if (paramstart != -1) {
				if (msg.paramc >= msg.paramv.length) {
					Log.warn("too many params in message: "
					         + new String(buf, 0, buflen));
					return null;
				}
				int len = parsepos - paramstart;
				char[] par = msg.paramv[msg.paramc++] = new char[len];
				arraycopy(buf, paramstart, par, 0, len);
			}
			paramstart = -1;
			if (isend) {
				break;
			}
		}
		if (paramstart == -1 && c != ' ') {
			if (c == ':') {
				if (msg.paramc >= msg.paramv.length) {
					Log.warn("too many params in message: "
					         + new String(buf, 0, buflen));
					return null;
				}
				int len = buflen - parsepos;
				char [] par = msg.paramv[msg.paramc++] = new char[len];
				arraycopy(buf, parsepos, par, 0, len);
				break;
			}
			paramstart = parsepos;
		}
		parsepos++;
	}

	return msg;
}
}
