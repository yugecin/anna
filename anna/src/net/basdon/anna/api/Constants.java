// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package net.basdon.anna.api;

public class Constants
{
public static final char[] EMPTY_CHAR_ARR = new char[0];

public static final char
	CTRL_BOLD = 2,
	CTRL_COLOR = 3,
	CTRL_ITALIC = 9,
	CTRL_STRIKETHROUGH = 13,
	CTRL_RESET = 15,
	CTRL_UNDERLINE = 21,
	CTRL_REVERSE = 22,
	CTRL_UNDERLINE2 = 31;

public static final char[]
	CMD_MODE = {'M', 'O', 'D', 'E'},
	CMD_JOIN = {'J', 'O', 'I', 'N'},
	CMD_PART = {'P', 'A', 'R', 'T'},
	CMD_QUIT = {'Q', 'U', 'I', 'T'},
	CMD_NICK = {'N', 'I', 'C', 'K'},
	CMD_KICK = {'K', 'I', 'C', 'K'},
	CMD_TOPIC = {'T', 'O', 'P', 'I', 'C'},
	CMD_PRIVMSG = {'P', 'R', 'I', 'V', 'M', 'S', 'G' };

public static final int
	RPL_ISUPPORT = 005, // https://tools.ietf.org/html/draft-brocklesby-irc-isupport-03
	RPL_NAMREPLY = 353,
	RPL_ENDOFNAMES = 366,
	RPL_ENDOFMOTD = 376,
	ERR_NOMOTD = 422,
	ERR_NICKNAMEINUSE = 422;

public static final int
	COL_WHITE = 0,
	COL_BLACK = 1,
	COL_BLUE = 2,
	COL_GREEN = 3,
	COL_RED = 4,
	COL_BROWN = 5,
	COL_MAGENTA = 6,
	COL_ORANGE = 7,
	COL_YELLOW = 8,
	COL_LIME = 9,
	COL_TEAL = 10,
	COL_CYAN = 11,
	COL_LIGHTBLUE = 12,
	COL_PINK = 13,
	COL_GREY = 14,
	COL_LIGHTGREY = 15;

public static final String
	SCOL_WHITE = "00",
	SCOL_BLACK = "01",
	SCOL_BLUE = "02",
	SCOL_GREEN = "03",
	SCOL_RED = "04",
	SCOL_BROWN = "05",
	SCOL_MAGENTA = "06",
	SCOL_ORANGE = "07",
	SCOL_YELLOW = "08",
	SCOL_LIME = "09",
	SCOL_TEAL = "10",
	SCOL_CYAN = "11",
	SCOL_LIGHTBLUE = "12",
	SCOL_PINK = "13",
	SCOL_GREY = "14",
	SCOL_LIGHTGREY = "15";

public static final String[] COLORMAP = {
	SCOL_WHITE,
	SCOL_BLACK,
	SCOL_BLUE,
	SCOL_GREEN,
	SCOL_RED,
	SCOL_BROWN,
	SCOL_MAGENTA,
	SCOL_ORANGE,
	SCOL_YELLOW,
	SCOL_LIME,
	SCOL_TEAL,
	SCOL_CYAN,
	SCOL_LIGHTBLUE,
	SCOL_PINK,
	SCOL_GREY,
	SCOL_LIGHTGREY,
};
}
