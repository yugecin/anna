// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package net.basdon.anna.api;

import java.util.Properties;

public abstract class Config
{
public final Properties props;
/**
 * {@code true} when the config did not exist when loaded and was created with default values
 */
public final boolean is_new;

protected Config(Properties props, boolean is_new)
{
	this.props = props;
	this.is_new = is_new;
}

/**
 * short form of {@code Integer.parseInt(props.getProperty(String))}, with error handling
 *
 * @return associated integer value associated with the key,
 *         or 0 if no such key or key is not an int
 */
public int getInt(String key)
{
	try {
		return Integer.parseInt(this.props.getProperty(key, "0"));
	} catch (NumberFormatException e) {
		return 0;
	}
}

/**
 * variant of {@link #getInt(String)} with boundaries
 */
public int getInt(String key, int lower_bound, int upper_bound)
{
	return Math.min(Math.max(this.getInt(key), lower_bound), upper_bound);
}

/**
 * short form of {@code props.getProperty(String)}
 *
 * @return value associated with the key or null
 */
public String getStr(String key)
{
	return this.props.getProperty(key);
}

/**
 * short form of {@code Boolean.parseBoolean(props.getProperty(String))}
 *
 * @return associated boolean value associated with the key, or {@code false} if no such key
 */
public boolean getBool(String key)
{
	return Boolean.parseBoolean(this.props.getProperty(key));
}
}
