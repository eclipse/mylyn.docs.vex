/*******************************************************************************
 * Copyright (c) 2015 Florian Thienel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		Florian Thienel - initial API and implementation
 *******************************************************************************/
package org.eclipse.vex.core.internal.widget.swt;

import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Scrollable;
import org.eclipse.vex.core.internal.core.Graphics;

/**
 * This class implements double buffering with a dedicated render thread for SWT. This prevents flickering and keeps the
 * UI responsive.<br/>
 *
 * <b>CAUTION:</b> The prevention of flickering works only in conjunction with the style bit SWT.NO_BACKGROUND.
 *
 * @see http://git.eclipse.org/c/platform/eclipse.platform.swt.git/tree/examples/org.eclipse.swt.snippets/src/org
 *      /eclipse/swt/snippets/Snippet48.java
 * @author Florian Thienel
 */
public class DoubleBufferedRenderer {

	private final Scrollable control;

	private Image bufferImage;
	private final Object bufferMonitor = new Object();

	public DoubleBufferedRenderer(final Scrollable control) {
		this.control = control;
		control.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(final DisposeEvent e) {
				DoubleBufferedRenderer.this.widgetDisposed(e);
			}
		});
		control.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(final PaintEvent e) {
				DoubleBufferedRenderer.this.paintControl(e);
			}
		});
	}

	private void widgetDisposed(final DisposeEvent e) {
		synchronized (bufferMonitor) {
			if (bufferImage != null) {
				bufferImage.dispose();
			}
			bufferImage = null;
		}
	}

	private void paintControl(final PaintEvent event) {
		event.gc.drawImage(getBufferImage(), 0, 0);
	}

	private Image getBufferImage() {
		synchronized (bufferMonitor) {
			if (bufferImage == null) {
				bufferImage = createImage();
			}
			return bufferImage;
		}
	}

	public void render(final IRenderStep... steps) {
		final Image image = createImage();
		final GC gc = new GC(image);
		final Graphics graphics = new SwtGraphics(gc);

		moveOriginToViewport(graphics);

		try {
			for (final IRenderStep step : steps) {
				step.render(graphics);
			}
		} finally {
			graphics.dispose();
			gc.dispose();
		}

		makeRenderedImageVisible(image);
	}

	private Image createImage() {
		return new Image(control.getDisplay(), control.getSize().x, control.getSize().y);
	}

	private void moveOriginToViewport(final Graphics graphics) {
		graphics.moveOrigin(0, -control.getVerticalBar().getSelection());
	}

	private void makeRenderedImageVisible(final Image newImage) {
		final Image oldImage = swapBufferImage(newImage);
		if (oldImage != null) {
			oldImage.dispose();
		}
		control.getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				control.redraw();
			}
		});
	}

	private Image swapBufferImage(final Image newImage) {
		synchronized (bufferMonitor) {
			final Image oldImage = bufferImage;
			bufferImage = newImage;
			return oldImage;
		}
	}

	public static interface IRenderStep {
		void render(Graphics graphics);
	}

}
