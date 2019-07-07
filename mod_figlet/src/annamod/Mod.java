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
static final int DEFAULT_CMD_DELAY = 10000;
static final int DEFAULT_LINE_DELAY = 450;
static final int DEFAULT_UNDELAYED_LINE_AMOUNT = 3;
static final String DEFAULT_NOFLOOD_MSG = "don't make me flood";
static final String CONF_CMD_DELAY = "command.delay";
static final String CONF_UNDELAYED_LINE_AMOUNT = "undelayed.line.amount";
static final String CONF_LINE_DELAY = "line.delay";
static final String CONF_NOFLOOD_MSG = "noflood.message";
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
	Properties dc = defaultconf = new Properties();
	dc.setProperty(CONF_CMD_DELAY, String.valueOf(DEFAULT_CMD_DELAY));
	dc.setProperty(CONF_LINE_DELAY, String.valueOf(DEFAULT_LINE_DELAY));
	dc.setProperty(CONF_UNDELAYED_LINE_AMOUNT, String.valueOf(DEFAULT_UNDELAYED_LINE_AMOUNT));
	dc.setProperty(CONF_NOFLOOD_MSG, DEFAULT_NOFLOOD_MSG);
}

byte[] font;
IAnna anna;
long nextinvoc;
long lastserve;
int totalserves;
int gayserves;
int floodprotect;
int delay = DEFAULT_CMD_DELAY;
int line_delay = DEFAULT_LINE_DELAY;
int undelayed_line_amount = DEFAULT_UNDELAYED_LINE_AMOUNT;
char[] nofloodmsg = DEFAULT_NOFLOOD_MSG.toCharArray();
SendThread send_thread;

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
public
Properties get_default_conf()
{
	return defaultconf;
}

@Override
public boolean on_enable(IAnna anna)
{
	this.anna = anna;

	if (!load_font()) {
		return false;
	}

	anna.load_mod_conf(this);
	return true;
}

@Override
public
void config_loaded(Config conf)
{
	this.delay = conf.getInt(CONF_CMD_DELAY, 2000, Integer.MAX_VALUE);
	this.line_delay = conf.getInt(CONF_LINE_DELAY, 200, 2000);
	this.undelayed_line_amount = conf.getInt(CONF_UNDELAYED_LINE_AMOUNT, 1, charheight);
	String nofloodmsg = conf.getStr(DEFAULT_NOFLOOD_MSG);
	if (nofloodmsg != null) {
		this.nofloodmsg = nofloodmsg.toCharArray();
	}
}

@Override
public
void on_disable()
{
	this.font = null;
	if (this.send_thread != null && this.send_thread.isAlive()) {
		this.send_thread.interrupt();
		try {
			this.send_thread.join(3000L);
		} catch (InterruptedException e) {
		}
	}
}

@Override
public
void print_stats(Output out)
throws IOException
{
	out.print(" serves: " + totalserves + "\n");
	out.print(" plain/gay: " + (totalserves - gayserves) + "/" + gayserves + "\n");
	out.print(" floodprotects: " + floodprotect + "\n");
	if (lastserve > 0) {
		out.print(" lastserve: " + format_time(lastserve) + "\n");
	}
}

@Override
public
boolean on_command(User user, char[] target, char[] replytarget,
                   char[] message, char[] cmd, char[] params)
{
	boolean rainbow = false;
	if (strcmp(cmd, 'f','i','g','l','e','t') ||
		(strcmp(cmd, 'f','i','g','l','e','t','g','a','y') && (rainbow = true)))
	{
		if (this.nextinvoc >= System.currentTimeMillis()) {
			if (this.nofloodmsg != null) {
				this.anna.privmsg(replytarget, nofloodmsg);
			}
			floodprotect++;
		} else if (params != null && params.length > 0) {
			this.totalserves++;
			char[][] result = new char[charheight][maxlen];
			int[] len = this.do_figlet(result, params);
			if (rainbow) {
				char[][] output = new char[charheight][maxlen];
				this.rainbowify(output, result, len);
				result = output;
				this.gayserves++;
			}
			int delay = this.delay;
			int sentlines = 0;
			for (int i = 0; i < charheight; i++) {
				if (len[i] > 0) {
					if (sentlines++ >= this.undelayed_line_amount) {
						SendThread t = this.send_thread = new SendThread();
						t.anna = this.anna;
						t.currentline = i;
						t.len = len;
						t.result = result;
						t.target = replytarget;
						t.start();
						delay += (charheight - i) * this.line_delay;
						break;
					}
					this.anna.privmsg(replytarget, result[i], 0, len[i]);
				}
			}
			this.nextinvoc = (this.lastserve = System.currentTimeMillis()) + delay;
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
int[] do_figlet(char[][] result, char[] text)
{
	int[] len = new int[charheight];
	int x = 0;
	int lastspace = 0;
	for (int i = 0, l = text.length; i < l; i++) {
		char c = text[i];
		if(c < 32 || c > 136) c = 32;
		c -= 32;
		if (c == 0) {
			lastspace = 2;
		}
		x = this.append_char(x, len, c, lastspace == 0, result);
		if (--lastspace < 0) {
			lastspace = 0;
		}
	}
	return len;
}

int append_char(int x, int[] len, int charindex, boolean do_overlap, char[][] result)
{
	int start = 0;
	for (int i = charindex - 1; i >= 0; i--) {
		start += charwidth[i] * charheight;
	}
	int cw = charwidth[charindex];
	if (do_overlap) {
		int overlap = -1;
		x++;
		out:
		do {
			overlap++;
			if (overlap > cw) {
				overlap = cw;
			}
			x--;
			if (x >= maxlen) {
				break;
			}
			for (int i = 0; i < charheight; i++) {
				for (int o = 0; o < overlap; o++) {
					char a = result[i][x + o];
					char b = (char) this.font[start + o + cw * i];
					if (a > ' ' && b > ' ') {
						break out;
					}
				}
			}
		} while (x > 0);
	}
	x--;
	for (int j = 0; j < cw; j++) {
		if (x + 1 >= maxlen) {
			break;
		}
		x++;
		for (int i = 0; i < charheight; i++) {
			char a = result[i][x];
			char b = (char) this.font[start + j + cw * i];
			if ((a > ' ' && b == ' ') ||
				(b == '|' && (a == ')' || a == '/' || a == '\\') || a == '<') ||
				(b == '_' && (a == '|' || a == ')' || a == '\\')))
			{
				b = a;
			}
			result[i][x] = b;
			if (b != ' ') {
				len[i] = x + 1;
			}
		}
	}
	return x + 1;
}

/**
 * Adds rainbow colors to the output
 *
 * @param output
 * @param input
 * @param len array of line lengths of the input, will be changed to reflect line lengths of output
 */
void rainbowify(char[][] output, char[][] input, int[] len)
{
	char[] colors1 = { '0', '0', '0', '0', '1', '0' };
	char[] colors2 = { '4', '7', '8', '3', '2', '6' };
	for (int line = 0; line < charheight; line++) {
		int color = line % colors1.length;
		char[] i = input[line];
		char[] o = output[line];
		int j = 0;
		int k = 0;
		while (k < o.length - 4 && j < len[line]) {
			boolean c = false;
			for (int z = 0; z < 2; z++) {
				char nc = i[j++];
				if (!c && nc != ' ') {
					o[k++] = Constants.CTRL_COLOR;
					o[k++] = colors1[color];
					o[k++] = colors2[color];
					c = true;
				}
				o[k++] = nc;
				if (j >= len[line]) {
					break;
				}
			}
			color = ++color % colors1.length;
		}
		len[line] = k;
	}
}

private static class SendThread extends Thread
{
IAnna anna;
char[] target;
char[][] result;
int[] len;
int currentline;

@Override
public
void run()
{
	while (currentline < charheight) {
		if (len[currentline] > 0) {
			try {
				Thread.sleep(DEFAULT_LINE_DELAY);
			} catch (InterruptedException e) {
				return;
			}
			this.anna.sync_exec(() -> {
				anna.privmsg(target, result[currentline], 0, len[currentline]);
			});
		}
		currentline++;
	}
}
} /*SendThread*/
} /*Mod*/
