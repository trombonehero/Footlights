package me.footlights.demo.plugins.good;

/**
 * Helper methods used by the plugin (tests that the class loader hasn't done
 * anything truly bizarre).
 *
 * @author jon@footlights.me
 */
public class Helper
{
	public static String staticHelp() { return "Helper.staticHelp()"; }
	public String help() { return "Helper.help()"; }
}
