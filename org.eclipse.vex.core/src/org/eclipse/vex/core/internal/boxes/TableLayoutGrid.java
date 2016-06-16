/*******************************************************************************
 * Copyright (c) 2016 Florian Thienel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		Florian Thienel - initial API and implementation
 *******************************************************************************/
package org.eclipse.vex.core.internal.boxes;

import java.util.HashMap;

import org.eclipse.vex.core.internal.core.Graphics;

/**
 * @author Florian Thienel
 */
public class TableLayoutGrid {

	private final HashMap<GridPosition, TableCell> grid = new HashMap<GridPosition, TableCell>();
	private int currentRow = 0;
	private int nextColumn = 1;
	private int maxColumn = 0;

	public static void setupLayoutGrid(final Graphics graphics, final IStructuralBox parent, final TableLayoutGrid layoutGrid) {
		// TODO merge with TableColumnLayout
		parent.accept(new DepthFirstBoxTraversal<Object>() {
			@Override
			public Object visit(final Table box) {
				if (box == parent) {
					traverseChildren(box);
				} else {
					box.setLayoutGrid(layoutGrid);
				}
				return null;
			}

			@Override
			public Object visit(final TableRowGroup box) {
				if (box == parent) {
					traverseChildren(box);
				} else {
					box.setLayoutGrid(layoutGrid);
				}
				return null;
			}

			@Override
			public Object visit(final TableColumnSpec box) {
				// TODO Auto-generated method stub
				return super.visit(box);
			}

			@Override
			public Object visit(final TableRow box) {
				if (box == parent) {
					layoutGrid.addNextRow(box);
					traverseChildren(box);
				} else {
					box.setLayoutGrid(layoutGrid);
				}
				return null;
			}

			@Override
			public Object visit(final TableCell box) {
				if (box.getStartColumnIndex() > 0 && box.getEndColumnIndex() > 0) {
					layoutGrid.addCellOnCurrentRow(box, box.getStartColumnIndex(), box.getEndColumnIndex());
				} else {
					layoutGrid.addNextCellOnCurrentRow(box);
				}
				return null;
			}
		});
	}

	public int getRows() {
		return currentRow;
	}

	public int getColumns() {
		return maxColumn;
	}

	public int addNextRow(final TableRow row) {
		currentRow += 1;
		row.setRowIndex(currentRow);
		nextColumn = 1;
		return currentRow;
	}

	public boolean addCellOnCurrentRow(final TableCell cell, final int column) {
		final GridArea area = new GridArea(currentRow, column, currentRow + cell.getVerticalSpan() - 1, column);
		if (isOccupied(area)) {
			return false;
		}
		occupy(area, cell);
		cell.setGridArea(area);
		updateNextColumn();
		return true;
	}

	public boolean addCellOnCurrentRow(final TableCell cell, final int startColumn, final int endColumn) {
		final GridArea area = new GridArea(currentRow, startColumn, currentRow + cell.getVerticalSpan() - 1, endColumn);
		if (isOccupied(area)) {
			return false;
		}
		occupy(area, cell);
		cell.setGridArea(area);
		updateNextColumn();
		return true;
	}

	public void addNextCellOnCurrentRow(final TableCell cell) {
		final GridArea area = new GridArea(currentRow, nextColumn, currentRow + cell.getVerticalSpan() - 1, nextColumn);
		occupy(area, cell);
		cell.setGridArea(area);
		updateNextColumn();
	}

	private void updateNextColumn() {
		while (isOccupied(new GridPosition(currentRow, nextColumn))) {
			nextColumn += 1;
		}
	}

	private void occupy(final GridArea area, final TableCell cell) {
		for (final GridPosition position : area.positions()) {
			grid.put(position, cell);
			maxColumn = Math.max(maxColumn, position.column);
		}
	}

	private boolean isOccupied(final GridArea area) {
		for (final GridPosition position : area.positions()) {
			if (isOccupied(position)) {
				return true;
			}
		}
		return false;
	}

	private boolean isOccupied(final GridPosition position) {
		return grid.containsKey(position);
	}

	public TableCell getCell(final GridPosition position) {
		return grid.get(position);
	}

	public IStructuralBox getRowChild(final GridPosition position) {
		final TableCell cell = grid.get(position);
		if (cell == null) {
			return null;
		}

		return cell.accept(new ParentTraversal<IStructuralBox>() {
			@Override
			public IStructuralBox visit(final HorizontalBar box) {
				return visitStructuralBox(box);
			}

			@Override
			public IStructuralBox visit(final List box) {
				return visitStructuralBox(box);
			}

			@Override
			public IStructuralBox visit(final ListItem box) {
				return visitStructuralBox(box);
			}

			@Override
			public IStructuralBox visit(final Paragraph box) {
				return visitStructuralBox(box);
			}

			@Override
			public IStructuralBox visit(final StructuralFrame box) {
				return visitStructuralBox(box);
			}

			@Override
			public IStructuralBox visit(final StructuralNodeReference box) {
				return visitStructuralBox(box);
			}

			@Override
			public IStructuralBox visit(final Table box) {
				return visitStructuralBox(box);
			}

			@Override
			public IStructuralBox visit(final TableCell box) {
				return visitStructuralBox(box);
			}

			@Override
			public IStructuralBox visit(final TableColumnSpec box) {
				return visitStructuralBox(box);
			}

			@Override
			public IStructuralBox visit(final TableRow box) {
				return null;
			}

			@Override
			public IStructuralBox visit(final TableRowGroup box) {
				return visitStructuralBox(box);
			}

			@Override
			public IStructuralBox visit(final VerticalBlock box) {
				return visitStructuralBox(box);
			}

			private IStructuralBox visitStructuralBox(final IStructuralBox box) {
				if (box.getParent() instanceof TableRow) {
					return box;
				}
				return box.getParent().accept(this);
			}
		});
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		for (int row = 1; row <= currentRow; row += 1) {
			final StringBuilder top = new StringBuilder();
			final StringBuilder middle = new StringBuilder();
			final StringBuilder bottom = new StringBuilder();
			for (int column = 1; column <= maxColumn; column += 1) {
				final GridPosition position = new GridPosition(row, column);
				final TableCell cell = grid.get(position);
				if (cell == null) {
					top.append("     ");
					middle.append("     ");
					bottom.append("     ");
				} else {
					final GridArea gridArea = cell.getGridArea();
					if (row == gridArea.startRow) {
						top.append("-----");
					} else {
						top.append("     ");
					}
					if (row == gridArea.endRow) {
						bottom.append("-----");
					} else {
						bottom.append("     ");
					}
					if (column == gridArea.startColumn) {
						middle.append("|");
					} else {
						middle.append(" ");
					}
					middle.append(Long.toString(cell.hashCode()).substring(0, 3));
					if (column == gridArea.endColumn) {
						middle.append("|");
					} else {
						middle.append(" ");
					}
				}
			}
			builder.append(top.toString()).append("\n");
			builder.append(middle.toString()).append("\n");
			builder.append(bottom.toString()).append("\n");
		}
		return builder.toString();
	}
}