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
void test_default_chanmodes()
{
	anna.connecting();

	assertEquals(0, anna.chanmodes_a.length);
	assertEquals(0, anna.chanmodes_b.length);
	assertEquals(0, anna.chanmodes_c.length);
	assertEquals(0, anna.chanmodes_d.length);
}

@Test
public
void test_chanmodes_reset_when_connecting()
{
	anna.chanmodes_a = new char[] { 'r' };
	anna.chanmodes_b = new char[] { 'r' };
	anna.chanmodes_c = new char[] { 'r' };
	anna.chanmodes_d = new char[] { 'r' };

	test_default_prefixes();
}

@Test
public
void test_isupport_chanmodes_simple()
{
	anna.connecting();
	anna.isupport(1, new char[][] { "CHANMODES=a,bc,def,ghij".toCharArray() });

	assertEquals(1, anna.chanmodes_a.length);
	assertEquals(2, anna.chanmodes_b.length);
	assertEquals(3, anna.chanmodes_c.length);
	assertEquals(4, anna.chanmodes_d.length);
	assertEquals('a', anna.chanmodes_a[0]);
	assertEquals('b', anna.chanmodes_b[0]);
	assertEquals('c', anna.chanmodes_b[1]);
	assertEquals('d', anna.chanmodes_c[0]);
	assertEquals('e', anna.chanmodes_c[1]);
	assertEquals('f', anna.chanmodes_c[2]);
	assertEquals('g', anna.chanmodes_d[0]);
	assertEquals('h', anna.chanmodes_d[1]);
	assertEquals('i', anna.chanmodes_d[2]);
	assertEquals('j', anna.chanmodes_d[3]);
}

@Test
public
void test_isupport_chanmodes_extras()
{
	anna.connecting();
	anna.isupport(1, new char[][] { "CHANMODES=a,b,c,d,e,f".toCharArray() });

	assertEquals(1, anna.chanmodes_a.length);
	assertEquals(1, anna.chanmodes_b.length);
	assertEquals(1, anna.chanmodes_c.length);
	assertEquals(1, anna.chanmodes_d.length);
	assertEquals('a', anna.chanmodes_a[0]);
	assertEquals('b', anna.chanmodes_b[0]);
	assertEquals('c', anna.chanmodes_c[0]);
	assertEquals('d', anna.chanmodes_d[0]);
}
}
