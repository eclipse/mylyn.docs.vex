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

import org.eclipse.vex.core.internal.boxes.ContentMap.Environment;
import org.eclipse.vex.core.internal.boxes.ContentMap.Neighbour;
import org.eclipse.vex.core.internal.core.Graphics;
import org.eclipse.vex.core.internal.core.Rectangle;

/**
 * @author Florian Thienel
 */
public class MoveDown implements ICursorMove {

	@Override
	public int calculateNewOffset(final Graphics graphics, final ContentMap contentMap, final int currentOffset, final IContentBox currentBox, final Rectangle hotArea, final int preferredX) {
		if (isAtStartOfEmptyBox(currentOffset, currentBox)) {
			return currentBox.getEndOffset();
		}
		if (isAtStartOfBoxWithChildren(currentOffset, currentBox) && !canContainText(currentBox)) {
			return getFirstChild(currentBox).getStartOffset();
		}

		final int x = preferredX;
		final int y = hotArea.getY() + hotArea.getHeight() - 1;
		final IContentBox box = findClosestBoxBelow(contentMap, currentBox, x, y);
		if (box.isEmpty()) {
			return box.getStartOffset();
		}
		return box.getOffsetForCoordinates(graphics, x - box.getAbsoluteLeft(), y - box.getAbsoluteTop());
	}

	@Override
	public boolean preferX() {
		return false;
	}

	private static boolean isAtStartOfEmptyBox(final int offset, final IContentBox box) {
		return isAtStartOfBox(offset, box) && box.isEmpty();
	}

	private static boolean isAtStartOfBoxWithChildren(final int offset, final IContentBox box) {
		return isAtStartOfBox(offset, box) && canHaveChildren(box);
	}

	private static boolean isAtStartOfBox(final int offset, final IContentBox box) {
		return offset == box.getStartOffset();
	}

	private static boolean canHaveChildren(final IContentBox box) {
		return box.accept(new BaseBoxVisitorWithResult<Boolean>(false) {
			@Override
			public Boolean visit(final NodeReference box) {
				return true;
			}
		});
	}

	private static boolean canContainText(final IContentBox box) {
		return box.accept(new BaseBoxVisitorWithResult<Boolean>(false) {
			@Override
			public Boolean visit(final NodeReference box) {
				return box.canContainText();
			}
		});
	}

	private static IContentBox getFirstChild(final IContentBox parent) {
		return parent.accept(new DepthFirstTraversal<IContentBox>() {
			@Override
			public IContentBox visit(final NodeReference box) {
				if (box == parent) {
					return super.visit(box);
				}
				return box;
			}

			@Override
			public IContentBox visit(final TextContent box) {
				return box;
			}
		});
	}

	private static IContentBox findClosestBoxBelow(final ContentMap contentMap, final IContentBox currentBox, final int x, final int y) {
		final Environment environment = contentMap.findEnvironmentForCoordinates(x, y, true);
		final Neighbour neighbourBelow = environment.neighbours.getBelow();
		if (neighbourBelow.box == null) {
			final IContentBox parent = getParent(currentBox);
			if (parent == null) {
				return currentBox;
			}
			return parent;
		}

		final IContentBox boxBelow = neighbourBelow.box.accept(new BaseBoxVisitorWithResult<IContentBox>() {
			@Override
			public IContentBox visit(final NodeReference box) {
				if (box.canContainText()) {
					return deepestFirstChild(box);
				}
				return box;
			}

			@Override
			public IContentBox visit(final TextContent box) {
				return box;
			}
		});

		return currentBox.accept(new BaseBoxVisitorWithResult<IContentBox>() {
			@Override
			public IContentBox visit(final NodeReference box) {
				if (box == getParent(boxBelow)) {
					return boxBelow;
				}
				if (!isInLastLine(box, boxBelow)) {
					return boxBelow;
				}
				if (boxBelow.isRightOf(x)) {
					return boxBelow;
				}
				return getParent(box);
			}

			@Override
			public IContentBox visit(final TextContent box) {
				if (!boxBelow.isLeftOf(x)) {
					return boxBelow;
				}
				return getParent(box);
			}

		});
	}

	private static IContentBox deepestFirstChild(final IContentBox parentBox) {
		return parentBox.accept(new DepthFirstTraversal<IContentBox>() {
			private IContentBox firstChild;

			@Override
			public IContentBox visit(final NodeReference box) {
				super.visit(box);
				if (firstChild == null) {
					firstChild = box;
				}
				return firstChild;
			}

			@Override
			public IContentBox visit(final TextContent box) {
				return box;
			}
		});
	}

	private static boolean isInLastLine(final IContentBox box, final IContentBox boxBelow) {
		return !(getParent(box) == getParent(boxBelow));
	}

	private static IContentBox getParent(final IContentBox childBox) {
		return childBox.accept(new ParentTraversal<IContentBox>() {
			@Override
			public IContentBox visit(final NodeReference box) {
				if (box == childBox) {
					return super.visit(box);
				}
				return box;
			}
		});
	}

}
