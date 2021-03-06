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

import org.eclipse.core.runtime.Assert;
import org.eclipse.vex.core.internal.core.ColorResource;
import org.eclipse.vex.core.internal.core.FontResource;
import org.eclipse.vex.core.internal.core.Graphics;
import org.eclipse.vex.core.internal.css.Styles;
import org.eclipse.vex.core.provisional.dom.INode;

/**
 * A TextBox representing a static string. Represents text which is not editable within the VexWidget, such as
 * enumerated list markers.
 */
public class StaticTextBox extends TextBox {

	public static final byte NO_MARKER = 0;
	public static final byte START_MARKER = 1;
	public static final byte END_MARKER = 2;

	private final String text;
	private final byte marker;
	private int startRelative;
	private final int endRelative;

	/**
	 * Class constructor.
	 *
	 * @param context
	 *            LayoutContext used to calculate the box's size.
	 * @param node
	 *            Node used to style the text.
	 * @param text
	 *            Static text to display
	 */
	public StaticTextBox(final LayoutContext context, final INode node, final String text) {
		this(context, node, text, NO_MARKER);
		if (text.length() == 0) {
			throw new IllegalArgumentException("StaticTextBox cannot have an empty text string.");
		}
	}

	/**
	 * Class constructor. This constructor is used when generating a static text box representing a marker for the start
	 * or end of an inline element. If the selection spans the related marker, the text is drawn in the platform's text
	 * selection colours.
	 *
	 * @param context
	 *            LayoutContext used to calculate the box's size
	 * @param node
	 *            Node used to style the text
	 * @param text
	 *            Static text to display
	 * @param marker
	 *            START_MARKER or END_MARKER, depending on whether the text represents the start sentinel or the end
	 *            sentinel of the element
	 */
	public StaticTextBox(final LayoutContext context, final INode node, final String text, final byte marker) {
		super(node);
		this.text = text;
		startRelative = 0;
		endRelative = text.length();
		this.marker = marker;
		calculateHeight(context);
		setWidth(-1);
	}

	/**
	 * Class constructor used by the splitAt method.
	 *
	 * @param other
	 *            Instance of DocumentTextBox that should be splitted.
	 * @param endOffset
	 *            The endOffset of the text in the new box.
	 * @param width
	 *            The calculated layout width of the new box.
	 */
	private StaticTextBox(final StaticTextBox other, final int endOffset, final int width) {
		super(other.getNode());
		text = other.text; // Text is shared in all instances
		startRelative = other.startRelative;
		endRelative = startRelative + endOffset;
		if (endRelative <= startRelative) {
			Assert.isTrue(startRelative < endRelative, "StaticTextBox start is greater than end");
		}
		marker = other.marker;
		setWidth(width);
		setHeight(other.getHeight());
		setBaseline(other.getBaseline());
	}

	/**
	 * @see org.eclipse.vex.core.internal.layout.TextBox#getText()
	 */
	@Override
	public String getText() {
		return text.substring(startRelative, endRelative);
	}

	@Override
	public boolean isEOL() {
		return text.length() > 0 && text.charAt(endRelative - 1) == NEWLINE_CHAR;
	}

	/**
	 * @see org.eclipse.vex.core.internal.layout.Box#hasContent()
	 */
	@Override
	public boolean hasContent() {
		return false;
	}

	/**
	 * @see org.eclipse.vex.core.internal.layout.Box#paint(org.eclipse.vex.core.internal.layout.LayoutContext, int, int)
	 */
	@Override
	public void paint(final LayoutContext context, final int x, final int y) {

		final Styles styles = context.getStyleSheet().getStyles(getNode());
		final Graphics g = context.getGraphics();

		boolean drawSelected = false;
		if (marker == START_MARKER) {
			drawSelected = getNode().getStartOffset() >= context.getSelectionStart() && getNode().getStartOffset() + 1 <= context.getSelectionEnd();
		} else if (marker == END_MARKER) {
			drawSelected = getNode().getEndOffset() >= context.getSelectionStart() && getNode().getEndOffset() + 1 <= context.getSelectionEnd();
		}

		final FontResource font = g.getFont(styles.getFont());
		final ColorResource color = g.getColor(styles.getColor());

		final FontResource oldFont = g.setCurrentFont(font);
		final ColorResource oldColor = g.setColor(color);

		if (drawSelected) {
			paintSelectedText(context, getText(), x, y);
		} else {
			g.drawString(getText(), x, y);
		}
		paintTextDecoration(context, styles, getText(), x, y);

		g.setCurrentFont(oldFont);
		g.setColor(oldColor);
	}

	/**
	 * @see org.eclipse.vex.core.internal.layout.TextBox#splitAt(int)
	 */
	@Override
	protected Pair splitAt(final LayoutContext context, final int offset, final int leftWidth, final int maxWidth) {

		int remaining = maxWidth;

		StaticTextBox left;
		if (offset == 0) {
			left = null;
		} else {
			left = new StaticTextBox(this, offset, leftWidth);
			remaining -= leftWidth;
		}

		StaticTextBox right;
		if (offset + startRelative >= endRelative) {
			right = null;
		} else {
			// Instead of creating a new box, we reuse this one
			startRelative += offset;
			if (left == null) {
				calculateSize(context);
				remaining -= getWidth();
			}
			right = this;
		}
		return new Pair(left, right, remaining);
	}

}
