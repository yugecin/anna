// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package net.basdon.anna.internal;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ChannelUserImplTest
{
char[] modes = { 'q', 'h', 'o', 'v' };
char[] prefixes = { '~', '%', '@', '+' };

private
ChannelUserImpl usr()
{
	ChannelUserImpl.maxmodes = modes.length;
	return new ChannelUserImpl(modes);
}

@Test
public
void test_new_user_empty_modes()
{
	ChannelUserImpl c = usr();

	assertEquals("new user should have no modes", 0, c.modec);
	assertEquals("new user should have prefix '0'", 0, c.prefix);
}

@Test
public
void test_add_modes()
{
	ChannelUserImpl c = usr();
	c.mode_add('v', modes, prefixes);
	c.mode_add('o', modes, prefixes);

	assertEquals("user should have 2 modes", 2, c.modec);
	assertEquals('o', c.modev[0]);
	assertEquals('v', c.modev[1]);
	assertEquals('@', c.prefix);
}

@Test
public
void test_adding_same_mode_again_has_no_effect()
{
	ChannelUserImpl c = usr();
	c.mode_add('v', modes, prefixes);
	c.mode_add('o', modes, prefixes);
	c.mode_add('v', modes, prefixes);

	assertEquals("user should have 2 modes", 2, c.modec);
	assertEquals('o', c.modev[0]);
	assertEquals('v', c.modev[1]);
	assertEquals('@', c.prefix);
}

@Test
public
void test_adding_unknown_mode_has_no_effect()
{
	ChannelUserImpl c = usr();
	c.mode_add('v', modes, prefixes);
	c.mode_add('z', modes, prefixes);

	assertEquals(1, c.modec);
	assertEquals('v', c.modev[0]);
	assertEquals('+', c.prefix);
}

@Test
public
void test_remove_modes()
{
	ChannelUserImpl c = usr();
	c.mode_add('v', modes, prefixes);
	c.mode_add('o', modes, prefixes);

	assertEquals("user should have 2 modes", 2, c.modec);
	assertEquals('o', c.modev[0]);
	assertEquals('v', c.modev[1]);
	assertEquals('@', c.prefix);

	c.mode_remove('o', modes, prefixes);

	assertEquals("user should have 1 mode", 1, c.modec);
	assertEquals('v', c.modev[0]);
	assertEquals('+', c.prefix);
}
}
