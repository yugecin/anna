// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package annamod;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import net.basdon.anna.api.IAnna.Output;
import net.basdon.anna.api.*;

import static net.basdon.anna.api.Util.*;

public class Mod implements IMod
{
static final int DEFAULT_DELAY = 10000;
static final String CONF_CMD_DELAY = "command.delay";
static final Properties defaultconf;
static final int charheight = 6, maxlen = 450;
static final int[] charwidth = {
	3, 3, 5, 10, 5, 6, 8, 3, 4, 4, 6, 7, 3, 7, 3, 6, 7, 3, 7, 7,
	8, 7, 7, 7, 7, 7, 3, 3, 4, 7, 4, 5, 9, 9, 7, 7, 7, 7, 7, 7,
	7, 5, 7, 6, 7, 8, 7, 7, 7, 7, 7, 7, 7, 7, 9, 12, 6, 7, 6, 4,
	6, 4, 4, 7, 3, 7, 7, 6, 7, 6, 5, 7, 7, 3, 5, 6, 3, 11, 7, 7,
	7, 7, 6, 5, 5, 7, 7, 10, 6, 7, 5, 5, 3, 5, 5
};

static
{
	defaultconf = new Properties();
	defaultconf.setProperty(CONF_CMD_DELAY, String.valueOf(DEFAULT_DELAY));
}

byte[] font;
IAnna anna;
long nextinvoc;
int serves;
int floodprotect;
int delay = DEFAULT_DELAY;

@Override
public
String getName()
{
	return "mod_figlet";
}

@Override
public
String getVersion()
{
	return "1";
}

@Override
public
String getDescription()
{
	return "FIGlet-ify the output of the given string";
}

@Override
public boolean on_enable(IAnna anna)
{
	this.anna = anna;

	if (!load_font()) {
		return false;
	}

	Config conf = anna.load_mod_conf(this, defaultconf);
	this.delay = conf.getInt(CONF_CMD_DELAY, 2000, Integer.MAX_VALUE);
	return true;
}

@Override
public
void on_disable()
{
	this.font = null;
}

@Override
public
void print_stats(Output output)
throws IOException
{
}

@Override
public
boolean on_command(User user, char[] target, char[] replytarget, char[] cmd, char[] params)
{
	if (strcmp(cmd, 'f','i','g','l','e','t')) {
		if (this.nextinvoc >= System.currentTimeMillis()) {
			this.anna.privmsg(replytarget, "don't make me flood".toCharArray());
			floodprotect++;
		} else if (params != null && params.length > 0) {
			char[][] result = new char[charheight][maxlen];
			int x = this.do_figlet(result, params);
			nextrow:
			for (int i = 0; i < charheight; i++) {
				int len = x;
				while (result[i][len] <= ' ') {
					if (--len == 0) {
						continue nextrow;
					}
				}
				this.anna.privmsg(replytarget, result[i], 0, len + 1);
			}
			this.nextinvoc = System.currentTimeMillis() + this.delay;
			serves++;
		}
		return true;
	}
	return false;
}

boolean load_font()
{
	try (InputStream in = Mod.class.getResourceAsStream("figletfont.txt")) {
		this.font = new byte[3505];
		int total = 0;
		do {
			int read = in.read(this.font, 0, this.font.length);
			if (read == -1) {
				throw new IOException("EOF");
			}
			total += read;
		} while (total != this.font.length);
		return true;
	} catch (IOException e) {
		return false;
	}
}

/**
 * @param result array to fill in the characters, should be of dimensions {@code charheight,maxlen}
 * @param text text to render
 * @return amount of characters filled horizontally (rows may have trailing whitespace)
 */
int do_figlet(char[][] result, char[] text)
{
	int x = 0;
	int lastspace = 0;
	for (int i = 0, l = text.length; i < l; i++) {
		char c = text[i];
		if(c < 32 || c > 136) c = 32;
		c -= 32;
		if (c == 0) {
			lastspace = 2;
		}
		x = this.append_char(x, c, lastspace == 0, result);
		if (--lastspace < 0) {
			lastspace = 0;
		}
	}
	return x;
}

int append_char(int x, int charindex, boolean do_overlap, char[][] result)
{
	int start = 0;
	for (int i = charindex - 1; i >= 0; i--) {
		start += charwidth[i] * charheight;
	}
	int cw = charwidth[charindex];
	boolean overlapped = false;
	if (do_overlap) {
		x++;
		do {
			x--;
			if (x >= maxlen) {
				break;
			}
			for (int i = 0; i < charheight; i++) {
				char a = result[i][x];
				char b = (char) this.font[start + cw * i];
				if (a > ' ' && b > ' ') {
					overlapped = true;
				}
			}
		} while (!overlapped && x > 0);
	}
	x--;
	for (int j = 0; j < cw; j++) {
		if (x + 1 >= maxlen) {
			break;
		}
		x++;
		for (int i = 0; i < charheight; i++) {
			result[i][x] = overlap(result[i][x], (char) this.font[start + j + cw * i]);
		}
	}
	return x + 1;
}

/**
 * @param a original char
 * @param b new char
 * @return the char that should be printed
 */
char overlap(char a, char b)
{
	if (a != 0 && (
			b == ' ' ||
			(b == '|' && (a == ')' || a == '/' || a == '\\') || a == '<') ||
			(b == '_' && (a == '|' || a == ')' || a == '\\'))
		))
	{
		return a;
	}
	return b;
}
}
