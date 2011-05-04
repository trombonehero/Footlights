package me.footlights.boot;

import java.security.*;
import java.util.regex.Pattern;



/** A very restrictive Java security policy */
public class RestrictivePolicy extends Policy
{
	@Override public PermissionCollection getPermissions(CodeSource codesource)
	{
		Permissions p = new Permissions();

		// give system libraries all permissions
		String url = codesource.getLocation().toString();

		if(Pattern.matches("file:/System/Library/Frameworks/Java.*\\.jar", url))
			p.add(new AllPermission());

		// TODO: temporary!
		else if(Pattern.matches("file:/.*bootstrap/", url)
		        || Pattern.matches("file:/.*bootstrap.jar", url))
			p.add(new AllPermission());

		else
			System.out.println("Code source: " + codesource.getLocation());

		// otherwise, give *no* static permissions (e.g. sockets)
		return p;
	}

	@Override public PermissionCollection getPermissions(ProtectionDomain domain)
	{
		return getPermissions(domain.getCodeSource());
	}

	@Override public void refresh() {}
}
