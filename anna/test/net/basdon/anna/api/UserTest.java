// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package net.basdon.anna.api;

import org.junit.Test;

import static org.junit.Assert.*;

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

@Test
public
void test_match_full()
{
	User u1 = User.parse("--nick!name@host--".toCharArray(), 2, 16);
	User u2 = User.parse("--n_ck!name@host--".toCharArray(), 2, 16);
	User u3 = User.parse("--nick!n_me@host--".toCharArray(), 2, 16);
	User u4 = User.parse("--nick!name@h_st--".toCharArray(), 2, 16);
	User u5 = User.parse("--nick!name@host--".toCharArray(), 2, 16);

	assertTrue(!u1.matches(u2));
	assertTrue(!u1.matches(u3));
	assertTrue(!u1.matches(u4));
	assertTrue(u1.matches(u5));
}

@Test
public
void test_match_wildcards()
{
	User u1 = User.parse("--nick!name@host--".toCharArray(), 2, 16);
	User u2 = User.parse("--n_ck!name@host--".toCharArray(), 2, 16);
	User u3 = User.parse("--nick!n_me@host--".toCharArray(), 2, 16);
	User u4 = User.parse("--nick!name@h_st--".toCharArray(), 2, 16);

	User u5 = User.parse("--*!name@host--".toCharArray(), 2, 13);
	User u6 = User.parse("--nick!*@host--".toCharArray(), 2, 13);
	User u7 = User.parse("--nick!name@*--".toCharArray(), 2, 13);
	User u8 = User.parse("--*!*@*--".toCharArray(), 2, 7);

	assertTrue(u5.matches(u1));
	assertTrue(u5.matches(u2));
	assertTrue(!u5.matches(u3));
	assertTrue(!u1.matches(u4));

	assertTrue(u6.matches(u1));
	assertTrue(!u6.matches(u2));
	assertTrue(u6.matches(u3));
	assertTrue(!u6.matches(u4));

	assertTrue(u7.matches(u1));
	assertTrue(!u7.matches(u2));
	assertTrue(!u7.matches(u3));
	assertTrue(u7.matches(u4));

	assertTrue(u8.matches(u1));
	assertTrue(u8.matches(u2));
	assertTrue(u8.matches(u3));
	assertTrue(u8.matches(u4));
}
}
