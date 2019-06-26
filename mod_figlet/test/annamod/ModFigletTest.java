// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package annamod;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.function.IntFunction;

import org.junit.Before;
import org.junit.Test;

import static annamod.Mod.*;
import static org.junit.Assert.*;

public class ModFigletTest
{
/**
 * interactive test
 */
public static void main(String[] args) throws Exception
{
	Mod m = new Mod();
	if (!m.load_font()) {
		throw new Exception();
	}
	try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {
		String line;
		while ((line = in.readLine()) != null) {
			char[][] result = new char[charheight][maxlen];
			int len = m.do_figlet(result, line.toCharArray());
			for (int i = 0; i < charheight; i++) {
				System.out.println(new String(result[i], 0, len));
			}
		}
	}
}

Mod m;

@Before
public
void before()
throws Exception
{
	m = new Mod();
	if (!m.load_font()) {
		throw new Exception();
	}
}

@Test
public
void test_like_this()
{
	char[][] result = new char[charheight][maxlen];
	int len = m.do_figlet(result, "like this.".toCharArray());
	assertEquals(36, len);
	assertEquals(" _ _ _            _   _     _       ", new String(result[0], 0, len));
	assertEquals("| (_) | _____    | |_| |__ (_)___   ", new String(result[1], 0, len));
	assertEquals("| | | |/ / _ \\   | __| '_ \\| / __|  ", new String(result[2], 0, len));
	assertEquals("| | |   <  __/   | |_| | | | \\__ \\_ ", new String(result[3], 0, len));
	assertEquals("|_|_|_|\\_\\___|    \\__|_| |_|_|___(_)", new String(result[4], 0, len));
}

@Test
public
void test_hello_world()
{
	char[][] result = new char[charheight][maxlen];
	int len = m.do_figlet(result, "Hello, world!".toCharArray());
	IntFunction<String> f = i -> new String(result[i], 0, len);
	assertEquals(56, len);
	assertEquals(" _   _      _ _                               _     _ _ ", f.apply(0));
	assertEquals("| | | | ___| | | ___      __      _____  _ __| | __| | |", f.apply(1));
	assertEquals("| |_| |/ _ \\ | |/ _ \\     \\ \\ /\\ / / _ \\| '__| |/ _` | |", f.apply(2));
	assertEquals("|  _  |  __/ | | (_) |     \\ V  V / (_) | |  | | (_| |_|", f.apply(3));
	assertEquals("|_| |_|\\___|_|_|\\___( )     \\_/\\_/ \\___/|_|  |_|\\__,_(_)", f.apply(4));
	assertEquals("                    |/                                  ", f.apply(5));
}
}
