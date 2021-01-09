// Copyright 2021 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package net.basdon.anna.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.function.Consumer;

import static java.nio.charset.StandardCharsets.UTF_8;

public
class DebugSocketInputStream extends InputStream implements Consumer<String>
{
private byte[] sendingLine;
private int sendingLinePos;
private LinkedList<String> linesToSend = new LinkedList<>();

private
void ensureSendingLineAvailable()
{
	while (sendingLine == null) {
		if (this.linesToSend.isEmpty()) {
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
			continue;
		}
		this.sendingLine = this.linesToSend.pollFirst().getBytes(UTF_8);
		this.sendingLinePos = 0;
	}
}

/**
 * Call after changing {@link #sendingLinePos}.
 */
private
void postRead()
{
	if (this.sendingLinePos == this.sendingLine.length) {
		this.sendingLine = null;
	}
}

@Override
public
int read() throws IOException
{
	this.ensureSendingLineAvailable();
	int value = this.sendingLine[this.sendingLinePos];
	this.sendingLinePos++;
	this.postRead();
	return value;
}

@Override
public
int available() throws IOException
{
	// Real number can be bigger, but it's an estimate anyways.
	if (this.sendingLine != null) {
		return this.sendingLine.length - this.sendingLinePos;
	}
	if (this.linesToSend.isEmpty()) {
		return 0;
	}
	return this.linesToSend.getFirst().length();
}

@Override
public
int read(byte[] b, int off, int len) throws IOException
{
	this.ensureSendingLineAvailable();
	int lenToRead = Math.min(len, this.sendingLine.length - this.sendingLinePos);
	System.arraycopy(this.sendingLine, this.sendingLinePos, b, off, lenToRead);
	this.sendingLinePos += lenToRead;
	this.postRead();
	return lenToRead;
}

@Override
public
void accept(String line)
{
	this.linesToSend.addLast(line);
}
}
