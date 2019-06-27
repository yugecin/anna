// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package annamod;

import java.io.BufferedReader;
import java.io.InputStreamReader;

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
			int[] len = m.do_figlet(result, line.toCharArray());
			for (int i = 0; i < charheight; i++) {
				System.out.println(new String(result[i], 0, len[i]));
			}
		}
	}
}

Mod m;

private
void check(String str, String...expected)
{
	char[][] result = new char[charheight][maxlen];
	int len[] = m.do_figlet(result, str.toCharArray());
	StringBuilder actual = new StringBuilder();
	for (int i = 0; i < charheight; i++) {
		if (len[i] > 0) {
			actual.append(result[i], 0, len[i]).append("\n");
		}
	}
	if (actual.length() > 0) {
		actual.deleteCharAt(actual.length() - 1);
	}
	try {
		assertEquals(String.join("\n", expected), actual.toString());
	} catch (AssertionError e) {
		System.err.println("expected:");
		System.err.println(String.join("\n", expected));
		System.err.println("actual:");
		System.err.println(actual.toString());
		throw e;
	}
}

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
	check(
		"like this.",
		" _ _ _            _   _     _",
		"| (_) | _____    | |_| |__ (_)___",
		"| | | |/ / _ \\   | __| '_ \\| / __|",
		"| | |   <  __/   | |_| | | | \\__ \\_",
		"|_|_|_|\\_\\___|    \\__|_| |_|_|___(_)"
	);
}

@Test
public
void test_hello_world()
{
	check(
		"Hello, world!",
		" _   _      _ _                               _     _ _",
		"| | | | ___| | | ___      __      _____  _ __| | __| | |",
		"| |_| |/ _ \\ | |/ _ \\     \\ \\ /\\ / / _ \\| '__| |/ _` | |",
		"|  _  |  __/ | | (_) |     \\ V  V / (_) | |  | | (_| |_|",
		"|_| |_|\\___|_|_|\\___( )     \\_/\\_/ \\___/|_|  |_|\\__,_(_)",
		"                    |/"
	);
}

@Test
public
void test_interactive()
{
	check(
		"Interactive",
		" ___       _                      _   _",
		"|_ _|_ __ | |_ ___ _ __ __ _  ___| |_(_)_   _____",
		" | || '_ \\| __/ _ \\ '__/ _` |/ __| __| \\ \\ / / _ \\",
		" | || | | | ||  __/ | | (_| | (__| |_| |\\ V /  __/",
		"|___|_| |_|\\__\\___|_|  \\__,_|\\___|\\__|_| \\_/ \\___|"
	);
}
}
