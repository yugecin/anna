// Copyright 2019-2021 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package net.basdon.anna.internal;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Provider.Service;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.StringJoiner;
import java.util.TreeSet;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import net.basdon.anna.api.Message;

import static java.lang.System.exit;
import static java.nio.charset.StandardCharsets.UTF_8;
import static net.basdon.anna.api.Constants.*;
import static net.basdon.anna.api.Util.*;

public class Main
{
private static boolean restart, shutdown;
private static Closeable socket;

static Socket debugSocket;
static boolean debug_print_in, debug_print_out;

static int recv, sent;

public static
void main(String[] args)
{
	int exit_code;
	try (Log _log = Log.init()) {
		exit_code = main();
	}
	exit(exit_code);
}

private static
int main()
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
	SSLContext sslContext = null;
	boolean sslTrustall = conf.getBool("server.ssl.trustall");
	String sslProtocol = conf.getStr("server.ssl.protocol");
	if (sslProtocol != null) {
		try {
			sslContext = initializeSSLContext(sslProtocol, sslTrustall);
			Log.debug("using SSL protocol " + sslProtocol);
		} catch (NoSuchAlgorithmException ignored) {
			Log.error("cannot load SSL context for protocol: " + sslProtocol);
			printAvailableSecureSocketAlgorithms();
			return 6;
		} catch (KeyManagementException e) {
			Log.error("cannot initialize SSL context", e);
			return 7;
		}
	}

	Anna anna = new Anna(conf);
	Runtime.getRuntime().addShutdownHook(new Thread(Main::shutdownhook));

	for (;;) {
		boolean had_successfull_connection = false;
		anna.connecting();
		try (Socket socket = createSocket(host, port, sslContext)) {
			had_successfull_connection = true;
			CapturedWriter out;
			InputStreamReader in;

			Main.socket = socket;
			anna.writer = out = new CapturedWriter(socket.getOutputStream());
			in = new InputStreamReader(socket.getInputStream(), UTF_8);

			out.print("NICK " + conf.getStr("bot.nick") + "\r\n");
			out.print("USER " + conf.getStr("bot.user") + " 0 0 :"
			          + conf.getStr("bot.userinfo") + "\r\n");

			pumpmessages(in, out, anna);
		} catch (Anna.QuitException ignored) {
		} catch (Anna.RestartException ignored) {
		} catch (UnknownHostException e) {
			Log.error("unknown host, check the config", e);
			return 5;
		} catch (Exception e) {
			Log.error("socket exception", e);
			if (e instanceof SSLHandshakeException && !sslTrustall) {
				Log.info("try setting 'server.ssl.trustall' to 'true'");
			}
		}
		Main.socket = null;
		anna.disconnected();

		if (restart) {
			restart = false;
			continue;
		}

		if (shutdown) {
			Log.info("shutdown was requested");
			break;
		}

		int retry_timeout = conf.getInt("connection.retrytimeoutseconds", 1, 3600);

		if (had_successfull_connection) {
			had_successfull_connection = false;
			Log.warn("disconnected, reconnecting every " + retry_timeout + "s");
		}

		try {
			Thread.sleep(retry_timeout * 1000L);
		} catch (InterruptedException e) {
		}
	}

	anna.shutdown();
	return 0;
}

private static
Socket createSocket(String host, int port, SSLContext sslContext)
throws UnknownHostException, IOException
{
	if (Main.debugSocket != null) {
		return Main.debugSocket;
	}
	if (sslContext != null) {
		return sslContext.getSocketFactory().createSocket(host, port);
	}
	return new Socket(host, port);
}

private static
void printAvailableSecureSocketAlgorithms()
{
	TreeSet<String> algorithms = new TreeSet<>(String::compareTo);
	for (Provider p : Security.getProviders()) {
		for (Service service : p.getServices()) {
			if ("SSLContext".equals(service.getType())) {
				algorithms.add(service.getAlgorithm());
			}
		}
	}
	StringJoiner joiner = new StringJoiner(", ");
	for (String algo : algorithms) {
		joiner.add(algo);
	}
	Log.info("Available SSLContext algorithms: " + joiner.toString());
}

private static
SSLContext initializeSSLContext(String protocol, boolean trustall)
throws KeyManagementException, NoSuchAlgorithmException
{
	SSLContext sslContext = SSLContext.getInstance(protocol);
	TrustManager[] trustManagers = null;
	if (trustall) {
		Log.info("trusting all certificates for SSL context");
		TrustManager tm = new X509TrustManager()
		{
			@Override
			public void checkClientTrusted(X509Certificate[] chain, String authType)
			{
			}
			@Override
			public void checkServerTrusted(X509Certificate[] chain, String authType)
			{
			}
			@Override
			public X509Certificate[] getAcceptedIssuers()
			{
				return new X509Certificate[0];
			}
		};
		trustManagers = new TrustManager[] { tm };
	}
	sslContext.init((KeyManager[]) null, trustManagers,  new SecureRandom());
	return sslContext;
}

private static
void pumpmessages(InputStreamReader in, CapturedWriter out, Anna anna)
throws IOException
{
	char[] buf = new char[512];
	int pos = 0;
	boolean cr = false;
	boolean waiting_for_motd = true;
	for (;;) {
		int c = in.read();
		if (c == -1) {
			Log.warn("end of stream is reached");
			break;
		}
		recv++;
		if (c == '\r') {
			cr = true;
			continue;
		} else if (cr) {
			cr = false;
			if (c == '\n') synchronized (Anna.lock) {
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

				if (pos == 0) {
					continue;
				}

				Message msg = Message.parse(buf, pos);
				if (msg == null) {
					Log.warn(Message.get_last_error());
				} else {
					/*
					System.out.println("Message:");
					System.out.println("  prefix " + new String(msg.prefix));
					System.out.println("  command " + new String(msg.cmd));
					System.out.println("  commandnum " + msg.cmdnum);
					System.out.println("  trailing " + msg.trailing_param);
					System.out.println("  params " + msg.paramc);
					for (int i = 0; i < msg.paramc; i++) {
						char[] v = msg.paramv[i];
						System.out.println("    " + new String(v));
					}
					*/
					if (waiting_for_motd) {
						switch (msg.cmdnum) {
						case RPL_ISUPPORT:
							int count = msg.paramc;
							if (msg.trailing_param) {
								count--;
							}
							if (count > 0) {
								anna.isupport(count, msg.paramv);
							}
							break;
						case ERR_NOMOTD:
						case RPL_ENDOFMOTD:
							waiting_for_motd = false;
							anna.connected(out);
							break;
						}
					} else {
						try {
							anna.dispatch_message(msg);
						} catch (Anna.QuitException e) {
							shutdown = true;
							String m;
							m = anna.conf.getStr("messages.quit");
							out.print("QUIT :" + m + "\r\n");
							throw e;
						} catch (Anna.RestartException e) {
							restart = true;
							String m;
							m = anna.conf.getStr("messages.restart");
							out.print("QUIT :" + m + "\r\n");
							throw e;
						} catch (Throwable t) {
							Log.error("something broke", t);
						}
					}
				}

				pos = 0;
				continue;
			}
			buf[pos++] = '\r';
		}
		buf[pos++] = (char) c;
	}
}

private static
void shutdownhook()
{
	if (Main.socket != null) {
		close(Main.socket);
	}
}
} /*Main*/

class CapturedWriter implements Anna.Output
{
private final Writer out;

CapturedWriter(OutputStream out)
{
	this.out = new BufferedWriter(new OutputStreamWriter(out, UTF_8));
}

@Override
public
void print(char[] buf, int offset, int len)
throws IOException
{
	if (Main.debug_print_out) {
		// -2 to remove CRLF
		System.out.println("-> " + new String(buf, offset, len - 2));
	}
	Main.sent += len;
	out.write(buf, offset, len);
	out.flush();
}
} /*CapturedWriter*/
