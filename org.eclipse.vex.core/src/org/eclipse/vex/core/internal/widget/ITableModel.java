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
package org.eclipse.vex.core.internal.widget;

import org.eclipse.vex.core.internal.css.StyleSheet;

/**
 * TODO This is only an intermediate solution to provide the stylesheet to all table-related functions. We need an
 * abstraction of the table model in order to remove the direct dependency to CSS.
 *
 * @author Florian Thienel
 */
public interface ITableModel {

	@Deprecated
	StyleSheet getStyleSheet();

}
