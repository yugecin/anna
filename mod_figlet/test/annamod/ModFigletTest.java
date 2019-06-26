// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package annamod;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import static annamod.Mod.*;

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
}
