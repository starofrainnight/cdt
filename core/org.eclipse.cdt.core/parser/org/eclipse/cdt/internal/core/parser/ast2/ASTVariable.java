/**********************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.cdt.internal.core.parser.ast2;

import org.eclipse.cdt.core.parser.ast2.IASTExpression;
import org.eclipse.cdt.core.parser.ast2.IASTType;
import org.eclipse.cdt.core.parser.ast2.IASTVariable;

/**
 * @author Doug Schaefer
 */
public class ASTVariable implements IASTVariable {

	private IASTType type;
	
	public IASTType getType() {
		return type;
	}

	public void setType(IASTType type) {
		this.type = type;
	}
	
	public IASTExpression getInitialization() {
		return null;
	}

}
