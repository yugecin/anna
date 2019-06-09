// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package net.basdon.anna.internal;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.basdon.anna.api.Util.*;

public class Main
{
private static boolean restart, shutdown;

static boolean debug_print_in, debug_print_out;

public static void main(String[] args)
{
	int exit_code;
	try (Log _log = Log.init()) {
		exit_code = main();
	}
	System.exit(exit_code);
}

private static int main()
{
	if (!ConfigImpl.check_config_directory()) {
		Log.error("cannot create config directory "
			  + ConfigImpl.config_dir.getAbsolutePath());
		return 2;
	}
	Log.info("config dir is " + ConfigImpl.config_dir.getAbsolutePath());

	ConfigImpl conf = ConfigImpl.load("anna", Anna.default_config);
	if (conf == null) {
		Log.error("no main config, exiting");
		return 1;
	}

	if (conf.is_new) {
		Log.warn("config file " + conf.conf_file.getName()
			 + " was created, check the settings and restart, exiting");
		return 3;
	}

	debug_print_in = conf.getBool("debug.print.incoming");
	debug_print_out = conf.getBool("debug.print.outgoing");

	String host = conf.getStr("server.host");
	int port = conf.getInt("server.port");
	if (host == null || port == 0) {
		Log.warn("update 'server.host' and 'server.port' in the config");
		return 4;
	}

	Anna anna = new Anna(conf);

	boolean disconnect_after_succesful_connection = true;
	for (;;) {
		try (Socket socket = new Socket(host, port)) {
			disconnect_after_succesful_connection = true;
			CapturedWriter out;
			InputStreamReader in;

			out = new CapturedWriter(socket.getOutputStream());
			in = new InputStreamReader(socket.getInputStream(), UTF_8);

			out.print("NICK " + conf.getStr("bot.nick") + "\r\n");
			out.print("USER " + conf.getStr("bot.user") + " 0 0 :"
			          + conf.getStr("bot.userinfo") + "\r\n");

			char[] buf = new char[512];
			int pos = 0;
			boolean cr = false;
			for (;;) {
				int c = in.read();
				if (c == -1) {
					Log.warn("end of stream is reached");
					break;
				}
				if (c == '\r') {
					cr = true;
					continue;
				} else if (cr) {
					cr = false;
					if (c == '\n') {
						if (debug_print_in) {
							String msg;
							msg = new String(buf, 0, pos);
							System.out.println("<- " + msg);
						}
						if (startsWith(buf, pos, "PING")) {
							buf[1] = 'O';
							buf[pos++] = '\r';
							buf[pos++] = '\n';
							out.print(buf, 0, pos);
							pos = 0;
							continue;
						}
						pos = 0;
						continue;
					}
					buf[pos++] = '\r';
				}
				buf[pos++] = (char) c;
			}
		} catch (UnknownHostException e) {
			Log.error("unknown host, check the config", e);
			return 5;
		} catch (Exception e) {
			Log.error("socket exception", e);
		}

		if (restart) {
			restart = false;
			continue;
		}

		if (shutdown) {
			break;
		}

		int retry_timeout = conf.getInt("connection.retrytimeoutseconds", 1, 3600);

		if (disconnect_after_succesful_connection) {
			disconnect_after_succesful_connection = false;
			Log.warn("disconnected, reconnecting every " + retry_timeout + "s");
		}

		try {
			Thread.sleep(retry_timeout * 1000L);
		} catch (InterruptedException e) {
		}
	}
	return 0;
}
}

class CapturedWriter
{
private final Writer out;

CapturedWriter(OutputStream out)
{
	this.out = new BufferedWriter(new OutputStreamWriter(out, UTF_8));
}

void print(String msg) throws IOException
{
	if (Main.debug_print_out) {
		// substring to remove CRLF
		System.out.println("-> " + msg.substring(0, msg.length() - 2));
	}
	out.write(msg, 0, msg.length());
	out.flush();
}

void print(char[] buf, int offset, int len) throws IOException
{
	if (Main.debug_print_out) {
		// -2 to remove CRLF
		System.out.println("-> " + new String(buf, offset, len - 2));
	}
	out.write(buf, offset, len);
	out.flush();
}
}
