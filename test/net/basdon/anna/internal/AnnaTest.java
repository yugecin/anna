// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package net.basdon.anna.internal;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class AnnaTest
{
Anna anna;

@Before
public
void before()
{
	anna = new Anna(new ConfigImpl(null, Anna.default_config, true));
}

@Test
public
void test_default_prefixes()
{
	anna.connecting();

	assertEquals(2, anna.modes.length);
	assertEquals('o', anna.modes[0]);
	assertEquals('v', anna.modes[1]);
	assertEquals(2, anna.prefixes.length);
	assertEquals('@', anna.prefixes[0]);
	assertEquals('+', anna.prefixes[1]);
}

@Test
public
void test_prefixes_reset_when_connecting()
{
	anna.modes = new char[] { 'r' };
	anna.prefixes = new char[] { '$' };

	test_default_prefixes();
}

@Test
public
void test_isupport_prefixes_simple()
{
	anna.connecting();
	anna.isupport(1, new char[][] { "PREFIX=(qov)~@+".toCharArray() });

	assertEquals(3, anna.modes.length);
	assertEquals('q', anna.modes[0]);
	assertEquals('o', anna.modes[1]);
	assertEquals('v', anna.modes[2]);
	assertEquals(3, anna.prefixes.length);
	assertEquals('~', anna.prefixes[0]);
	assertEquals('@', anna.prefixes[1]);
	assertEquals('+', anna.prefixes[2]);
}

@Test
public
void test_isupport_prefixes_invalid_imbalance_prefix()
{
	anna.connecting();
	anna.isupport(1, new char[][] { "PREFIX=(q)~@".toCharArray() });

	assertEquals(Anna.MODES_DEFAULT, anna.modes);
	assertEquals(Anna.PREFIXES_DEFAULT, anna.prefixes);
}

@Test
public
void test_isupport_prefixes_invalid_imbalance_mode()
{
	anna.connecting();
	anna.isupport(1, new char[][] { "PREFIX=(qo)~".toCharArray() });

	assertEquals(Anna.MODES_DEFAULT, anna.modes);
	assertEquals(Anna.PREFIXES_DEFAULT, anna.prefixes);
}

@Test
public
void test_isupport_prefixes_extended()
{
	anna.connecting();
	anna.isupport(3, new char[][] {
		"BLABLA=something".toCharArray(),
		"PREFIX=(qov)~@+".toCharArray(),
		"NOTHING=bla".toCharArray()
	});

	assertEquals(3, anna.modes.length);
	assertEquals('q', anna.modes[0]);
	assertEquals('o', anna.modes[1]);
	assertEquals('v', anna.modes[2]);
	assertEquals(3, anna.prefixes.length);
	assertEquals('~', anna.prefixes[0]);
	assertEquals('@', anna.prefixes[1]);
	assertEquals('+', anna.prefixes[2]);
}
}
