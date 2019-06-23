// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package annamod;

import net.basdon.anna.api.IMod;

import static java.lang.System.out;

public class Mod implements IMod
{
@Override
public String getName()
{
	return "mod_test";
}

@Override
public String getVersion()
{
	return "0";
}

@Override
public String getDescription()
{
	return "prints all IMod events to console to test things";
}

@Override
public boolean onEnable()
{
	out.println("mod_test: enabled");
	return true;
}

@Override
public void onDisable()
{
	out.println("mod_test: disabled");
}
}