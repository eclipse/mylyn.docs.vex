/*******************************************************************************
 * Copyright (c) 2004, 2013 John Krasnay and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     John Krasnay - initial API and implementation
 *     Igor Jacy Lino Campista - Java 5 warnings fixed (bug 311325)
 *     Carsten Hiesserich - do not add text nodes containing only whitespace when reading the document (bug 407803)
 *     Carsten Hiesserich - added processing instructions support
 *******************************************************************************/
package org.eclipse.vex.core.internal.io;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.vex.core.XML;
import org.eclipse.vex.core.internal.css.IStyleSheetProvider;
import org.eclipse.vex.core.internal.css.IWhitespacePolicy;
import org.eclipse.vex.core.internal.css.IWhitespacePolicyFactory;
import org.eclipse.vex.core.internal.css.StyleSheet;
import org.eclipse.vex.core.internal.dom.Comment;
import org.eclipse.vex.core.internal.dom.Document;
import org.eclipse.vex.core.internal.dom.DocumentTextPosition;
import org.eclipse.vex.core.internal.dom.Element;
import org.eclipse.vex.core.internal.dom.GapContent;
import org.eclipse.vex.core.internal.dom.IncludeNode;
import org.eclipse.vex.core.internal.dom.Namespace;
import org.eclipse.vex.core.internal.dom.Node;
import org.eclipse.vex.core.internal.dom.ProcessingInstruction;
import org.eclipse.vex.core.provisional.dom.BaseNodeVisitorWithResult;
import org.eclipse.vex.core.provisional.dom.ContentRange;
import org.eclipse.vex.core.provisional.dom.DocumentContentModel;
import org.eclipse.vex.core.provisional.dom.DocumentValidationException;
import org.eclipse.vex.core.provisional.dom.IComment;
import org.eclipse.vex.core.provisional.dom.IContent;
import org.eclipse.vex.core.provisional.dom.IDocument;
import org.eclipse.vex.core.provisional.dom.IElement;
import org.eclipse.vex.core.provisional.dom.INode;
import org.eclipse.vex.core.provisional.dom.IText;
import org.eclipse.vex.core.provisional.dom.IValidator;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ext.LexicalHandler;

/**
 * A SAX handler that builds a Vex document. This builder collapses whitespace as it goes, according to the following
 * rules.
 *
 * <ul>
 * <li>Elements with style white-space: pre are left alone.</li>
 * <li>Runs of whitespace are replaced with a single space.</li>
 * <li>Space just inside the start and end of elements is removed.</li>
 * <li>Space just outside the start and end of block-formatted elements is removed.</li>
 * </ul>
 */
public class DocumentBuilder implements ContentHandler, LexicalHandler {

	private final IValidator validator;

	private final IStyleSheetProvider styleSheetProvider;

	private final IWhitespacePolicyFactory whitespacePolicyFactory;
	private IWhitespacePolicy whitespacePolicy = IWhitespacePolicy.NULL;

	// Holds pending characters until we see another element boundary.
	// This is (a) so we can collapse spaces in multiple adjacent character
	// blocks, and (b) so we can trim trailing whitespace, if necessary.
	private final StringBuilder pendingChars = new StringBuilder();

	// If true, trim the leading whitespace from the next received block of
	// text.
	private boolean trimLeading = false;

	// Content object to hold document content
	private final IContent content = new GapContent(100);

	// Stack of StackElement objects
	private final LinkedList<StackEntry> stack = new LinkedList<StackEntry>();

	private final NamespaceStack namespaceStack = new NamespaceStack();

	private final List<Node> nodesBeforeRoot = new ArrayList<Node>();
	private final List<Node> nodesAfterRoot = new ArrayList<Node>();

	private boolean inDTD = false;

	private Element rootElement;

	private final String baseUri;
	private String dtdPublicID;
	private String dtdSystemID;
	private IDocument document;
	private Locator locator;

	private DocumentTextPosition caretPosition;
	private INode nodeAtCaret = null;

	public DocumentBuilder(final String baseUri, final IValidator validator, final IStyleSheetProvider styleSheetProvider, final IWhitespacePolicyFactory whitespacePolicyFactory) {
		this.baseUri = baseUri;
		this.validator = validator;
		this.styleSheetProvider = styleSheetProvider;
		this.whitespacePolicyFactory = whitespacePolicyFactory;
	}

	/**
	 * Returns the newly built <code>Document</code> object.
	 */
	public IDocument getDocument() {
		return document;
	}

	public IWhitespacePolicy getWhitespacePolicy() {
		return whitespacePolicy;
	}

	// ============================================= ContentHandler methods

	@Override
	public void characters(final char[] ch, final int start, final int length) throws SAXException {
		appendPendingCharsFiltered(ch, start, length);
	}

	private void appendPendingCharsFiltered(final char[] ch, final int start, final int length) {
		// Convert control characters to spaces, since we use nulls for element delimiters
		for (int i = start; i < start + length; i++) {
			if (isControlCharacter(ch[i])) {
				pendingChars.append(' ');
			} else {
				pendingChars.append(ch[i]);
			}
		}
	}

	private static boolean isControlCharacter(final char ch) {
		return Character.isISOControl(ch) && ch != '\n' && ch != '\r' && ch != '\t';
	}

	@Override
	public void endDocument() {
		if (rootElement == null) {
			return;
		}

		document = new Document(content, rootElement);
		document.setPublicID(dtdPublicID);
		document.setSystemID(dtdSystemID);

		for (final Node node : nodesBeforeRoot) {
			((Document) document).insertChildBefore(document.getRootElement(), node);
		}

		for (final Node node : nodesAfterRoot) {
			((Document) document).addChild(node);
		}
	}

	@Override
	public void endElement(final String namespaceURI, final String localName, final String qName) {
		appendChars(true);

		final StackEntry entry = stack.removeLast();

		// we must insert the trailing sentinel first, else the insertion
		// pushes the end position of the element to after the sentinel
		content.insertTagMarker(content.length());
		entry.element.associate(content, new ContentRange(entry.offset, content.length() - 1));

		if (isBlock(entry.element)) {
			trimLeading = true;
		}
	}

	@Override
	public void endPrefixMapping(final String prefix) {
	}

	@Override
	public void ignorableWhitespace(final char[] ch, final int start, final int length) {
	}

	@Override
	public void processingInstruction(final String target, final String data) {

		final ProcessingInstruction pi = new ProcessingInstruction(target);

		if (isBeforeRoot()) {
			nodesBeforeRoot.add(pi);
		} else if (isAfterRoot()) {
			nodesAfterRoot.add(pi);
		} else {
			final Element parent = stack.getLast().element;
			parent.addChild(pi);
			appendChars(false);
		}

		final int startOffset = content.length();
		content.insertTagMarker(content.length());
		content.insertText(content.length(), data);
		content.insertTagMarker(content.length());
		pi.associate(content, new ContentRange(startOffset, content.length() - 1));

		trimLeading = false; // Keep a leading whitespace after a processing instruction
	}

	@Override
	public void setDocumentLocator(final Locator locator) {
		this.locator = locator;
	}

	@Override
	public void skippedEntity(final java.lang.String name) {
	}

	@Override
	public void startDocument() {
	}

	@Override
	public void startElement(final String namespaceURI, final String localName, final String qName, final Attributes attrs) throws SAXException {

		final QualifiedName elementName;
		if ("".equals(namespaceURI)) {
			elementName = new QualifiedName(null, qName);
		} else {
			elementName = new QualifiedName(namespaceURI, localName);
		}
		Element element;
		if (stack.isEmpty()) {
			rootElement = new Element(elementName);
			element = rootElement;
		} else {
			element = new Element(elementName);

			final Element parent = stack.getLast().element;

			// We have to set the parent before accessing the CSS the first time to enable cascading.
			element.setParent(parent);
			if (isInclude(element)) {
				// Wrap the xml element in an include node
				final Node include = new IncludeNode(element);
				parent.addChild(include);
			} else {
				parent.addChild(element);
			}
		}

		if (nodeAtCaret == null && caretPosition != null) {
			// Sax line number start with 1, document line number start with 0
			if (locator.getLineNumber() >= caretPosition.getLine() + 1 && locator.getColumnNumber() >= caretPosition.getColumn()) {
				nodeAtCaret = element;
			}
		}

		final String defaultNamespaceUri = namespaceStack.peekDefault();
		if (defaultNamespaceUri != null) {
			element.declareDefaultNamespace(defaultNamespaceUri);
		}

		for (final String prefix : namespaceStack.getPrefixes()) {
			element.declareNamespace(prefix, namespaceStack.peek(prefix));
		}

		final int n = attrs.getLength();
		for (int i = 0; i < n; i++) {
			final QualifiedName attributeName;
			if ("".equals(attrs.getLocalName(i))) {
				attributeName = new QualifiedName(null, attrs.getQName(i));
			} else if ("".equals(attrs.getURI(i))) {
				// Attributes do not inherit the elements namespace (http://www.w3.org/TR/REC-xml-names/#defaulting)
				attributeName = new QualifiedName(null, attrs.getLocalName(i));
			} else {
				attributeName = new QualifiedName(attrs.getURI(i), attrs.getLocalName(i));
			}
			try {
				element.setAttribute(attributeName, attrs.getValue(i));
			} catch (final DocumentValidationException e) {
				throw new SAXParseException("DocumentValidationException", locator, e);
			}
		}

		final DocumentContentModel documentContentModel = validator.getDocumentContentModel();
		if (stack.isEmpty() && documentContentModel != null) {
			final String previousDocTypeID = documentContentModel.getMainDocumentTypeIdentifier();
			if (dtdPublicID != null && dtdSystemID != null || previousDocTypeID == null) {
				// The content model is initialized only if the input document defines a DocType
				// or if it has not been initialized already.
				// This way, a user selected doctype is reapplied when the document is reloaded.
				documentContentModel.initialize(baseUri, dtdPublicID, dtdSystemID, rootElement);
			}
			final StyleSheet styleSheet = styleSheetProvider.getStyleSheet(documentContentModel);
			whitespacePolicy = whitespacePolicyFactory.createPolicy(validator, documentContentModel, styleSheet);
		}

		appendChars(isBlock(element));

		stack.add(new StackEntry(element, content.length(), isPre(element)));
		content.insertTagMarker(content.length());

		trimLeading = true;

		namespaceStack.clear();
	}

	@Override
	public void startPrefixMapping(final String prefix, final String uri) {
		checkPrefix(prefix);
		if (isDefaultPrefix(prefix)) {
			namespaceStack.pushDefault(uri);
		} else {
			namespaceStack.push(prefix, uri);
		}
	}

	private static void checkPrefix(final String prefix) {
		Assert.isNotNull(prefix, "null is not a valid namespace prefix.");
	}

	private static boolean isDefaultPrefix(final String prefix) {
		return "".equals(prefix);
	}

	// ============================================== LexicalHandler methods

	@Override
	public void comment(final char[] ch, final int start, final int length) {
		if (inDTD) {
			return;
		}

		if (isBeforeRoot()) {
			final Comment comment = new Comment();
			final int startOffset = content.length();
			content.insertTagMarker(content.length());

			trimLeading = true;
			appendPendingCharsFiltered(ch, start, length);
			appendCharsNoValidation(true); // Trailing whitespace of a comment must not be removed (could end with '-- ')

			content.insertTagMarker(content.length());
			comment.associate(content, new ContentRange(startOffset, content.length() - 1));
			if (isBlock(comment)) {
				trimLeading = true;
			}

			nodesBeforeRoot.add(comment);
		} else if (isAfterRoot()) {
			final Comment comment = new Comment();
			final int startOffset = content.length();
			content.insertTagMarker(content.length());

			trimLeading = true;
			appendPendingCharsFiltered(ch, start, length);
			appendCharsNoValidation(true); // Trailing whitespace of a comment must not be removed (could end with '-- ')

			content.insertTagMarker(content.length());
			comment.associate(content, new ContentRange(startOffset, content.length() - 1));
			if (isBlock(comment)) {
				trimLeading = true;
			}

			nodesAfterRoot.add(comment);
		} else {
			final Comment comment = new Comment();
			final Element parent = stack.getLast().element;
			parent.addChild(comment);

			appendChars(isBlock(comment));
			final int startOffset = content.length();
			content.insertTagMarker(content.length());

			trimLeading = true;
			appendPendingCharsFiltered(ch, start, length);
			appendCharsNoValidation(true); // Trailing whitespace of a comment must not be removed (could end with '-- ')

			content.insertTagMarker(content.length());
			comment.associate(content, new ContentRange(startOffset, content.length() - 1));
			if (isBlock(comment)) {
				trimLeading = true;
			}
		}
	}

	private boolean isBeforeRoot() {
		return stack.isEmpty() && rootElement == null;
	}

	private boolean isAfterRoot() {
		return stack.isEmpty() && rootElement != null;
	}

	@Override
	public void endCDATA() {
	}

	@Override
	public void endDTD() {
		inDTD = false;
	}

	@Override
	public void endEntity(final String name) {
	}

	@Override
	public void startCDATA() {
	}

	@Override
	public void startDTD(final String name, final String publicId, final String systemId) {
		dtdPublicID = publicId;
		dtdSystemID = systemId;
		inDTD = true;
	}

	@Override
	public void startEntity(final String name) {
	}

	// ======================================================== PRIVATE

	// Append any pending characters to the content
	private void appendChars(final boolean trimTrailing) {

		StringBuilder sb;

		sb = cleanUpTextContent(trimTrailing);

		if (!stack.isEmpty()) {
			final Element parent = stack.getLast().element;
			if (canInsertText(parent, 0) || sb.toString().trim().length() > 0) {
				// Whitespace only is ignored if element does not allow text
				content.insertText(content.length(), sb.toString());
			}
		}

		pendingChars.setLength(0);
		trimLeading = false;
	}

	// Append any pending characters without validation
	private void appendCharsNoValidation(final boolean trimTrailing) {
		StringBuilder sb;

		sb = cleanUpTextContent(trimTrailing);

		content.insertText(content.length(), sb.toString());

		pendingChars.setLength(0);
		trimLeading = false;
	}

	private StringBuilder cleanUpTextContent(final boolean trimTrailing) {
		StringBuilder sb;
		final StackEntry entry = stack.isEmpty() ? null : stack.getLast();

		if (entry != null && entry.pre) {
			sb = pendingChars;
			XML.normalizeNewlines(sb);
		} else {
			sb = new StringBuilder(pendingChars);
			XML.normalizeNewlines(sb);
			sb = XML.compressWhitespace(sb, trimLeading, trimTrailing, false);
		}

		return sb;
	}

	private boolean isBlock(final Node node) {
		return whitespacePolicy != null && whitespacePolicy.isBlock(node);
	}

	private boolean isPre(final Node node) {
		return whitespacePolicy != null && whitespacePolicy.isPre(node);
	}

	private boolean isInclude(final Element element) {
		return element.getQualifiedName().equals(new QualifiedName(Namespace.XINCLUDE_NAMESPACE_URI, "include"));
	}

	private boolean canInsertText(final INode insertionNode, final int offset) {

		final List<QualifiedName> textNode = Arrays.asList(IValidator.PCDATA);

		if (insertionNode == null) {
			return false;
		}
		return insertionNode.accept(new BaseNodeVisitorWithResult<Boolean>(false) {
			@Override
			public Boolean visit(final IElement element) {
				return validator.isValidSequence(element.getQualifiedName(), textNode, null, null, true);
			}

			@Override
			public Boolean visit(final IComment comment) {
				return true;
			}

			@Override
			public Boolean visit(final IText text) {
				return true;
			}
		});
	}

	private static class StackEntry {
		public Element element;
		public int offset;
		public boolean pre;

		public StackEntry(final Element element, final int offset, final boolean pre) {
			this.element = element;
			this.offset = offset;
			this.pre = pre;
		}
	}

	/**
	 * Set the stored caret position. While parsing the document, the node at this position will be stored and returned
	 * with {@link #getNodeAtCaret}.
	 *
	 * @param position
	 */
	public void setCaretPosition(final DocumentTextPosition position) {
		caretPosition = position;
	}

	/**
	 *
	 * @return The node at the given caret position.
	 *
	 * @see #setCaretPosition(DocumentTextPosition)
	 */
	public INode getNodeAtCaret() {
		return nodeAtCaret;
	}

}
