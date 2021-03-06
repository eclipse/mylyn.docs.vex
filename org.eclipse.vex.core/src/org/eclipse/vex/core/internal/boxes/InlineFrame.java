/*******************************************************************************
 * Copyright (c) 2015 Florian Thienel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		Florian Thienel - initial API and implementation
 *******************************************************************************/
package org.eclipse.vex.core.internal.boxes;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.vex.core.internal.core.Color;
import org.eclipse.vex.core.internal.core.Graphics;
import org.eclipse.vex.core.internal.core.Rectangle;

public class InlineFrame extends BaseBox implements IInlineBox, IDecoratorBox<IInlineBox> {

	private IBox parent;
	private int top;
	private int left;
	private int width;
	private int height;
	private int maxWidth;

	private Margin margin = Margin.NULL;
	private Border border = Border.NULL;
	private Padding padding = Padding.NULL;
	private Color backgroundColor = null;

	private IInlineBox component;

	@Override
	public void setParent(final IBox parent) {
		this.parent = parent;
	}

	@Override
	public IBox getParent() {
		return parent;
	}

	@Override
	public int getAbsoluteTop() {
		if (parent == null) {
			return top;
		}
		return parent.getAbsoluteTop() + top;
	}

	@Override
	public int getAbsoluteLeft() {
		if (parent == null) {
			return left;
		}
		return parent.getAbsoluteLeft() + left;
	}

	@Override
	public int getTop() {
		return top;
	}

	@Override
	public int getLeft() {
		return left;
	}

	@Override
	public void setPosition(final int top, final int left) {
		this.top = top;
		this.left = left;
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}

	@Override
	public int getBaseline() {
		if (component == null) {
			return 0;
		}
		return component.getTop() + component.getBaseline();
	}

	@Override
	public int getMaxWidth() {
		return maxWidth;
	}

	@Override
	public void setMaxWidth(final int maxWidth) {
		this.maxWidth = maxWidth;
	}

	@Override
	public int getInvisibleGapAtStart(final Graphics graphics) {
		if (component == null) {
			return 0;
		}
		return component.getInvisibleGapAtStart(graphics);
	}

	@Override
	public int getInvisibleGapAtEnd(final Graphics graphics) {
		if (component == null) {
			return 0;
		}
		return component.getInvisibleGapAtEnd(graphics);
	}

	@Override
	public LineWrappingRule getLineWrappingAtStart() {
		if (component == null) {
			return LineWrappingRule.ALLOWED;
		}
		return component.getLineWrappingAtStart();
	}

	@Override
	public LineWrappingRule getLineWrappingAtEnd() {
		if (component == null) {
			return LineWrappingRule.ALLOWED;
		}
		return component.getLineWrappingAtEnd();
	}

	@Override
	public boolean requiresSplitForLineWrapping() {
		if (component == null) {
			return false;
		}
		return component.requiresSplitForLineWrapping();
	}

	@Override
	public Rectangle getBounds() {
		return new Rectangle(left, top, width, height);
	}

	@Override
	public void accept(final IBoxVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public <T> T accept(final IBoxVisitorWithResult<T> visitor) {
		return visitor.visit(this);
	}

	public Margin getMargin() {
		return margin;
	}

	public void setMargin(final Margin margin) {
		this.margin = margin;
	}

	public Border getBorder() {
		return border;
	}

	public void setBorder(final Border border) {
		this.border = border;
	}

	public Padding getPadding() {
		return padding;
	}

	public void setPadding(final Padding padding) {
		this.padding = padding;
	}

	public Color getBackgroundColor() {
		return backgroundColor;
	}

	public void setBackgroundColor(final Color backgroundColor) {
		this.backgroundColor = backgroundColor;
	}

	@Override
	public void setComponent(final IInlineBox component) {
		this.component = component;
		component.setParent(this);
	}

	@Override
	public IInlineBox getComponent() {
		return component;
	}

	@Override
	public void layout(final Graphics graphics) {
		if (component == null) {
			width = 0;
			height = 0;
			return;
		}

		layoutComponent(graphics);
		calculateBounds();
	}

	private void calculateBounds() {
		if (component == null || component.getWidth() == 0 || component.getHeight() == 0) {
			height = 0;
			width = 0;
		} else {
			height = topFrame(component.getHeight()) + component.getHeight() + bottomFrame(component.getHeight());
			width = leftFrame(component.getWidth()) + component.getWidth() + rightFrame(component.getWidth());
		}
	}

	private void layoutComponent(final Graphics graphics) {
		component.setMaxWidth(maxWidth);
		component.layout(graphics);
		component.setPosition(topFrame(component.getHeight()), leftFrame(component.getWidth()));
	}

	@Override
	public Collection<IBox> reconcileLayout(final Graphics graphics) {
		final int oldHeight = height;
		final int oldWidth = width;

		calculateBounds();

		if (oldHeight != height || oldWidth != width) {
			return Collections.singleton(getParent());
		} else {
			return NOTHING_INVALIDATED;
		}
	}

	private int topFrame(final int componentHeight) {
		return margin.top.get(componentHeight) + border.top.width + padding.top.get(componentHeight);
	}

	private int leftFrame(final int componentWidth) {
		return margin.left.get(componentWidth) + border.left.width + padding.left.get(componentWidth);
	}

	private int bottomFrame(final int componentHeight) {
		return margin.bottom.get(componentHeight) + border.bottom.width + padding.bottom.get(componentHeight);
	}

	private int rightFrame(final int componentWidth) {
		return margin.right.get(componentWidth) + border.right.width + padding.right.get(componentWidth);
	}

	@Override
	public void paint(final Graphics graphics) {
		drawBackground(graphics);
		drawBorder(graphics);
		paintComponent(graphics);
	}

	private void drawBackground(final Graphics graphics) {
		if (backgroundColor == null) {
			return;
		}

		graphics.setBackground(graphics.getColor(backgroundColor));
		graphics.fillRect(0, 0, width, height);
	}

	private void drawBorder(final Graphics graphics) {
		final int rectTop = margin.top.get(component.getHeight()) + border.top.width / 2;
		final int rectLeft = margin.left.get(component.getWidth()) + border.left.width / 2;
		final int rectBottom = height - margin.bottom.get(component.getHeight()) - border.bottom.width / 2;
		final int rectRight = width - margin.right.get(component.getWidth()) - border.right.width / 2;

		drawBorderLine(graphics, border.top, rectTop, rectLeft - border.left.width / 2, rectTop, rectRight + border.right.width / 2);
		drawBorderLine(graphics, border.left, rectTop - border.top.width / 2, rectLeft, rectBottom + border.bottom.width / 2, rectLeft);
		drawBorderLine(graphics, border.bottom, rectBottom, rectLeft - border.left.width / 2, rectBottom, rectRight + border.right.width / 2);
		drawBorderLine(graphics, border.right, rectTop - border.top.width / 2, rectRight, rectBottom + border.bottom.width / 2, rectRight);
	}

	private void drawBorderLine(final Graphics graphics, final BorderLine borderLine, final int top, final int left, final int bottom, final int right) {
		if (borderLine.width <= 0) {
			return;
		}
		graphics.setLineStyle(borderLine.style);
		graphics.setLineWidth(borderLine.width);
		graphics.setColor(graphics.getColor(borderLine.color));
		graphics.drawLine(left, top, right, bottom);
	}

	private void paintComponent(final Graphics graphics) {
		ChildBoxPainter.paint(component, graphics);
	}

	@Override
	public boolean canJoin(final IInlineBox other) {
		if (!(other instanceof InlineFrame)) {
			return false;
		}
		final InlineFrame otherFrame = (InlineFrame) other;

		if (!margin.equals(otherFrame.margin) || !border.equals(otherFrame.border) || !padding.equals(otherFrame.padding)) {
			return false;
		}
		if (backgroundColor == null && otherFrame.backgroundColor != null || backgroundColor != null && !backgroundColor.equals(otherFrame.backgroundColor)) {
			return false;
		}
		if (!component.canJoin(otherFrame.component)) {
			return false;
		}

		return true;
	}

	@Override
	public boolean join(final IInlineBox other) {
		if (!canJoin(other)) {
			return false;
		}
		final InlineFrame otherFrame = (InlineFrame) other;

		component.join(otherFrame.component);

		calculateBounds();

		return true;
	}

	@Override
	public boolean canSplit() {
		if (component == null) {
			return false;
		}
		return component.canSplit();
	}

	@Override
	public IInlineBox splitTail(final Graphics graphics, final int headWidth, final boolean force) {
		final IInlineBox tailComponent;
		final int tailHeadWidth = headWidth - leftFrame(component.getWidth());
		if (tailHeadWidth < 0) {
			tailComponent = component;
			component = null;
		} else {
			tailComponent = component.splitTail(graphics, tailHeadWidth, force);
		}

		final InlineFrame tail = new InlineFrame();
		tail.setComponent(tailComponent);
		tail.setParent(parent);
		tail.setMargin(margin);
		tail.setBorder(border);
		tail.setPadding(padding);
		tail.setBackgroundColor(backgroundColor);
		tail.layout(graphics);

		layout(graphics);

		return tail;
	}

	@Override
	public String toString() {
		return "InlineFrame [top=" + top + ", left=" + left + ", width=" + width + ", height=" + height + ", margin=" + margin + ", border=" + border + ", padding=" + padding + "]";
	}

}
