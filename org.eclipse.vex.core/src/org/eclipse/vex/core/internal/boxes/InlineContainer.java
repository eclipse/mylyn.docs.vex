package org.eclipse.vex.core.internal.boxes;

import java.util.ArrayList;

import org.eclipse.vex.core.internal.core.Graphics;
import org.eclipse.vex.core.internal.core.Rectangle;

public class InlineContainer extends BaseBox implements IInlineBox, IParentBox<IInlineBox> {

	private IBox parent;
	private int top;
	private int left;
	private int width;
	private int height;
	private int baseline;
	private final ArrayList<IInlineBox> children = new ArrayList<IInlineBox>();

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

	public int getTop() {
		return top;
	}

	public int getLeft() {
		return left;
	}

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
	public Rectangle getBounds() {
		return new Rectangle(left, top, width, height);
	}

	@Override
	public int getBaseline() {
		return baseline;
	}

	@Override
	public void accept(final IBoxVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public <T> T accept(final IBoxVisitorWithResult<T> visitor) {
		return visitor.visit(this);
	}

	@Override
	public boolean hasChildren() {
		return !children.isEmpty();
	}

	@Override
	public void appendChild(final IInlineBox child) {
		child.setParent(this);
		children.add(child);
	}

	@Override
	public Iterable<IInlineBox> getChildren() {
		return children;
	}

	@Override
	public void layout(final Graphics graphics) {
		layoutChildren(graphics);
		calculateBoundsAndBaseline();
		arrangeChildrenOnBaseline();
	}

	private void layoutChildren(final Graphics graphics) {
		for (final IInlineBox child : children) {
			child.layout(graphics);
		}
	}

	private void calculateBoundsAndBaseline() {
		width = 0;
		height = 0;
		baseline = 0;
		int descend = 0;
		for (final IInlineBox child : children) {
			width += child.getWidth();
			descend = Math.max(descend, child.getHeight() - child.getBaseline());
			baseline = Math.max(baseline, child.getBaseline());
		}
		height = baseline + descend;
	}

	private void arrangeChildrenOnBaseline() {
		int childLeft = left;
		for (final IInlineBox child : children) {
			final int childTop = baseline - child.getBaseline();
			child.setPosition(childTop, childLeft);
			childLeft += child.getWidth();
		}
	}

	@Override
	public boolean reconcileLayout(final Graphics graphics) {
		final int oldWidth = width;
		final int oldHeight = height;
		final int oldBaseline = baseline;
		calculateBoundsAndBaseline();
		return oldWidth != width || oldHeight != height || oldBaseline != baseline;
	}

	@Override
	public void paint(final Graphics graphics) {
		ChildBoxPainter.paint(children, graphics);
	}

	@Override
	public boolean canJoin(final IInlineBox other) {
		if (!(other instanceof InlineContainer)) {
			return false;
		}
		return true;
	}

	@Override
	public boolean join(final IInlineBox other) {
		if (!canJoin(other)) {
			return false;
		}
		final InlineContainer otherInlineContainer = (InlineContainer) other;

		final int mergeIndex = children.size() - 1;
		for (int i = 0; i < otherInlineContainer.children.size(); i += 1) {
			final IInlineBox child = otherInlineContainer.children.get(i);
			appendChild(child);
		}

		if (mergeIndex >= 0 && mergeIndex < children.size() - 1) {
			final IInlineBox leftChild = children.get(mergeIndex);
			final IInlineBox rightChild = children.get(mergeIndex + 1);
			if (leftChild.join(rightChild)) {
				children.remove(mergeIndex + 1);
				rightChild.setParent(null);
			}
		}
		return true;
	}

	@Override
	public boolean canSplit() {
		if (children.isEmpty()) {
			return false;
		}
		return true;
	}

	@Override
	public IInlineBox splitTail(final Graphics graphics, final int headWidth, final boolean force) {
		final int splitIndex = findChildIndexToSplitAt(headWidth);
		if (splitIndex == -1) {
			return null;
		}
		final IInlineBox splitChild = children.get(splitIndex);
		final IInlineBox splitChildTail = splitChild.splitTail(graphics, headWidth - splitChild.getLeft(), force);
		final InlineContainer tail = new InlineContainer();
		tail.setParent(parent);

		if (splitChildTail == null) {
			moveChildrenTo(tail, splitIndex);
		} else {
			tail.appendChild(splitChildTail);
			moveChildrenTo(tail, splitIndex + 1);
		}

		return tail;
	}

	private int findChildIndexToSplitAt(final int headWidth) {
		for (int i = 0; i < children.size(); i += 1) {
			final IInlineBox child = children.get(0);
			if (child.getLeft() + child.getWidth() > headWidth) {
				return i;
			}
		}
		return -1;
	}

	private void moveChildrenTo(final InlineContainer destination, final int startIndex) {
		while (startIndex < children.size()) {
			final IInlineBox child = children.get(startIndex);
			children.remove(startIndex);
			destination.appendChild(child);
		}
	}

}