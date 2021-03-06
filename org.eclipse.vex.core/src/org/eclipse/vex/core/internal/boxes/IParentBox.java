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
package org.eclipse.vex.core.internal.boxes;

import java.util.Collection;

/**
 * @author Florian Thienel
 */
public interface IParentBox<T extends IBox> extends IBox {

	boolean hasChildren();

	void prependChild(T child);

	void appendChild(T child);

	void replaceChildren(Collection<? extends IBox> oldChildren, T newChild);

	Iterable<T> getChildren();

}
