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
package org.eclipse.vex.core.internal.css;

import java.net.URI;
import java.net.URISyntaxException;

public class URIContent implements IPropertyContent {

	public final String uri;

	public URIContent(final String uri) {
		this.uri = uri;
	}

	@Override
	public <T> T accept(final IPropertyContentVisitor<T> visitor) {
		return visitor.visit(this);
	}

	@Override
	public String toString() {
		return uri.toString();
	}

	public URI uriValue() throws URISyntaxException {
		return new URI(uri);
	}
}
