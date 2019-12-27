package net.basdon.anna.api;

import org.junit.Test;

import static net.basdon.anna.internal.ChannelUserImplTest.*;
import static org.junit.Assert.*;

public class UtilTest
{
@Test
public
void test_has_mode_no()
{
	ChannelUser usr = usr();
	assertFalse(Util.has_user_mode_or_higher(usr, modes, 'v'));
}

@Test
public
void test_has_mode_direct()
{
	ChannelUser usr = usr();
	usr.modec = 1;
	usr.modev[0] = 'v';
	assertTrue(Util.has_user_mode_or_higher(usr, modes, 'v'));
}

@Test
public
void test_has_mode_indirect()
{
	ChannelUser usr = usr();
	usr.modec = 1;
	usr.modev[0] = 'o';
	assertTrue(Util.has_user_mode_or_higher(usr, modes, 'o'));
	assertTrue(Util.has_user_mode_or_higher(usr, modes, 'v'));
}

@Test
public
void test_has_mode_and_others()
{
	ChannelUser usr = usr();
	usr.modec = 2;
	usr.modev[0] = 'o';
	usr.modev[1] = 'v';
	assertTrue(Util.has_user_mode_or_higher(usr, modes, 'v'));
}

@Test
public
void test_has_mode_multi()
{
	ChannelUser usr = usr();
	usr.modec = 1;
	usr.modev[0] = 'o';
	assertFalse(Util.has_user_mode_or_higher(usr, modes, 'q'));
	assertFalse(Util.has_user_mode_or_higher(usr, modes, 'h'));
	assertTrue(Util.has_user_mode_or_higher(usr, modes, 'o'));
	assertTrue(Util.has_user_mode_or_higher(usr, modes, 'v'));

	usr.modec = 2;
	usr.modev[0] = 'v';
	usr.modev[1] = 'q';
	assertTrue(Util.has_user_mode_or_higher(usr, modes, 'q'));
	assertTrue(Util.has_user_mode_or_higher(usr, modes, 'h'));
	assertTrue(Util.has_user_mode_or_higher(usr, modes, 'o'));
	assertTrue(Util.has_user_mode_or_higher(usr, modes, 'v'));

	usr.modec = 0;
	assertFalse(Util.has_user_mode_or_higher(usr, modes, 'q'));
	assertFalse(Util.has_user_mode_or_higher(usr, modes, 'h'));
	assertFalse(Util.has_user_mode_or_higher(usr, modes, 'o'));
	assertFalse(Util.has_user_mode_or_higher(usr, modes, 'v'));
}
}
