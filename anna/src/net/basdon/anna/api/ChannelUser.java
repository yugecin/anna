// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package net.basdon.anna.api;

public abstract class ChannelUser
{
public char[] nick;
public int modec;
/**
 * modes this user has, sorted
 */
public char[] modev;
/**
 * prefix that should be shown, or {@code 0}
 */
public char prefix;

protected ChannelUser()
{
}
}
