/*******************************************************************************
 * Copyright (c) 2004, 2014 John Krasnay and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     John Krasnay - initial API and implementation
 *     Florian Thienel - use IVexWidget.unwrap instead of explicit implementation
 *******************************************************************************/
package org.eclipse.vex.ui.internal.handlers;

import java.util.Map;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;
import org.eclipse.vex.core.internal.widget.IDocumentEditor;

/**
 * Removes the current tag: deletes the element but adds its content to the parent element.
 */
public class RemoveTagHandler extends AbstractVexWidgetHandler implements IElementUpdater {

	/** ID of the corresponding remove element command. */
	public static final String COMMAND_ID = "org.eclipse.vex.ui.RemoveTagCommand"; //$NON-NLS-1$

	private static final String WINDOW_SCOPE_DYNAMIC_LABEL_ID = "command.removeTag.inRemoveMenu.dynamicName"; //$NON-NLS-1$
	private static final String PARTSITE_SCOPE_DYNAMIC_LABEL_ID = "command.removeTag.dynamicName"; //$NON-NLS-1$

	@Override
	public void execute(ExecutionEvent event, final IDocumentEditor editor) throws ExecutionException {
		if (editor.canUnwrap()) {
			editor.unwrap();
		}
	}

	@Override
	public void updateElement(final UIElement element, @SuppressWarnings("rawtypes") final Map parameters) {
		updateElement(element, parameters, WINDOW_SCOPE_DYNAMIC_LABEL_ID, PARTSITE_SCOPE_DYNAMIC_LABEL_ID);
	}

}
