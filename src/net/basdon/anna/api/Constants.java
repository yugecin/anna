// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package net.basdon.anna.api;

public class Constants
{
public static final char
	CTRL_BOLD = 2,
	CTRL_COLOR = 3,
	CTRL_ITALIC = 9,
	CTRL_STRIKETHROUGH = 13,
	CTRL_RESET = 15,
	CTRL_REVERSE = 22,
	CTRL_UNDERLINE = 21,
	CTRL_UNDERLINE2 = 31;

public static final char[]
	CMD_MODE = {'M', 'O', 'D', 'E'},
	CMD_JOIN = {'J', 'O', 'I', 'N'},
	CMD_PART = {'P', 'A', 'R', 'T'},
	CMD_PRIVMSG = {'P', 'R', 'I', 'V', 'M', 'S', 'G' };
}
