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
package org.eclipse.vex.core.internal.widget.swt;

import org.eclipse.swt.widgets.Display;
import org.eclipse.vex.core.internal.widget.BaseClipboardTest;
import org.eclipse.vex.core.internal.widget.IClipboard;

public class SwtClipboardTest extends BaseClipboardTest {

	@Override
	protected IClipboard createClipboard() {
		return new SwtClipboard(Display.getDefault());
	}

}
