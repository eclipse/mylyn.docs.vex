/*******************************************************************************
 * Copyright (c) 2004, 2014 John Krasnay and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     John Krasnay - initial API and implementation
 *     David Carver unit tests fixes
 *     Igor Jacy Lino Campista - Java 5 warnings fixed (bug 311325)
 *     Carsten Hiesserich - fix wrapping in addNoSplit
 *******************************************************************************/
package org.eclipse.vex.core.internal.io;

import java.util.ArrayList;
import java.util.List;

/**
 * Wraps text to a given width.
 */
public class TextWrapper {

	private final List<String> parts = new ArrayList<String>();

	private boolean lastIsWhite = true;

	/**
	 * Class constructor.
	 */
	public TextWrapper() {
	}

	/**
	 * Adds text to the list of things to be wrapped.
	 *
	 * @param s
	 *            Text to be added.
	 */
	public void add(final String s) {
		int i = 0;
		int j = 0;
		boolean thisIsWhite = true;
		while (j < s.length()) {

			// skip non-whitespace
			while (j < s.length() && !Character.isWhitespace(s.charAt(j))) {
				j++;
				thisIsWhite = false;
			}

			// skip whitespace
			while (j < s.length() && Character.isWhitespace(s.charAt(j))) {
				j++;
				thisIsWhite = true;
			}

			if (lastIsWhite || parts.isEmpty()) {
				parts.add(s.substring(i, j));
			} else if (!parts.isEmpty()) {

				parts.add(parts.remove(parts.size() - 1) + s.substring(i, j));
			}
			i = j;
			lastIsWhite = thisIsWhite;
		}
	}

	/**
	 * Adds text to the list of things to be wrapped. The given text will be treated as a single unit and will not be
	 * split across lines.
	 *
	 * @param s
	 *            Text to be added.
	 */
	public void addNoSplit(final String s) {

		if (s.length() == 0) {
			return;
		}

		if (lastIsWhite || parts.isEmpty()) {
			parts.add(s);
		} else {
			// Last char is not a whitespace - we must not split here
			parts.add(parts.remove(parts.size() - 1) + s);
		}

		lastIsWhite = Character.isWhitespace(s.charAt(s.length() - 1));
	}

	/**
	 * Clears any added text.
	 */
	public void clear() {
		parts.clear();
	}

	/**
	 * Wraps the text into the given width. The text is only broken at spaces, meaning the returned lines will not
	 * necessarily fit within width.
	 *
	 * @param width
	 */
	public String[] wrap(final int width) {
		final List<String> lines = new ArrayList<String>();
		final StringBuffer line = new StringBuffer();

		for (final String s : parts) {
			if (line.length() > 0 && line.length() + s.length() > width) {
				// part won't fit on the current line
				lines.add(line.toString());
				line.setLength(0);

				if (s.length() > width) {
					lines.add(s);
				} else {
					line.append(s);
				}
			} else {
				line.append(s);
			}
		}

		if (line.length() > 0) {
			lines.add(line.toString());
		}

		return lines.toArray(new String[lines.size()]);
	}

}
