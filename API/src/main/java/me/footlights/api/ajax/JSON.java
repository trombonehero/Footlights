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
package me.footlights.api.ajax;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.LinkedHashMap;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;


/**
 * JavaScript Object Notation, a common data serialization format.
 *
 * @see RFC 4627; http://www.ietf.org/rfc/rfc4627.txt
 */
public final class JSON implements AjaxResponse
{
	@Override public String mimeType() { return "application/json"; }
	@Override public InputStream data() { return new ByteArrayInputStream(toString().getBytes()); }
	@Override public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("{ ");
		builder.append(Joiner.on(", ").withKeyValueSeparator(": ").join(members));
		builder.append(" }");

		return builder.toString();
	}

	public static Builder newBuilder() { return new Builder(); }
	/**
	 * Builds {@link JSON} objects, using a few contortions to get around Java type erasure in
	 * the case of JSON arrays.
	 */
	public static class Builder
	{
		public Builder put(String k, int v)    { return set(k, JSONData.build(v)); }
		public Builder put(String k, double v) { return set(k, JSONData.build(v)); }
		public Builder put(String k, String v) { return set(k, JSONData.build(v)); }
		public Builder put(String k, JSON v)   { return set(k, JSONData.build(v)); }

		public Builder putInts(String k, Iterable<Integer> v) { return put(k, v, INT); }
		public Builder putDoubles(String k, Iterable<Double> v) { return put(k, v, DOUBLE); }
		public Builder putStrings(String k, Iterable<String> v) { return put(k, v, STRING); }

		public JSON build() { return new JSON(data); }

		private <T> Builder put(String key, Iterable<T> values, Function<T,JSONData> transformer)
		{
			return set(key, JSONData.build(Iterables.transform(values, transformer)));
		}

		private Builder set(String key, JSONData value)
		{
			data.put(quote(key), value);
			return this;
		}

		// I really wish that Java's generic system was a bit more like C++.
		private static Function<Integer,JSONData> INT = new Function<Integer,JSONData>()
			{
				@Override public JSONData apply(Integer value)
				{
					return JSONData.build(value);
				}
			};

		private static Function<Double,JSONData> DOUBLE = new Function<Double,JSONData>()
			{
				@Override public JSONData apply(Double value)
				{
					return JSONData.build(value);
				}
			};

		private static Function<String,JSONData> STRING = new Function<String,JSONData>()
			{
				@Override public JSONData apply(String value)
				{
					return JSONData.build(value);
				}
		};

		private LinkedHashMap<String, JSONData> data = Maps.newLinkedHashMap();
	}


	/** An element which can be stored in a JSON object (primitive type or another JSON). */
	private static final class JSONData
	{
		@Override public String toString() { return stringRepresentation; }

		private static JSONData build(int i)    { return new JSONData(Integer.toString(i)); }
		private static JSONData build(double f) { return new JSONData(Double.toString(f)); }
		private static JSONData build(String s) { return new JSONData(quote(s)); }

		private static JSONData build(JSON j)   { return new JSONData(j.toString()); }
		private static JSONData build(Iterable<JSONData> i)
		{
			return new JSONData("[ " + Joiner.on(", ").join(i) + " ]");
		}

		private JSONData(String repr) { this.stringRepresentation = repr; }
		private final String stringRepresentation;
	}

	/** Escape and quote a {@link String}. */
	private static String quote(String s) { return "\"" + s.replaceAll("\"", "\\\"") + "\""; }

	private JSON(LinkedHashMap<String, JSONData> members)
	{
		this.members = ImmutableMap.copyOf(members);
	}

	/** Object data, in the form of an ordered associative array. */
	private final ImmutableMap<String, JSONData> members;
}
