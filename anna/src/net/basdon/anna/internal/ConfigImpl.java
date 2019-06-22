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

static boolean check_config_directory()
{
	if (!config_dir.exists()) {
		return config_dir.mkdir();
	}
	return config_dir.isDirectory();
}

/**
 * @return {@code null} if file does not exist, an error occured while reading the file or an error
 *         occured while creating the file with default values
 */
static ConfigImpl load(String name, Properties defaults)
{
	Properties props = new Properties();
	File conf_file = new File(config_dir, name + ".properties");
	try (InputStream istream = new FileInputStream(conf_file)) {
		props.load(istream);
		for (String key : defaults.stringPropertyNames()) {
			if (!props.containsKey(key)) {
				props.setProperty(key, defaults.getProperty(key));
			}
		}
		return new ConfigImpl(conf_file, props, /*is_new*/ false);
	} catch (FileNotFoundException e) {
		Log.warn("config file " + conf_file.getName() + " does not exist, "
		         + "creating with defaults");
	} catch (IOException e) {
		Log.error("failure while reading config file" + conf_file.getName(), e);
		return null;
	}

	try (OutputStream ostream = new FileOutputStream(conf_file)) {
		defaults.store(ostream, /*comments*/ null);
		return new ConfigImpl(conf_file, defaults, /*is_new*/ true);
	} catch (FileNotFoundException e) {
		Log.error("failure while writing default config file" + conf_file.getName(), e);
		return null;
	} catch (IOException e) {
		Log.error("failure while writing default config file" + conf_file.getName(), e);
		return null;
	}

}

final File conf_file;

ConfigImpl(File conf_file, Properties props, boolean is_new)
{
	super(props, is_new);
	this.conf_file = conf_file;
}
}
