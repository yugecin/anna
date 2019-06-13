// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package net.basdon.anna.internal;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.basdon.anna.api.Util.*;

class StatsServer extends Thread
{
static int serves;
static int restarts = -1;
static long last_start;

private final Anna anna;
private final int port;
private final LinkedList<StatsServerConnection> connections;

private ServerSocket socket;

StatsServer(Anna anna, int port)
{
	this.anna = anna;
	this.port = port;
	this.connections = new LinkedList<>();
}

StatsServer create_new()
{
	return new StatsServer(this.anna, this.port);
}

@Override
public
void run()
{
	try (ServerSocket socket = new ServerSocket(this.port)) {
		restarts++;
		last_start = System.currentTimeMillis();
		this.socket = socket;
		Log.info("stats listening on port " + this.port);
		for (;;) {
			Socket clientsocket = socket.accept();

			serves++;

			// kill slow connections
			if (!this.connections.isEmpty()) {
				ArrayList<StatsServerConnection> toremove = new ArrayList<>();
				synchronized (this.connections) {
					long now = System.currentTimeMillis();
					Iterator<StatsServerConnection> iter;
					iter = this.connections.iterator();
					while (iter.hasNext()) {
						StatsServerConnection c = iter.next();
						if (now - c.starttime > 1500) {
							toremove.add(c);
							iter.remove();
						}
					}
				}
				for (StatsServerConnection r : toremove) {
					close(r.socket);
					r.interrupt();
				}
			}

			StatsServerConnection connection = new StatsServerConnection(clientsocket);
			connection.start();

			synchronized (connections) {
				this.connections.add(connection);
			}
		}
	} catch (IOException e) {
		if (!this.isInterrupted()) {
			anna.log_error(e, "stats socket exception");
		}
	}
}

@Override
public
void interrupt()
{
	super.interrupt();
	ArrayList<StatsServerConnection> connectionscopy;
	synchronized (connections) {
		connectionscopy = new ArrayList<>(this.connections);
	}
	for (StatsServerConnection c : connectionscopy) {
		c.interrupt();
		close(c.socket);
		try {
			c.join();
		} catch (InterruptedException e) {
			Log.error("statsserv got interrupted while interrupting a worker");
		}
	}
	synchronized (connections) {
		this.connections.clear();
	}
	close(this.socket);
}

private
class StatsServerConnection extends Thread
{
private final Socket socket;
private final long starttime;

private
StatsServerConnection(Socket socket)
{
	this.socket = socket;
	this.starttime = System.currentTimeMillis();
}

@Override
public
void run()
{
	try {
		BufferedReader in;
		BufferedWriter out;
		in = new BufferedReader(new InputStreamReader(socket.getInputStream(), UTF_8));
		out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), UTF_8));
		String line = in.readLine();
		if (line == null) {
			return;
		}
		boolean head = line.startsWith("HEAD");
		boolean favicon = line.contains("favicon.ico");
		while ((line = in.readLine()) != null) {
			if (line.isEmpty()) {
				send_response(out, head, favicon);
				out.flush();
				break;
			}
		}
	} catch (IOException e) {
		Log.warn("exception while handling a stats connection: " + e.getMessage());
	} finally {
		close(socket);
		synchronized (connections) {
			connections.remove(this);
		}
	}
}

private
void send_response(BufferedWriter out, boolean head, boolean favicon)
throws IOException
{
	if (favicon) {
		out.write("HTTP/1.1 404 Not Found\r\nConnection: Close\r\n"
			  + "Content-Length: 0\r\n\r\n");
		return;
	}

	out.write("HTTP/1.1 200 OK\r\nConnection: Close\r\n"
		  + "Content-Type: text/plain; charset=utf-8\r\n"
		  + "Access-Control-Allow-Origin: *\r\n");

	if (head) {
		out.write("Content-Length: ");
		int[] length = { 0 };
		Anna.Output output = new Anna.Output()
		{
			@Override
			public
			void print(String msg)
			throws IOException
			{
				length[0] += msg.length();
			}

			@Override
			public
			void print(char[] buf, int offset, int len)
			throws IOException
			{
				length[0] += len;
			}
		};
		append_my_stats(output);
		anna.print_stats(output);
		out.write(String.valueOf(length[0]));
		out.write("\r\n\r\n");
		return;
	}

	out.write("\r\n");
	Anna.Output output = new Anna.Output()
	{
		@Override
		public
		void print(String msg)
		throws IOException
		{
			out.write(msg);
		}

		@Override
		public
		void print(char[] buf, int offset, int len)
		throws IOException
		{
			out.write(buf, offset, len);
		}
	};
	append_my_stats(output);
	anna.print_stats(output);
}

private
void append_my_stats(Anna.Output out)
throws IOException
{
	out.print("stats\n");
	out.print(" boot: " + format_time(last_start) + "\n");
	out.print(" restarts: " + restarts + "\n");
	out.print(" serves: " + serves + "\n");
	synchronized (connections) {
		out.print(" connections: " + connections.size() + "\n");
	}
	out.print("\n");
}
} /*StatsServerConnection*/
} /*StatsServer*/
