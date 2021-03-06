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
package org.eclipse.vex.core.internal.cursor;

import org.eclipse.vex.core.provisional.dom.ContentRange;

public interface ICursor {

	int getOffset();

	boolean hasSelection();

	ContentRange getSelectedRange();

	void addPositionListener(ICursorPositionListener listener);

	void removePositionListener(ICursorPositionListener listener);

	void move(ICursorMove move);

	void select(ICursorMove move);

}