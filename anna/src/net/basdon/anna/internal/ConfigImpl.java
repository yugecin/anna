// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package net.basdon.anna.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import net.basdon.anna.api.Config;

class ConfigImpl extends Config
{
static final File config_dir;

static
{
	config_dir = new File("config");
}

static
boolean check_config_directory()
{
	if (!config_dir.exists()) {
		return config_dir.mkdir();
	}
	return config_dir.isDirectory();
}

/**
 * @return {@code null} if an error occured while reading the file or an error occured while
 *                      creating the file with default values
 */
static
ConfigImpl load(String name, Properties defaults)
{
	Properties props = new Properties();
	File conf_file = new File(config_dir, name + ".properties");
	try (InputStream istream = new FileInputStream(conf_file)) {
		props.load(istream);
		boolean needsave = false;
		for (String key : defaults.stringPropertyNames()) {
			if (!props.containsKey(key)) {
				props.setProperty(key, defaults.getProperty(key));
				needsave = true;
			}
		}
		if (needsave) {
			save(conf_file, defaults);
		}
		return new ConfigImpl(conf_file, props, /*is_new*/ false);
	} catch (FileNotFoundException e) {
		Log.warn("config file " + conf_file.getName() + " does not exist, "
		         + "creating with defaults");
	} catch (IOException e) {
		Log.error("failure while reading config file" + conf_file.getName(), e);
		return null;
	}

	if (save(conf_file, defaults)) {
		return new ConfigImpl(conf_file, defaults, /*is_new*/ true);
	}
	Log.error("failed to write defaults (see prev error)");
	return null;

}

private static
boolean save(File conf_file, Properties props)
{
	try (OutputStream ostream = new FileOutputStream(conf_file)) {
		props.store(ostream, /*comments*/ null);
		return true;
	} catch (FileNotFoundException e) {
		Log.error("failure while writing config file" + conf_file.getName(), e);
	} catch (IOException e) {
		Log.error("failure while writing config file" + conf_file.getName(), e);
	}
	return false;
}

final File conf_file;

ConfigImpl(File conf_file, Properties props, boolean is_new)
{
	super(props, is_new);
	this.conf_file = conf_file;
}

@Override
public
boolean save()
{
	return ConfigImpl.save(this.conf_file, this.props);
}
}
