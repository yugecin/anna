// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package net.basdon.anna.api;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UserTest
{
@Test
public
void test_parse_normal()
{
	User u = User.parse("--robin!nick@host--".toCharArray(), 2, 17);
	assertEquals("robin", new String(u.nick));
	assertEquals("nick", new String(u.name));
	assertEquals("host", new String(u.host));
}

@Test
public
void test_parse_nonick()
{
	User u = User.parse("--robin@host--".toCharArray(), 2, 12);
	assertEquals("robin", new String(u.nick));
	assertEquals(null, u.name);
	assertEquals("host", new String(u.host));
}

@Test
public
void test_parse_invalid_exat()
{
	User u = User.parse("--robin!@host--".toCharArray(), 2, 13);
	assertEquals(null, u);
}

@Test
public
void test_parse_invalid_nohost()
{
	User u = User.parse("--robin!nick@--".toCharArray(), 2, 13);
	assertEquals(null, u);
}
}
