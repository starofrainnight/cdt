/*******************************************************************************
 * Copyright (c) 2007, 2008 Intel Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Intel Corporation - initial API and implementation
 *     IBM Corporation
 *******************************************************************************/
package org.eclipse.cdt.ui.newui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.widgets.TableColumn;

import org.eclipse.cdt.core.model.util.CDTListComparator;
import org.eclipse.cdt.core.settings.model.CMacroEntry;
import org.eclipse.cdt.core.settings.model.ICLanguageSettingEntry;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;

public class SymbolTab extends AbstractLangsListTab {
    public void additionalTableSet() {
    	TableColumn tc = new TableColumn(table, SWT.LEFT);
    	tc.setText(UIMessages.getString("SymbolTab.0")); //$NON-NLS-1$
    	tc.setWidth(80);
    	tc = new TableColumn(table, SWT.LEFT);
    	tc.setText(UIMessages.getString("SymbolTab.1")); //$NON-NLS-1$
    	tc.setWidth(130);
    	table.getAccessible().addAccessibleListener(
				new AccessibleAdapter() {                       
                    public void getName(AccessibleEvent e) {
                            e.result = UIMessages.getString("SymbolTab.0"); //$NON-NLS-1$
                    }
                }
		  );
    }

	public ICLanguageSettingEntry doAdd() {
		SymbolDialog dlg = new SymbolDialog(
				usercomp.getShell(), true,
				UIMessages.getString("SymbolTab.2"), EMPTY_STR, EMPTY_STR, getResDesc()); //$NON-NLS-1$
		if (dlg.open() && dlg.text1.trim().length() > 0 ) {
			toAllCfgs = dlg.check1;
			toAllLang = dlg.check3;
			return new CMacroEntry(dlg.text1, dlg.text2, 0);
		} else 
			return null;
	}

	public ICLanguageSettingEntry doEdit(ICLanguageSettingEntry ent) {
		SymbolDialog dlg = new SymbolDialog(
				usercomp.getShell(), false,
				UIMessages.getString("SymbolTab.3"), ent.getName(),  //$NON-NLS-1$
				ent.getValue(), getResDesc());
		if (dlg.open())
			return new CMacroEntry(dlg.text1, dlg.text2, 0);
		else 
			return null;
	}
	
	public int getKind() { return ICSettingEntry.MACRO; }

	// Specific version of "update()" for Symbols tab only
	public void update() {
		if (lang != null) {
			int x = table.getSelectionIndex();
			if (x == -1) x = 0;
			
//			incs = new LinkedList<ICLanguageSettingEntry>(lang.getSettingEntriesList(getKind()));
			incs = getIncs();
			ArrayList<ICLanguageSettingEntry> lst = new ArrayList<ICLanguageSettingEntry>();
			if (incs != null) {
				Iterator it = incs.iterator();
				while (it.hasNext()) {
					ICLanguageSettingEntry ent = (ICLanguageSettingEntry)it.next();
					if (!(ent.isBuiltIn() && (!showBIButton.getSelection()))) lst.add(ent);
				}
				Collections.sort(lst, CDTListComparator.getInstance());
			}
			tv.setInput(lst.toArray(new Object[lst.size()]));
			if (table.getItemCount() > x) table.select(x);
			else if (table.getItemCount() > 0) table.select(0);
		}		
		updateLbs(lb1, lb2);
		updateButtons();
	}
	
}
