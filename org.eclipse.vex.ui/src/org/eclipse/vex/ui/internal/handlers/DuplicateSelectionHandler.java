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
import org.eclipse.vex.core.internal.dom.Element;
import org.eclipse.vex.ui.internal.swt.VexWidget;

/**
 * Duplicates current element or current selection.
 */
public class DuplicateSelectionHandler extends AbstractVexWidgetHandler {

	@Override
	public void execute(final VexWidget widget) throws ExecutionException {
		widget.doWork(new Runnable() {
			public void run() {
				if (!widget.hasSelection()) {
					final Element element = widget.getCurrentElement();

					// Can't duplicate the root element
					if (element.getParent() == null) {
						return;
					}

					widget.moveTo(element.getStartOffset());
					widget.moveTo(element.getEndOffset() + 1, true);
				}

				widget.copySelection();
				final int startOffset = widget.getSelectedRange().getEndOffset() + 1;
				widget.moveTo(startOffset);
				widget.paste();
				final int endOffset = widget.getCaretOffset();
				widget.moveTo(startOffset);
				widget.moveTo(endOffset, true);
			}
		});
	}

}
