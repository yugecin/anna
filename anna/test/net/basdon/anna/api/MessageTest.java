// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package net.basdon.anna.api;

import org.junit.Test;

import static org.junit.Assert.*;

public class MessageTest
{
private static
Message parse(String msg)
{
	return Message.parse(msg.toCharArray(), msg.length());
}

@Test
public
void test_parse_normal()
{
	Message m = parse(":server 345 hi");

	assertNotNull(m);
	assertEquals("server", new String(m.prefix));
	assertEquals("345", new String(m.cmd));
	assertEquals(345, m.cmdnum);
	assertEquals(1, m.paramc);
	assertEquals("hi", new String(m.paramv[0]));
	assertTrue(m.trailing_param == false);
}

@Test
public
void test_parse_normal_trailing_whitespace()
{
	Message m = parse(":server 345 hi ");

	assertNotNull(m);
	assertEquals("server", new String(m.prefix));
	assertEquals("345", new String(m.cmd));
	assertEquals(345, m.cmdnum);
	assertEquals(1, m.paramc);
	assertEquals("hi", new String(m.paramv[0]));
	assertTrue(m.trailing_param == false);
}

@Test
public
void test_parse_normal_lowcmdnum()
{
	Message m = parse(":server 005 hi");

	assertNotNull(m);
	assertEquals("server", new String(m.prefix));
	assertEquals("005", new String(m.cmd));
	assertEquals(5, m.cmdnum);
	assertEquals(1, m.paramc);
	assertEquals("hi", new String(m.paramv[0]));
	assertTrue(m.trailing_param == false);
}

@Test
public
void test_parse_normal_trailing()
{
	Message m = parse(":server 345 :hi there");

	assertNotNull(m);
	assertEquals("server", new String(m.prefix));
	assertEquals("345", new String(m.cmd));
	assertEquals(345, m.cmdnum);
	assertEquals(1, m.paramc);
	assertEquals("hi there", new String(m.paramv[0]));
	assertTrue(m.trailing_param == true);
}

@Test
public
void test_parse_normal_multiparam()
{
	Message m = parse(":server 345 #chan is hi");

	assertNotNull(m);
	assertEquals("server", new String(m.prefix));
	assertEquals("345", new String(m.cmd));
	assertEquals(345, m.cmdnum);
	assertEquals(3, m.paramc);
	assertEquals("#chan", new String(m.paramv[0]));
	assertEquals("is", new String(m.paramv[1]));
	assertEquals("hi", new String(m.paramv[2]));
	assertTrue(m.trailing_param == false);
}

@Test
public
void test_parse_normal_multiparam_trailing()
{
	Message m = parse(":server 345 #chan is :hi there");

	assertNotNull(m);
	assertEquals("server", new String(m.prefix));
	assertEquals("345", new String(m.cmd));
	assertEquals(345, m.cmdnum);
	assertEquals(3, m.paramc);
	assertEquals("#chan", new String(m.paramv[0]));
	assertEquals("is", new String(m.paramv[1]));
	assertEquals("hi there", new String(m.paramv[2]));
	assertTrue(m.trailing_param == true);
}

@Test
public
void test_parse_normal_multiparam_trailing_excesswhitespace()
{
	Message m = parse(":server    345   #chan    is   :hi there");

	assertNotNull(m);
	assertEquals("server", new String(m.prefix));
	assertEquals("345", new String(m.cmd));
	assertEquals(345, m.cmdnum);
	assertEquals(3, m.paramc);
	assertEquals("#chan", new String(m.paramv[0]));
	assertEquals("is", new String(m.paramv[1]));
	assertEquals("hi there", new String(m.paramv[2]));
	assertTrue(m.trailing_param == true);
}

@Test
public
void test_parse_noprefix()
{
	Message m = parse("345 a b c");

	assertNotNull(m);
	assertEquals(null, m.prefix);
	assertEquals("345", new String(m.cmd));
	assertEquals(345, m.cmdnum);
	assertEquals(3, m.paramc);
	assertEquals("a", new String(m.paramv[0]));
	assertEquals("b", new String(m.paramv[1]));
	assertEquals("c", new String(m.paramv[2]));
	assertTrue(m.trailing_param == false);
}

@Test
public
void test_parse_noprefix_trailing()
{
	Message m = parse("345 a b :c d");

	assertNotNull(m);
	assertEquals(null, m.prefix);
	assertEquals("345", new String(m.cmd));
	assertEquals(345, m.cmdnum);
	assertEquals(3, m.paramc);
	assertEquals("a", new String(m.paramv[0]));
	assertEquals("b", new String(m.paramv[1]));
	assertEquals("c d", new String(m.paramv[2]));
	assertTrue(m.trailing_param == true);
}

@Test
public
void test_parse_noprefix_trailing_excesswhitespace()
{
	Message m = parse("345   a   b    :c d");

	assertNotNull(m);
	assertEquals(null, m.prefix);
	assertEquals("345", new String(m.cmd));
	assertEquals(345, m.cmdnum);
	assertEquals(3, m.paramc);
	assertEquals("a", new String(m.paramv[0]));
	assertEquals("b", new String(m.paramv[1]));
	assertEquals("c d", new String(m.paramv[2]));
	assertTrue(m.trailing_param == true);
}
}
