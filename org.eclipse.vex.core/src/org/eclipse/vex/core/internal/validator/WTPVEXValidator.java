/*******************************************************************************
 * Copyright (c) 2008, 2011 Standard for Technology in Automotive Retail  and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *Contributors:
 *    David Carver (STAR) - initial API and implementation
 *    Holger Voormann - bug 283646 - Document wizard throws NPW with DITA is selected
 *	  Igor Jacy Lino Campista - Java 5 warnings fixed (bug 311325)
 *    Florian Thienel - bug 299999 - completed implementation of validation
 *******************************************************************************/
package org.eclipse.vex.core.internal.validator;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.vex.core.internal.dom.Attribute;
import org.eclipse.vex.core.internal.dom.DocumentContentModel;
import org.eclipse.vex.core.internal.dom.Element;
import org.eclipse.vex.core.internal.dom.Validator;
import org.eclipse.vex.core.internal.validator.AttributeDefinition.Type;
import org.eclipse.wst.xml.core.internal.contentmodel.CMAnyElement;
import org.eclipse.wst.xml.core.internal.contentmodel.CMAttributeDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMContent;
import org.eclipse.wst.xml.core.internal.contentmodel.CMDataType;
import org.eclipse.wst.xml.core.internal.contentmodel.CMDocument;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMGroup;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNode;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNodeList;
import org.eclipse.wst.xml.core.internal.contentmodel.ContentModelManager;
import org.eclipse.wst.xml.core.internal.contentmodel.internal.util.CMValidator;
import org.eclipse.wst.xml.core.internal.contentmodel.internal.util.CMValidator.ElementContentComparator;
import org.eclipse.wst.xml.core.internal.contentmodel.internal.util.CMValidator.ElementPathRecordingResult;
import org.eclipse.wst.xml.core.internal.contentmodel.internal.util.CMValidator.StringElementContentComparator;

public class WTPVEXValidator implements Validator {

	private static final ElementContentComparator ELEMENT_CONTENT_COMPARATOR = new StringElementContentComparator() {
		@Override
		public boolean isPCData(final Object o) {
			return "#PCDATA".equals(o);
		};
	};

	private final DocumentContentModel documentContentModel;

	private CMDocument dtd;

	private final CMValidator validator = new CMValidator();

	public WTPVEXValidator() {
		this(new DocumentContentModel());
	}

	public WTPVEXValidator(final String schemaIdentifier) {
		this(new DocumentContentModel(null, schemaIdentifier, null, null));
	}

	public WTPVEXValidator(final DocumentContentModel documentContentModel) {
		this.documentContentModel = documentContentModel;
	}

	private CMDocument getSchema(final String namespaceURI) {
		if (isDTDDefined())
			return getDTD();
		if (namespaceURI == null)
			/*
			 * TODO this is a common case that should be handled somehow
			 * - a hint should be shown: there is no DTD or Schema referenced in
			 * the document
			 * - an inferred schema should be used, to allow to at least display
			 * the document in the editor
			 * - this is not the right place to either check or handle this
			 */
			throw new AssertionError("There is no definition of the document structure available.");
		final URL resolved = documentContentModel.resolveSchemaIdentifier(namespaceURI);
		final ContentModelManager modelManager = ContentModelManager.getInstance();
		return modelManager.createCMDocument(resolved.toString(), null);
	}

	private boolean isDTDDefined() {
		return documentContentModel.isDtdAssigned();
	}

	private CMDocument getDTD() {
		if (dtd == null && documentContentModel.isDtdAssigned()) {
			final URL dtdUrl = documentContentModel.resolveSchemaIdentifier(documentContentModel.getMainDocumentTypeIdentifier());
			if (dtdUrl == null)
				return null;
			final ContentModelManager modelManager = ContentModelManager.getInstance();
			final String resolved = dtdUrl.toString();
			dtd = modelManager.createCMDocument(resolved, null);
		}
		return dtd;
	}

	public AttributeDefinition getAttributeDefinition(final Attribute attribute) {
		final String attributeName = attribute.getLocalName();
		final CMElementDeclaration cmElement = getElementDeclaration(attribute.getParent());
		/*
		 * #342320: If we do not find the element, it is acutally not valid.
		 * But we are benevolent here since we do not want to loose data at this
		 * point.
		 */
		if (cmElement == null)
			return createUnknownAttributeDefinition(attributeName);

		final CMAttributeDeclaration cmAttribute = (CMAttributeDeclaration) cmElement.getAttributes().getNamedItem(attributeName);
		if (cmAttribute != null)
			return createAttributeDefinition(cmAttribute);
		/*
		 * #318834 If we do not find the attribute, it is actually not valid.
		 * But we are benevolent here since we do not want to loose data at this
		 * point.
		 */
		return createUnknownAttributeDefinition(attributeName);
	}

	private static AttributeDefinition createUnknownAttributeDefinition(final String attributeName) {
		return new AttributeDefinition(attributeName, Type.CDATA, /* default value */"", /* values */new String[0], /* required */false, /* fixed */true);
	}

	public List<AttributeDefinition> getAttributeDefinitions(final Element element) {
		final CMElementDeclaration cmElement = getElementDeclaration(element);
		/*
		 * #342320: If we do not find the element, it is acutally not valid.
		 * But we are benevolent here since we do not want to loose data at this
		 * point.
		 */
		if (cmElement == null)
			return Collections.emptyList();
		final List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>(cmElement.getAttributes().getLength());
		final Iterator<?> iter = cmElement.getAttributes().iterator();
		while (iter.hasNext()) {
			final CMAttributeDeclaration attribute = (CMAttributeDeclaration) iter.next();
			final AttributeDefinition vexAttr = createAttributeDefinition(attribute);
			attributeList.add(vexAttr);
		}

		return attributeList;
	}

	private CMElementDeclaration getElementDeclaration(final Element element) {
		if (element == null)
			return null;
		final String localName = element.getLocalName();
		final CMElementDeclaration declarationFromRoot = (CMElementDeclaration) getSchema(element.getQualifiedName().getQualifier()).getElements()
				.getNamedItem(localName);
		if (declarationFromRoot != null)
			return declarationFromRoot;
		final CMElementDeclaration parentDeclaration = getElementDeclaration(element.getParent());
		if (parentDeclaration == null)
			return null;
		return (CMElementDeclaration) parentDeclaration.getLocalElements().getNamedItem(localName);
	}

	private AttributeDefinition createAttributeDefinition(final CMAttributeDeclaration attribute) {
		@SuppressWarnings("deprecation")
		final String defaultValue = attribute.getDefaultValue();
		String[] values = null;
		AttributeDefinition.Type type = null;
		if (attribute.getAttrType().equals(CMDataType.ENUM)) {
			type = AttributeDefinition.Type.ENUMERATION;
			values = attribute.getAttrType().getEnumeratedValues();
		} else if (attribute.getAttrType().equals(CMDataType.NOTATION)) {
			type = AttributeDefinition.Type.ENUMERATION;
			values = attribute.getAttrType().getEnumeratedValues();
		} else
			type = AttributeDefinition.Type.get(attribute.getAttrType().getDataTypeName());
		final boolean required = attribute.getUsage() == CMAttributeDeclaration.REQUIRED;
		final boolean fixed = attribute.getUsage() == CMAttributeDeclaration.FIXED;
		final AttributeDefinition vexAttr = new AttributeDefinition(attribute.getAttrName(), type, defaultValue, values, required, fixed);
		return vexAttr;
	}

	public Set<QualifiedName> getValidItems(final Element element) {
		return getValidItems(getElementDeclaration(element));
	}

	private Set<QualifiedName> getValidItems(final CMElementDeclaration elementDeclaration) {
		/*
		 * #342320: If we do not find the element, it is acutally not valid.
		 * But we are benevolent here since we do not want to loose data at this
		 * point.
		 */
		if (elementDeclaration == null)
			return Collections.emptySet();
		final Set<QualifiedName> result = new HashSet<QualifiedName>();
		for (final CMNode node : getAvailableContent(elementDeclaration))
			if (node instanceof CMElementDeclaration) {
				final CMElementDeclaration childDeclaration = (CMElementDeclaration) node;
				result.add(createQualifiedElementName(childDeclaration));
			}
		return result;
	}

	private static QualifiedName createQualifiedElementName(final CMElementDeclaration elementDeclaration) {
		final CMDocument cmDocument = (CMDocument) elementDeclaration.getProperty("CMDocument");
		if (cmDocument == null)
			return new QualifiedName(null, elementDeclaration.getElementName());
		final String namespaceUri = (String) cmDocument.getProperty("http://org.eclipse.wst/cm/properties/targetNamespaceURI");
		return new QualifiedName(namespaceUri, elementDeclaration.getElementName());
	}

	/**
	 * Returns a list of all CMNode 'meta data' that may be potentially added to
	 * the element.
	 */
	private List<CMNode> getAvailableContent(final CMElementDeclaration elementDeclaration) {
		final List<CMNode> list = new ArrayList<CMNode>();
		if (elementDeclaration.getContentType() == CMElementDeclaration.ELEMENT || elementDeclaration.getContentType() == CMElementDeclaration.MIXED) {
			final CMContent content = elementDeclaration.getContent();
			if (content instanceof CMElementDeclaration)
				list.add(content);
			else if (content instanceof CMGroup) {
				final CMGroup groupContent = (CMGroup) content;
				list.addAll(getAllChildren(groupContent));
			}
		}
		return list;
	}

	private List<CMNode> getAllChildren(final CMGroup group) {
		final List<CMNode> list = new ArrayList<CMNode>();
		final CMNodeList nodeList = group.getChildNodes();

		for (int i = 0; i < nodeList.getLength(); i++) {
			final CMNode node = nodeList.item(i);
			if (node instanceof CMElementDeclaration)
				list.add(node);
			else if (node instanceof CMGroup)
				list.addAll(getAllChildren((CMGroup) node));
			else if (node instanceof CMAnyElement)
				list.addAll(getValidRootElements(((CMAnyElement) node).getNamespaceURI()));
		}
		return list;
	}

	private Set<CMElementDeclaration> getValidRootElements(final String namespaceURI) {
		final HashSet<CMElementDeclaration> result = new HashSet<CMElementDeclaration>();
		final Iterator<?> iter = getSchema(namespaceURI).getElements().iterator();
		while (iter.hasNext()) {
			final CMElementDeclaration element = (CMElementDeclaration) iter.next();
			result.add(element);
		}

		return result;
	}

	public Set<QualifiedName> getValidRootElements() {
		final HashSet<QualifiedName> result = new HashSet<QualifiedName>();
		for (final CMElementDeclaration element : getValidRootElements(null))
			result.add(createQualifiedElementName(element));
		return result;
	}

	public boolean isValidSequence(final QualifiedName element, final List<QualifiedName> nodes, final boolean partial) {
		final CMNode parent = getSchema(element.getQualifier()).getElements().getNamedItem(element.getLocalName());
		if (!(parent instanceof CMElementDeclaration))
			return true;

		final CMElementDeclaration elementDeclaration = (CMElementDeclaration) parent;
		final ElementPathRecordingResult validationResult = new ElementPathRecordingResult();
		final List<String> nodeNames = new ArrayList<String>();
		for (final QualifiedName node : nodes)
			nodeNames.add(node.getLocalName()); // TODO learn how the WTP content model handles namespaces
		validator.validate(elementDeclaration, nodeNames, ELEMENT_CONTENT_COMPARATOR, validationResult);

		final int elementCount = getElementCount(nodes);
		if (partial && elementCount > 0)
			return validationResult.getPartialValidationCount() >= elementCount;

		return validationResult.isValid;
	}

	private static int getElementCount(final List<QualifiedName> nodes) {
		int count = 0;
		for (final QualifiedName node : nodes)
			if (ELEMENT_CONTENT_COMPARATOR.isElement(node.getLocalName()))
				count++;
		return count;
	}

	public boolean isValidSequence(final QualifiedName element, final List<QualifiedName> seq1, final List<QualifiedName> seq2, final List<QualifiedName> seq3,
			final boolean partial) {
		final List<QualifiedName> joinedSequence = new ArrayList<QualifiedName>();
		if (seq1 != null)
			joinedSequence.addAll(seq1);
		if (seq2 != null)
			joinedSequence.addAll(seq2);
		if (seq3 != null)
			joinedSequence.addAll(seq3);
		return isValidSequence(element, joinedSequence, partial);
	}
}
