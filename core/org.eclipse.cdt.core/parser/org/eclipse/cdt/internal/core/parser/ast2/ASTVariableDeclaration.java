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

import org.eclipse.cdt.core.parser.ast2.IASTVariable;
import org.eclipse.cdt.core.parser.ast2.IASTVariableDeclaration;

/**
 * @author Doug Schaefer
 */
public class ASTVariableDeclaration extends ASTDeclaration implements IASTVariableDeclaration{

	private IASTVariable variable;
	
	public IASTVariable getVariable() {
		return variable;
	}

	public void setVariable(IASTVariable variable) {
		this.variable = variable;
	}
	
}
