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
package org.eclipse.vex.ui.internal.handlers;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.vex.core.internal.widget.swt.VexWidget;
import org.eclipse.vex.core.provisional.dom.ContentRange;
import org.eclipse.vex.ui.internal.handlers.VexHandlerUtil.SelectedRows;

/**
 * Moves the current table row either down below its next sibling or up above its previous sibling.
 * 
 * @see MoveRowUpHandler
 * @see MoveRowDownHandler
 */
public abstract class AbstractMoveRowHandler extends AbstractVexWidgetHandler {

	@Override
	public void execute(final VexWidget widget) throws ExecutionException {
		final VexHandlerUtil.SelectedRows selected = VexHandlerUtil.getSelectedTableRows(widget);

		if (selected.getRows() == null || targetRow(selected) == null) {
			return;
		}

		widget.doWork(new Runnable() {
			public void run() {
				final ContentRange range = VexHandlerUtil.getOuterRange(targetRow(selected));
				widget.moveTo(range.getStartOffset());
				widget.moveTo(range.getEndOffset(), true);
				widget.cutSelection();

				widget.moveTo(target(selected));
				widget.paste();
			}

		}, true);
	}

	/**
	 * @param selected
	 *            current selected row
	 * @return the row with which to switch current row
	 */
	protected abstract Object targetRow(SelectedRows selected);

	/**
	 * @param selected
	 *            current selected row
	 * @return offset where to move to
	 */
	protected abstract int target(SelectedRows selected);

}
