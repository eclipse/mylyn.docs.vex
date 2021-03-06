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
package org.eclipse.vex.core.internal.layout;

import java.net.URL;

import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.vex.core.internal.core.DisplayDevice;
import org.eclipse.vex.core.internal.css.CssWhitespacePolicy;
import org.eclipse.vex.core.internal.css.MockDisplayDevice;
import org.eclipse.vex.core.internal.css.StyleSheet;
import org.eclipse.vex.core.internal.css.StyleSheetReader;
import org.eclipse.vex.core.internal.dom.Document;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests proper function of a block-level element within an inline element. These must be layed out as a block child of
 * the containing block element.
 */
public class TestBlocksInInlines {

	FakeGraphics g;
	LayoutContext context;

	@Before
	public void setUp() throws Exception {
		DisplayDevice.setCurrent(new MockDisplayDevice(90, 90));
	}

	public TestBlocksInInlines() throws Exception {
		final URL url = this.getClass().getResource("test.css");
		final StyleSheetReader reader = new StyleSheetReader();
		final StyleSheet ss = reader.read(url);

		g = new FakeGraphics();

		context = new LayoutContext();
		context.setBoxFactory(new MockBoxFactory());
		context.setGraphics(g);
		context.setStyleSheet(ss);
		context.setWhitespacePolicy(new CssWhitespacePolicy(ss));
	}

	@Test
	public void testBlockInInline() throws Exception {
		final Document doc = new Document(new QualifiedName(null, "root"));
		context.setDocument(doc);

		doc.insertText(2, "one  five");
		doc.insertElement(6, new QualifiedName(null, "b"));
		doc.insertText(7, "two  four");
		doc.insertElement(11, new QualifiedName(null, "p"));
		doc.insertText(12, "three");

		final RootBox rootBox = new RootBox(context, doc, 500);
		rootBox.layout(context, 0, Integer.MAX_VALUE);
	}
}
