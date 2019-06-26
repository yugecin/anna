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
void check_line(int[] len, char[][] result, int row, String line)
{
	assertEquals(line.length(), len[row]);
	assertEquals(line, new String(result[row], 0, len[row]));
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
	char[][] result = new char[charheight][maxlen];
	int len[] = m.do_figlet(result, "like this.".toCharArray());
	check_line(len, result, 0, " _ _ _            _   _     _");
	check_line(len, result, 1, "| (_) | _____    | |_| |__ (_)___");
	check_line(len, result, 2, "| | | |/ / _ \\   | __| '_ \\| / __|");
	check_line(len, result, 3, "| | |   <  __/   | |_| | | | \\__ \\_");
	check_line(len, result, 4, "|_|_|_|\\_\\___|    \\__|_| |_|_|___(_)");
}

@Test
public
void test_hello_world()
{
	char[][] r = new char[charheight][maxlen];
	int len[] = m.do_figlet(r, "Hello, world!".toCharArray());
	check_line(len, r, 0, " _   _      _ _                               _     _ _");
	check_line(len, r, 1, "| | | | ___| | | ___      __      _____  _ __| | __| | |");
	check_line(len, r, 2, "| |_| |/ _ \\ | |/ _ \\     \\ \\ /\\ / / _ \\| '__| |/ _` | |");
	check_line(len, r, 3, "|  _  |  __/ | | (_) |     \\ V  V / (_) | |  | | (_| |_|");
	check_line(len, r, 4, "|_| |_|\\___|_|_|\\___( )     \\_/\\_/ \\___/|_|  |_|\\__,_(_)");
	check_line(len, r, 5, "                    |/");
}
}
