/*******************************************************************************
 * Copyright (c) 2014 Florian Thienel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		Florian Thienel - initial API and implementation
 *******************************************************************************/
package org.eclipse.vex.core.internal.cursor;

import org.eclipse.vex.core.internal.boxes.BaseBoxVisitorWithResult;
import org.eclipse.vex.core.internal.boxes.DepthFirstBoxTraversal;
import org.eclipse.vex.core.internal.boxes.IBox;
import org.eclipse.vex.core.internal.boxes.IContentBox;
import org.eclipse.vex.core.internal.boxes.ParentTraversal;
import org.eclipse.vex.core.internal.boxes.RootBox;
import org.eclipse.vex.core.internal.boxes.StructuralNodeReference;
import org.eclipse.vex.core.internal.boxes.TextContent;
import org.eclipse.vex.core.provisional.dom.ContentRange;

/**
 * @author Florian Thienel
 */
public class ContentTopology {

	private RootBox rootBox;
	private IContentBox outmostContentBox;

	public void setRootBox(final RootBox rootBox) {
		this.rootBox = rootBox;
		outmostContentBox = findOutmostContentBox(rootBox);
	}

	private static IContentBox findOutmostContentBox(final RootBox rootBox) {
		return rootBox.accept(new DepthFirstBoxTraversal<IContentBox>(null) {
			@Override
			public IContentBox visit(final StructuralNodeReference box) {
				return box;
			}

			@Override
			public IContentBox visit(final TextContent box) {
				return box;
			}
		});
	}

	public int getLastOffset() {
		if (outmostContentBox == null) {
			return 0;
		}
		return outmostContentBox.getEndOffset();
	}

	public IContentBox getOutmostContentBox() {
		return outmostContentBox;
	}

	public IContentBox findBoxForPosition(final int offset) {
		return rootBox.accept(new DepthFirstBoxTraversal<IContentBox>() {
			@Override
			public IContentBox visit(final StructuralNodeReference box) {
				if (box.getStartOffset() == offset || box.getEndOffset() == offset) {
					return box;
				}
				if (box.getStartOffset() < offset && box.getEndOffset() > offset) {
					return box.getComponent().accept(this);
				}
				return null;
			}

			@Override
			public IContentBox visit(final TextContent box) {
				if (box.getStartOffset() <= offset && box.getEndOffset() >= offset) {
					return box;
				}
				return null;
			}
		});
	}

	public IContentBox findBoxForRange(final ContentRange range) {
		return rootBox.accept(new DepthFirstBoxTraversal<IContentBox>() {
			@Override
			public IContentBox visit(final StructuralNodeReference box) {
				if (box.getRange().contains(range)) {
					final IContentBox childBox = box.getComponent().accept(this);
					if (childBox == null) {
						return box;
					} else {
						return childBox;
					}
				}

				return null;
			}

			@Override
			public IContentBox visit(final TextContent box) {
				if (box.getRange().contains(range)) {
					return box;
				}
				return null;
			}
		});
	}

	public IContentBox findBoxForCoordinates(final int x, final int y) {
		if (outmostContentBox == null) {
			return null;
		}

		return outmostContentBox.accept(new DepthFirstBoxTraversal<IContentBox>() {
			@Override
			public IContentBox visit(final StructuralNodeReference box) {
				if (!box.containsCoordinates(x, y)) {
					return null;
				}
				final IContentBox deeperContainer = super.visit(box);
				if (deeperContainer != null) {
					return deeperContainer;
				}
				return box;
			}

			@Override
			public IContentBox visit(final TextContent box) {
				if (!box.containsCoordinates(x, y)) {
					return null;
				}
				return box;
			}
		});
	}

	public static IContentBox getParentContentBox(final IContentBox childBox) {
		return childBox.accept(new ParentTraversal<IContentBox>() {
			@Override
			public IContentBox visit(final StructuralNodeReference box) {
				if (box == childBox) {
					return super.visit(box);
				}
				return box;
			}
		});
	}

	public static int verticalDistance(final IContentBox box, final int y) {
		return box.accept(new BaseBoxVisitorWithResult<Integer>(0) {
			@Override
			public Integer visit(final StructuralNodeReference box) {
				return Math.abs(y - box.getAbsoluteTop() - box.getHeight());
			}

			@Override
			public Integer visit(final TextContent box) {
				return Math.abs(y - box.getAbsoluteTop() - box.getBaseline());
			}
		});
	}

	public static IContentBox findHorizontallyClosestContentBox(final Iterable<IContentBox> candidates, final int x) {
		IContentBox finalCandidate = null;
		int minHorizontalDistance = Integer.MAX_VALUE;
		for (final IContentBox candidate : candidates) {
			final int distance = horizontalDistance(candidate, x);
			if (distance < minHorizontalDistance) {
				finalCandidate = candidate;
				minHorizontalDistance = distance;
			}
		}
		return finalCandidate;
	}

	public static int horizontalDistance(final IBox box, final int x) {
		if (box.getAbsoluteLeft() > x) {
			return box.getAbsoluteLeft() - x;
		}
		if (box.getAbsoluteLeft() + box.getWidth() < x) {
			return x - box.getAbsoluteLeft() - box.getWidth();
		}
		return 0;
	}

}