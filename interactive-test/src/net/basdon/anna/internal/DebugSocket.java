// Copyright 2021 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package net.basdon.anna.internal;

import java.net.Socket;
import java.util.function.Consumer;

public
class DebugSocket extends Socket
{
public DebugSocketInputStream is;
public DebugSocketOutputStream os;

public
DebugSocket(Consumer<String> lineConsumer)
{
	this.is = new DebugSocketInputStream();
	this.os = new DebugSocketOutputStream(lineConsumer);
}

@Override
public
DebugSocketInputStream getInputStream()
{
	return this.is;
}

@Override
public
DebugSocketOutputStream getOutputStream()
{
	return this.os;
}

@Override
public synchronized
void close()
{
}
}
