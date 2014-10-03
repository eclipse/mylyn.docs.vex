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
package org.eclipse.vex.core.internal.widget.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.vex.core.internal.boxes.Border;
import org.eclipse.vex.core.internal.boxes.HorizontalBar;
import org.eclipse.vex.core.internal.boxes.Margin;
import org.eclipse.vex.core.internal.boxes.Padding;
import org.eclipse.vex.core.internal.boxes.RootBox;
import org.eclipse.vex.core.internal.boxes.VerticalBlock;
import org.eclipse.vex.core.internal.core.Color;
import org.eclipse.vex.core.internal.core.Graphics;

/**
 * A widget to show the new box model.
 *
 * @author Florian Thienel
 */
public class BoxWidget extends Canvas {

	private/* final */RootBox rootBox;

	public BoxWidget(final Composite parent, final int style) {
		super(parent, style | SWT.NO_BACKGROUND);
		connectDispose();
		connectPaintControl();
		connectResize();
		if ((style & SWT.V_SCROLL) == SWT.V_SCROLL) {
			connectScrollVertically();
		}

		rootBox = new RootBox();
		for (int i = 0; i < 500000; i += 1) {
			final HorizontalBar bar = new HorizontalBar();
			bar.setHeight(10);
			bar.setColor(Color.BLACK);
			final VerticalBlock block = new VerticalBlock();
			block.appendChild(bar);
			block.setMargin(new Margin(10, 20, 30, 40));
			block.setBorder(new Border(10));
			block.setPadding(new Padding(15, 25, 35, 45));
			rootBox.appendChild(block);
		}
	}

	private void connectDispose() {
		addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(final DisposeEvent e) {
				BoxWidget.this.widgetDisposed();
			}
		});
	}

	private void connectPaintControl() {
		addPaintListener(new PaintListener() {
			@Override
			public void paintControl(final PaintEvent e) {
				BoxWidget.this.paintControl(e);
			}
		});
	}

	private void connectResize() {
		addControlListener(new ControlListener() {
			@Override
			public void controlResized(final ControlEvent e) {
				resize(e);
			}

			@Override
			public void controlMoved(final ControlEvent e) {
				// ignore
			}
		});
	}

	private void connectScrollVertically() {
		getVerticalBar().addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				scrollVertically(e);
			}

			@Override
			public void widgetDefaultSelected(final SelectionEvent e) {
				// ignore
			}
		});
	}

	private void widgetDisposed() {
		rootBox = null;
	}

	private void paintControl(final PaintEvent event) {
		/*
		 * Use double buffering to render the box model to prevent flickering e.g. while scrolling. This works only in
		 * conjunction with the style bit SWT.NO_BACKGROUND.
		 * 
		 * @see
		 * http://git.eclipse.org/c/platform/eclipse.platform.swt.git/tree/examples/org.eclipse.swt.snippets/src/org
		 * /eclipse/swt/snippets/Snippet48.java
		 */
		final Image bufferImage = new Image(getDisplay(), getSize().x, getSize().y);
		final GC bufferGC = new GC(bufferImage);

		final Graphics graphics = new SwtGraphics(bufferGC);
		graphics.moveOrigin(0, -getVerticalBar().getSelection());

		System.out.print("Painting ");
		final long start = System.currentTimeMillis();
		rootBox.paint(graphics);
		System.out.println("took " + (System.currentTimeMillis() - start));

		event.gc.drawImage(bufferImage, 0, 0);

		graphics.dispose();
		bufferGC.dispose();
		bufferImage.dispose();
	}

	private void resize(final ControlEvent event) {
		rootBox.setWidth(getClientArea().width);

		System.out.print("Layout ");
		final long start = System.currentTimeMillis();
		rootBox.layout();
		System.out.println("took " + (System.currentTimeMillis() - start));

		updateVerticalBar();
	}

	private void updateVerticalBar() {
		final int maximum = rootBox.getHeight();
		final int pageSize = getClientArea().height;
		getVerticalBar().setValues(0, 0, maximum, pageSize, 1, pageSize);
	}

	private void scrollVertically(final SelectionEvent event) {
		redraw();
	}
}
