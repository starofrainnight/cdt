/*******************************************************************************
 * Copyright (c) 2014 Institute for Software, HSR Hochschule fuer Technik
 * Rapperswil, University of applied sciences.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Thomas Corbat (IFS) - Initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.internal.core.dom.parser.cpp;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTAttribute;
import org.eclipse.cdt.core.dom.ast.IASTAttributeSpecifier;
import org.eclipse.cdt.core.parser.util.ArrayUtil;
import org.eclipse.cdt.internal.core.dom.parser.ASTNode;

/**
 * Represents an attribute specifier, containing attributes.
 */
public abstract class ASTAttributeSpecifier extends ASTNode implements IASTAttributeSpecifier {
	protected IASTAttribute[] attributes = IASTAttribute.EMPTY_ATTRIBUTE_ARRAY;

	public ASTAttributeSpecifier() {
		super();
	}

	@Override
	public void addAttribute(IASTAttribute attribute) {
		assertNotFrozen();
		if (attribute != null) {
			attribute.setParent(this);
			attribute.setPropertyInParent(IASTAttributeSpecifier.ATTRIBUTE);
			attributes = ArrayUtil.append(attributes, attribute);
		}
	}

	@Override
	public IASTAttribute[] getAttributes() {
		attributes = ArrayUtil.trim(attributes);
		return attributes;
	}

	@Override
	public boolean accept(ASTVisitor action) {
		if (action.shouldVisitAttributes) {
			switch (action.visit(this)) {
			case ASTVisitor.PROCESS_ABORT:
				return false;
			case ASTVisitor.PROCESS_SKIP:
				return true;
			}
		}

		for (IASTAttribute attribute : getAttributes()) {
			if (!attribute.accept(action))
				return false;
		}

		if (action.shouldVisitAttributes && action.leave(this) == ASTVisitor.PROCESS_ABORT)
			return false;

		return true;
	}

	protected <T extends ASTAttributeSpecifier> T copy(T copy, CopyStyle style) {
		copy.attributes = ArrayUtil.trim(attributes, true);
		for (int i = 0; i < copy.attributes.length; i++) {
			IASTAttribute attributeCopy = copy.attributes[i].copy(style);
			attributeCopy.setParent(this);
			copy.attributes[i] = attributeCopy;
		}
		return super.copy(copy, style);
	}
}