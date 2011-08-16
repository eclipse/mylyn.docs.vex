/*******************************************************************************
 * Copyright (c) 2004, 2008 John Krasnay and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     John Krasnay - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.xml.vex.core.internal.dom;

/**
 * Determines whitespace policy for document elements. For example, a CSS
 * stylesheet implements a whitespace policy via its display and white-space
 * properties.
 */
public interface IWhitespacePolicy {

	/**
	 * A NULL object of this type. No blocks and no pre elements.
	 */
	IWhitespacePolicy NULL = new IWhitespacePolicy() {
		public boolean isBlock(Element element) {
			return false;
		}

		public boolean isPre(Element element) {
			return false;
		}
	};

	/**
	 * Returns true if the given element is normally block-formatted.
	 * 
	 * @param element
	 *            Element to test.
	 */
	boolean isBlock(Element element);

	/**
	 * Returns true if the given element is pre-formatted, that is, all of its
	 * contained whitespace should be preserved.
	 * 
	 * @param element
	 *            Element to test.
	 */
	boolean isPre(Element element);
}
