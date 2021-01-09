// Copyright 2021 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package net.basdon.anna.internal;

import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Consumer;

import static java.nio.charset.StandardCharsets.UTF_8;

public
class DebugSocketOutputStream extends OutputStream
{
private byte[] buf;
private int bufPos;
private Consumer<String> lineConsumer;

public
DebugSocketOutputStream(Consumer<String> lineConsumer)
{
	this.buf = new byte[2048];
	this.lineConsumer = lineConsumer;
}

@Override
public void write(byte[] buf, int off, int len) throws IOException
{
	while (len > 0) {
		byte b = buf[off];
		if (b == '\n') {
		} else if (b == '\r') {
			this.lineConsumer.accept(new String(this.buf, 0, this.bufPos, UTF_8));
			this.bufPos = 0;
		} else {
			this.buf[this.bufPos++] = b;
			if (this.bufPos == this.buf.length) {
				this.lineConsumer.accept(new String(this.buf, 0, this.bufPos, UTF_8));
				this.bufPos = 0;
			}
		}
		off++;
		len--;
	}
}

@Override
public void write(int b) throws IOException
{
	if (b == '\r') {
	} else if (b == '\n') {
		this.lineConsumer.accept(new String(this.buf, 0, this.bufPos));
		this.bufPos = 0;
	} else {
		this.buf[this.bufPos++] = (byte) b;
		if (this.bufPos == this.buf.length) {
			this.lineConsumer.accept(new String(this.buf, 0, this.bufPos, UTF_8));
			this.bufPos = 0;
		}
	}
}
}
