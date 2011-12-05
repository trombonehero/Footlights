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
package me.footlights.plugin;


/**
 * A mutable version of {@link Preferences}.
 *
 * The actual storage bookkeeping is hidden as an implementation detail.
 */
public interface ModifiablePreferences extends Preferences
{
	public Preferences set(String key, String value);
	public Preferences set(String key, boolean value);
	public Preferences set(String key, int value);
	public Preferences set(String key, float value);
}
