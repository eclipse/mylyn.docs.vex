/*******************************************************************************
 * Copyright (c) 2004, 2008 John Krasnay and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     John Krasnay - initial API and implementation
 *     Igor Jacy Lino Campista - Java 5 warnings fixed (bug 311325)
 *******************************************************************************/
package org.eclipse.vex.ui.internal.handlers;

import java.util.NoSuchElementException;

import org.eclipse.vex.core.internal.widget.IDocumentEditor;
import org.eclipse.vex.core.provisional.dom.ContentPosition;
import org.eclipse.vex.core.provisional.dom.IElement;

/**
 * Navigates to the next table cell (usual shortcut: {@code Tab}).
 *
 * @see PreviousTableCellHandler
 */
public class NextTableCellHandler extends AbstractNavigateTableCellHandler {

	@Override
	protected void navigate(final IDocumentEditor editor, final IElement tableRow, final ContentPosition position) {
		// in this row
		for (final IElement cell : tableRow.childElements()) {
			if (cell.getStartPosition().isAfter(position)) {
				editor.moveTo(cell.getStartPosition().moveBy(1));
				return;
			}
		}

		// in other row
		for (final IElement siblingRow : tableRow.getParentElement().childElements()) {
			if (siblingRow.getStartPosition().isAfter(position)) {
				final IElement firstCell = firstCellOf(siblingRow);

				if (firstCell != null) {
					editor.moveTo(firstCell.getStartPosition().moveBy(1));
				} else {
					System.out.println("TODO - dup row into new empty row");
				}
				return;
			}
		}

		// We didn't find a "next row", so let's dup the current one
		VexHandlerUtil.duplicateTableRow(editor, tableRow, false);
	}

	private static IElement firstCellOf(final IElement tableRow) {
		try {
			return tableRow.childElements().first();
		} catch (final NoSuchElementException e) {
			return null;
		}
	}

}
