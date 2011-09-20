/*
 * Copyright 2011 Jonathan Anderson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.footlights.boot;

import java.security.AllPermission;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.regex.Pattern;


/** A very restrictive Java security policy. */
public class RestrictivePolicy extends Policy
{
	@Override public PermissionCollection getPermissions(CodeSource codesource)
	{
		Permissions p = new Permissions();

		// give system libraries all permissions
		String url = codesource.getLocation().toString();

		if (Pattern.matches("file:/System/Library/Frameworks/Java.*\\.jar", url))
			p.add(new AllPermission());

		// TODO: temporary!
		else if (Pattern.matches("file:/.*bootstrap/", url)
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
