// Copyright 2021-2022 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package net.basdon.anna.internal;

import static net.basdon.anna.api.Constants.*;

/**
 * Parsing copied from mod_log's LogWriter class.
 */
public
class RichMessage
{
private static final String[] COLORS = {
	"FFFFFF", "000000", "00007F", "009300", "FF0000", "7F0000", "9C009C",
	"FC7F00", "FFFF00", "00FC00", "009393", "00FFFF", "0000FC", "FF00FF",
	"7F7F7F", "D2D2D2"
};
private static final int
	STYLE_FG = 0,
	STYLE_BG = 1,
	STYLE_BOLD = 2,
	STYLE_UNDERLINE = 3,
	STYLE_ITALIC = 4,
	STYLE_STRIKETHROUGH = 5,
	STYLE_REVERSE = 6;

private static boolean has_span;
private static String[] styles = new String[7];

static
RichMessage parse(char[] raw)
{
	StringBuilder rich = new StringBuilder(1024);
	StringBuilder plain = new StringBuilder(512);

	for (int i = 0; i < styles.length; i++) {
		styles[i] = null;
	}

	rich.append("<html>");

	int color_parsing = 0, fg = 0, bg = 0;
	int len = raw.length;
	int off = 0;
	while (off < len) {
		char c = raw[off++];
		if (c == 1) {
			// for PRIVMSG target :\1ACTION says hi\1
			color(13);
			add_formatting(rich);
			continue;
		}
		switch (color_parsing)
		{
		case 1:
			if ('0' <= c && c <= '9') {
				fg = c - '0';
				color_parsing = 2;
				continue;
			}
			break;
		case 2:
			if (c == ',') {
				color_parsing = 4;
				continue;
			}
			if ('0' <= c && c <= '9') {
				fg = fg * 10 + c - '0';
				color_parsing = 3;
				continue;
			}
			color(fg);
			add_formatting(rich);
			break;
		case 3:
			if (c == ',') {
				color_parsing = 4;
				continue;
			}
			color(fg);
			add_formatting(rich);
			break;
		case 4:
			if ('0' <= c && c <= '9') {
				bg = c - '0';
				color_parsing = 5;
				continue;
			}
			color(fg);
			add_formatting(rich);
			rich.append(',');
			break;
		case 5:
			if ('0' <= c && c <= '9') {
				bg = bg * 10 + c - '0';
				color_parsing = 0;
				color(fg, bg);
				add_formatting(rich);
				continue;
			}
			color(fg, bg);
			add_formatting(rich);
			break;
		}
		color_parsing = 0;
		switch (c)
		{
		case CTRL_BOLD:
			if (styles[STYLE_BOLD] != null) {
				styles[STYLE_BOLD] = null;
			} else {
				styles[STYLE_BOLD] = "font-weight:bold";
			}
			add_formatting(rich);
			continue;
		case CTRL_COLOR:
			color_parsing = 1;
			continue;
		case CTRL_ITALIC:
			if (styles[STYLE_ITALIC] != null) {
				styles[STYLE_ITALIC] = null;
			} else {
				styles[STYLE_ITALIC] = "font-style:italic";
			}
			add_formatting(rich);
			continue;
		case CTRL_STRIKETHROUGH:
			if (styles[STYLE_STRIKETHROUGH] != null) {
				styles[STYLE_STRIKETHROUGH] = null;
			} else {
				styles[STYLE_STRIKETHROUGH] = "text-decoration:line-through";
			}
			add_formatting(rich);
			continue;
		case CTRL_RESET:
			for (int i = 0; i < styles.length; i++) {
				styles[i] = null;
			}
			add_formatting(rich);
			continue;
		case CTRL_UNDERLINE:
		case CTRL_UNDERLINE2:
			if (styles[STYLE_UNDERLINE] != null) {
				styles[STYLE_UNDERLINE] = null;
			} else {
				styles[STYLE_UNDERLINE] = "text-decoration:underline";
			}
			add_formatting(rich);
			continue;
		case CTRL_REVERSE:
			if (styles[STYLE_REVERSE] != null) {
				styles[STYLE_REVERSE] = null;
			} else {
				styles[STYLE_REVERSE] = "color:#FFF;background:#000";
			}
			add_formatting(rich);
			continue;
		}
		plain.append(c);
		if (c < ' ' || c == '&' || c == '<' || c == '>' || c > '~') {
			rich.append("&#" + String.valueOf((int) c) + ";");
		} else {
			rich.append(c);
		}
	}

	rich.append("</html>");
	return new RichMessage(plain.toString(), rich.toString());
}

private static
void color(int fg)
{
	if (fg < 0 || 15 < fg) {
		fg = COL_BLACK;
	}
	styles[STYLE_FG] = "color:#" + COLORS[fg];
}

private static
void color(int fg, int bg)
{
	if (fg < 0 || 15 < fg) {
		fg = COL_BLACK;
	}
	if (bg < 0 || 15 < bg) {
		styles[STYLE_BG] = null;
		color(fg);
		return;
	}
	styles[STYLE_FG] = "color:#" + COLORS[fg];
	styles[STYLE_BG] = "background:#" + COLORS[bg];
}

private static
void add_formatting(StringBuilder rich)
{
	if (has_span) {
		rich.append("</span>");
		has_span = false;
	}
	out:
	{
		for (int i = 0; i < styles.length; i++) {
			if (styles[i] != null) {
				break out;
			}
		}
		return;
	}
	rich.append("<span style=\"");
	for (int i = 0; i < styles.length; i++) {
		if (styles[i] != null) {
			switch (i) {
			// combine underline and line-through when needed
			case STYLE_UNDERLINE: if (styles[STYLE_STRIKETHROUGH] != null) {
				rich.append("text-decoration:line-through underline;");
				continue;
			}
			case STYLE_STRIKETHROUGH: if (styles[STYLE_UNDERLINE] != null) {
				continue;
			}
			// prevent fg and bg if reverse is active
			case STYLE_FG: if (styles[STYLE_REVERSE] != null) {
				continue;
			}
			case STYLE_BG: if (styles[STYLE_REVERSE] != null) {
				continue;
			}
			}
			rich.append(styles[i]);
			rich.append(';');
		}
	}
	rich.append("\">");
	has_span = true;
}

public final String plain;
public final String rich;

private
RichMessage(String plain, String rich)
{
	this.plain = plain;
	this.rich = rich;
}
}
