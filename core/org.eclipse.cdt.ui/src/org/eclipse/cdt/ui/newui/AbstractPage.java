/*******************************************************************************
 * Copyright (c) 2007, 2008 Intel Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Intel Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.ui.newui;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferencePageContainer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.actions.WorkspaceModifyDelegatingOperation;
import org.eclipse.ui.dialogs.PropertyPage;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.util.CDTListComparator;
import org.eclipse.cdt.core.settings.model.CConfigurationStatus;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICFolderDescription;
import org.eclipse.cdt.core.settings.model.ICMultiItemsHolder;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.cdt.core.settings.model.MultiItemsHolder;
import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.cdt.ui.PreferenceConstants;
import org.eclipse.cdt.utils.ui.controls.ControlFactory;

import org.eclipse.cdt.internal.ui.CPluginImages;

/**
 * It is a parent for all standard CDT property pages
 * in new CDT model. 
 * 
 * Although it is enough for new page to implement
 * "IWorkbenchPropertyPage" interface, it would be
 * better to extend it from "AbstractPage".
 * 
 * In this case, we'll able to use:
 * - dynamic tabs support via cPropertyTab extension point
 * - a lot of utility methods: see ICPropertyProvider interface
 * - mechanism of messages sent to all pages and all tabs in them
 * 
 * In fact, descendants of AbstractPage have to implement
 * the only method:
 * 		protected boolean isSingle(); 
 * It it returns false, current page can contain multiple tabs
 * (obtained through "cPropertyTab" extension point).
 * If it returns true, only one content tab is possible. If
 * more than 1 tabs refer to this pas as a parent, only 1st
 * one would be taken into account, others will be ignored. 
 */
public abstract class AbstractPage extends PropertyPage 
implements
		IWorkbenchPropertyPage, // ext point 
		IPreferencePageContainer, // dynamic pages
		ICPropertyProvider // utility methods for tabs
{
	private static ICResourceDescription resd = null;
	private static ICConfigurationDescription[] cfgDescs = null;
	private static int cfgIndex = -1;
	private static ICConfigurationDescription[] multiCfgs = null; // selected multi cfg
	// tabs
	private static final String EXTENSION_POINT_ID = "org.eclipse.cdt.ui.cPropertyTab"; //$NON-NLS-1$
	public static final String ELEMENT_NAME = "tab"; //$NON-NLS-1$
	public static final String CLASS_NAME = "class"; //$NON-NLS-1$
	public static final String PARENT_NAME = "parent"; //$NON-NLS-1$
	public static final String IMAGE_NAME = "icon"; //$NON-NLS-1$
	public static final String TIP_NAME = "tooltip"; //$NON-NLS-1$
	public static final String TEXT_NAME = "name"; //$NON-NLS-1$
	public static final String WEIGHT_NAME = "weight"; //$NON-NLS-1$

	private static final Object NOT_NULL = new Object();
	public static final String EMPTY_STR = "";  //$NON-NLS-1$
	
	private static final int SAVE_MODE_OK = 1;
	private static final int SAVE_MODE_APPLY = 2;
	private static final int SAVE_MODE_APPLYOK = 3;
	
	private final Image IMG_WARN = CPluginImages.get(CPluginImages.IMG_OBJS_REFACTORING_WARNING);
	/*
	 * Dialog widgets
	 */
	private Combo configSelector;
	private Button manageButton;
	private Button excludeFromBuildCheck;
	private Label errIcon;
	private Label errMessage;
	private Composite errPane;
	private Composite parentComposite;
	/*
	 * Bookeeping variables
	 */
	protected boolean noContentOnPage = false;
	protected boolean displayedConfig = false;
	protected IResource internalElement = null;
	protected boolean isProject = false;
	protected boolean isFolder  = false;
	protected boolean isFile    = false;
	
	// tabs
	protected TabFolder folder;
	protected ArrayList itabs = new ArrayList();
	protected ICPropertyTab currentTab;

	private static boolean isNewOpening = true;
	
	protected class InternalTab {
		Composite comp;
		String text;
		String tip;
		Image image;
		ICPropertyTab tab;
		
		InternalTab(Composite _comp, String _text, Image _image, ICPropertyTab _tab, String _tip) {
			comp  = _comp;
			text  = _text;
			image = _image;
			tab   = _tab;
			tip   = _tip;
		}

		public TabItem createOn(TabFolder f) {
			if (tab.canBeVisible()) {
				TabItem ti = new TabItem(f, SWT.NONE);
				ti.setText(text);
				if (tip != null) ti.setToolTipText(tip);
				if (image != null) ti.setImage(image);
				ti.setControl(comp);
				ti.setData(tab);
				return ti;
			}
			return null;
		}
	}
	
	/**
	 * Default constructor
	 */
	public AbstractPage() {
		if (CDTPropertyManager.getPagesCount() == 0) {
			cfgDescs = null;
			cfgIndex = -1;
		}
	}
	
	protected Control createContents(Composite parent) {
		//	Create the container we return to the property page editor
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setFont(parent.getFont());
		GridLayout compositeLayout = new GridLayout();
		compositeLayout.numColumns = 1;
		compositeLayout.marginHeight = 0;
		compositeLayout.marginWidth = 0;
		composite.setLayout( compositeLayout );

		String s = null;
		if (!checkElement()) {
			s = UIMessages.getString("AbstractPage.0"); //$NON-NLS-1$
		} else if (!isApplicable()) {
			return null;
		} else if (!isCDTProject(getProject())) {
			s = UIMessages.getString("AbstractPage.2"); //$NON-NLS-1$
		}
		
	    if (s == null) {
	    	contentForCDT(composite);
	    	return composite;
	    }
		
		// no contents
		Label label = new Label(composite, SWT.LEFT);
		label.setText(s);
		label.setFont(composite.getFont());
		noContentOnPage = true;
		noDefaultAndApplyButton();
		return composite;
	}
	
	protected void contentForCDT(Composite composite) {
		GridData gd;

		// Add a config selection area
		Group configGroup = ControlFactory.createGroup(composite, EMPTY_STR, 1);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.grabExcessHorizontalSpace = true;
		configGroup.setLayoutData(gd);
		configGroup.setLayout(new GridLayout(3, false));
		
		Label configLabel = new Label(configGroup, SWT.NONE);
		configLabel.setText(UIMessages.getString("AbstractPage.6")); //$NON-NLS-1$
		configLabel.setLayoutData(new GridData(GridData.BEGINNING));
		
		configSelector = new Combo(configGroup, SWT.READ_ONLY | SWT.DROP_DOWN);
		configSelector.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				handleConfigSelection();
			}
		});
		gd = new GridData(GridData.FILL);
		gd.grabExcessHorizontalSpace = true;
	    gd.grabExcessVerticalSpace = true;
	    gd.horizontalAlignment = GridData.FILL;
	    gd.verticalAlignment = GridData.FILL;
	  	
		configSelector.setLayoutData(gd);
		
		if (!CDTPrefUtil.getBool(CDTPrefUtil.KEY_NOMNG)) {
			manageButton = new Button(configGroup, SWT.PUSH);
			manageButton.setText(UIMessages.getString("AbstractPage.12")); //$NON-NLS-1$
			gd = new GridData(GridData.END);
			gd.minimumWidth = 150;
			manageButton.setLayoutData(gd);
			manageButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					IProject[] obs = new IProject[] { getProject() };
					IConfigManager cm = ManageConfigSelector.getManager(obs);
					if (cm != null && cm.manage(obs, false)) {
						cfgDescs = null;
						populateConfigurations();					
					}
				}
			});
		} else { // dummy object to avoid breaking layout
			new Label(configGroup, SWT.NONE).setLayoutData(new GridData(GridData.END));
		}

		errPane = new Composite(configGroup, SWT.NONE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 3;
		errPane.setLayoutData(gd);
		GridLayout gl = new GridLayout(2, false);
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		gl.verticalSpacing = 0;
		gl.horizontalSpacing = 0;
		errPane.setLayout(gl);
		
		errIcon = new Label(errPane, SWT.LEFT);
		errIcon.setLayoutData(new GridData(GridData.BEGINNING));
		errIcon.setImage(IMG_WARN);
		
		errMessage = new Label(errPane, SWT.LEFT);
		errMessage.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		if (isForFolder() || isForFile()) {
			excludeFromBuildCheck = new Button(configGroup, SWT.CHECK);
			excludeFromBuildCheck.setText(UIMessages.getString("AbstractPage.7")); //$NON-NLS-1$
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan = 3;
			excludeFromBuildCheck.setLayoutData(gd);
			excludeFromBuildCheck.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					getResDesc().setExcluded(excludeFromBuildCheck.getSelection());
				}
			});
		}
		
		//	Update the contents of the configuration widget
		populateConfigurations();
		
		if (excludeFromBuildCheck != null) {
			excludeFromBuildCheck.setSelection(getResDesc().isExcluded());
		}
		//	Create the Specific objects for each page
		createWidgets(composite);
	}
	
	public void createWidgets(Composite c) {
		parentComposite = new Composite(c, SWT.NONE);
		parentComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		itabs.clear(); 
		if (!isSingle()) {
			parentComposite.setLayout(new FillLayout());
			folder = new TabFolder(parentComposite, SWT.NONE);
//			folder.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY));
		}
		loadExtensionsSynchronized(parentComposite);
		
		// Set listener after data load, to avoid firing
		// selection event on not-initialized tab items 
		if (folder != null) {
		    folder.addSelectionListener(new SelectionAdapter() {
			      public void widgetSelected(org.eclipse.swt.events.SelectionEvent event) {
			    	  if (folder.getSelection().length > 0 ) {
			    		  ICPropertyTab newTab = (ICPropertyTab)folder.getSelection()[0].getData();
			    		  if (newTab != null && currentTab != newTab) {
				    		  if (currentTab != null) currentTab.handleTabEvent(ICPropertyTab.VISIBLE, null);			    			  
				    		  currentTab = newTab;
				    		  currentTab.handleTabEvent(ICPropertyTab.VISIBLE, NOT_NULL);
			    		  }
			    	  }
			      }
			    });
		    if (folder.getItemCount() > 0) folder.setSelection(0);
		}
	}
	/**
	 * 
	 */
	public IProject getProject() {
		Object element = getElement();
		if (element != null) { 
			if (element instanceof IFile ||
				element instanceof IProject ||
				element instanceof IFolder)
				{
			IResource f = (IResource) element;
			return f.getProject();
				}
			else if (element instanceof ICProject)
				return ((ICProject)element).getProject();
		}
		return null;
	}

	/*
	 * Event Handlers
	 */
	private void handleConfigSelection() {
		// If there is nothing in config selection widget just bail
		if (configSelector.getItemCount() == 0) return;
		int selectionIndex = configSelector.getSelectionIndex();
		if (selectionIndex == -1) return;

		// Check if the user has selected the "all / multiple" configuration
		if (selectionIndex >= cfgDescs.length) {
			if ((selectionIndex - cfgDescs.length) == 0) {  // all
				multiCfgs = cfgDescs;
				cfgIndex = selectionIndex; 
			} else { // multiple
				if (cfgIndex != selectionIndex) { // to avoid re-request on page change 
					ICConfigurationDescription[] mcfgs = ConfigMultiSelectionDialog.select(cfgDescs);
					if (mcfgs == null || mcfgs.length == 0) {
						// return back to previous selection
						if (cfgIndex > configSelector.getItemCount()) {
							cfgIndex = 0;
							configSelector.select(0);
							cfgChanged(cfgDescs[0]);
						} else {
							configSelector.select(cfgIndex);
						}
						return;
					}
					multiCfgs = mcfgs;
					cfgIndex = selectionIndex;
				}
			}
			// TODO: avoid repeated update like for single cfg 
			cfgChanged(MultiItemsHolder.createCDescription(multiCfgs, 
					ICMultiItemsHolder.MODE_DEFAULT)); 
			return;
		} else {
			String id1 = getResDesc() == null ? null : getResDesc().getId();
			cfgIndex = selectionIndex;
			ICConfigurationDescription newConfig = cfgDescs[selectionIndex];
			String id2 = newConfig.getId();
			if (id2 != null && !id2.equals(id1))
				cfgChanged(newConfig);
		}
	}
	
    public boolean performCancel() {
		if (! noContentOnPage && displayedConfig) forEach(ICPropertyTab.CANCEL);
		
		CDTPropertyManager.performCancel(this);
		
        return true;
    }
	public void performDefaults() {
		if (! noContentOnPage && displayedConfig) forEach(ICPropertyTab.DEFAULTS);
	}
    public void performApply() { performSave(SAVE_MODE_APPLY); }
    
    /**
     * There are 2 ways to perform OK for CDT property pages.
     * 1st (default): 
     *   All pages use the same editable copy of ICProjectDescription.
     *   When OK occurs, this object is simply set.
     *   
     * 2nd:  
     *   When OK occurs, each page must copy its data to new instance
     *   of ICProjectDescription, like it occurs during Apply event.
     *   It allows to avoid collisions with other property pages, 
     *   which do not share ICProjectDescription instance.
     *   But some changes may be saved wrong if they are affected
     *   by data from another property pages (Discovery options etc).

     *   To enable 2nd mode, just create the following file:
     *   <workspace>/.metadata/.plugins/org.eclipse.cdt.ui/apply_mode
     */
    
    public boolean performOk() {
    	File f = CUIPlugin.getDefault().getStateLocation().append("apply_mode").toFile(); //$NON-NLS-1$
    	if (f.exists()) 
        	return performSave(SAVE_MODE_APPLYOK); 
        else 
        	return performSave(SAVE_MODE_OK); 
        	
    }
    /**
     * The same code used to perform OK and Apply 
     */
    private boolean performSave(int mode)	{
    	final int finalMode = mode;
		if (noContentOnPage || !displayedConfig) return true;
		if ((mode == SAVE_MODE_OK || mode == SAVE_MODE_APPLYOK) && CDTPropertyManager.isSaveDone()) return true; // do not duplicate
		
		final boolean needs = (mode != SAVE_MODE_OK);
		final ICProjectDescription local_prjd = needs ? CoreModel.getDefault().getProjectDescription(getProject()) : null;
		
		ICResourceDescription lc = null;
		
		if (needs) { 
			if (isMultiCfg()) {
				lc = resd;
			} else {
				ICConfigurationDescription c = needs ? local_prjd.getConfigurationById(resd.getConfiguration().getId()) : null;
				lc = getResDesc(c);
			}
		}
		final ICResourceDescription local_cfgd = lc;
		
		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			
			private void sendOK() {
				for (int j=0; j<CDTPropertyManager.getPagesCount(); j++) {
					Object p = CDTPropertyManager.getPage(j);
					if (p != null && p instanceof AbstractPage) { 
						AbstractPage ap = (AbstractPage)p;
						if (ap.displayedConfig) ap.forEach(ICPropertyTab.OK, null);
					}
				}
			}
			
			public void run(IProgressMonitor monitor) {
				// ask all tabs to store changes in cfg
				switch (finalMode) {
				case SAVE_MODE_APPLYOK:
					sendOK();
					ICConfigurationDescription[] olds = CDTPropertyManager.getProjectDescription(AbstractPage.this, getProject()).getConfigurations();
					for (int i=0; i<olds.length; i++) {
						resd = getResDesc(olds[i]);
						ICResourceDescription r = getResDesc(local_prjd.getConfigurationById(olds[i].getId()));
						for (int j=0; j<CDTPropertyManager.getPagesCount(); j++) {
							Object p = CDTPropertyManager.getPage(j);
							if (p != null && p instanceof AbstractPage) { 
								AbstractPage ap = (AbstractPage)p;
								if (ap.displayedConfig) {
									ap.forEach(ICPropertyTab.UPDATE, resd);
									ap.forEach(ICPropertyTab.APPLY, r);
								}
							}
						}
					}
					break;
				case SAVE_MODE_APPLY:
					forEach(ICPropertyTab.APPLY, local_cfgd);
					break;
				case SAVE_MODE_OK:
					sendOK();
					break;
				} // end switch
				try {
					if (needs) // 
						CoreModel.getDefault().setProjectDescription(getProject(), local_prjd);
					else
						CDTPropertyManager.performOk(AbstractPage.this);
				} catch (CoreException e) {
					CUIPlugin.getDefault().logErrorMessage(UIMessages.getString("AbstractPage.11") + e.getLocalizedMessage()); //$NON-NLS-1$
				}
				updateViews(internalElement);
			}
		};
		IRunnableWithProgress op = new WorkspaceModifyDelegatingOperation(runnable);
		try {
			new ProgressMonitorDialog(getShell()).run(false, true, op);
		} catch (InvocationTargetException e) {
			Throwable e1 = e.getTargetException();
			CUIPlugin.errorDialog(getShell(), 
					UIMessages.getString("AbstractPage.8"),  //$NON-NLS-1$
					UIMessages.getString("AbstractPage.9"), e1, true); //$NON-NLS-1$
			return false;
		} catch (InterruptedException e) {}
		return true;
    }

	private void populateConfigurations() {
		// Do nothing if widget not created yet.
		if (configSelector == null)	return;

		// Do not re-read if list already created by another page
		if (cfgDescs == null) {
			cfgDescs = CDTPropertyManager.getProjectDescription(this, getProject()).getConfigurations();
			if (cfgDescs == null || cfgDescs.length == 0) return;
			Arrays.sort(cfgDescs, CDTListComparator.getInstance());
		} else {
			// just register in CDTPropertyManager;
			CDTPropertyManager.getProjectDescription(this, getProject());
		}
		
		// Clear and replace the contents of the selector widget
		configSelector.removeAll();
		for (int i = 0; i < cfgDescs.length; ++i) {
			configSelector.add(cfgDescs[i].getName());
			if (cfgIndex == -1 && cfgDescs[i].isActive()) 
				cfgIndex = i;
		}
		// Handling of All/Multiple configurations can be disabled
		if (CDTPrefUtil.getBool(CDTPrefUtil.KEY_MULTI)) {
			if (cfgDescs.length > 1) // "All cfgs" - shown if at least 2 cfgs available
				configSelector.add(UIMessages.getString("AbstractPage.4")); //$NON-NLS-1$
			if (cfgDescs.length > 2)// "Multi cfgs" - shown if at least 3 cfgs available
				configSelector.add(UIMessages.getString("AbstractPage.5")); //$NON-NLS-1$
		}
		configSelector.select(cfgIndex);
		handleConfigSelection();
	}

	public void updateButtons() {}
	public void updateMessage() { }
	public void updateTitle() {	}
	public void updateContainer() {	}

	public boolean isValid() {
		updateContainer();
		return super.isValid();
	}
	
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			handleResize(true);
			displayedConfig = true;
			if (excludeFromBuildCheck != null && resd != null)
				excludeFromBuildCheck.setSelection(resd.isExcluded());
			populateConfigurations();
		}

		if (itabs.size() < 1) return;
		
		if (currentTab == null && folder.getItemCount() > 0)	{
			Object ob = folder.getItem(0).getData();
			currentTab = (ICPropertyTab)ob;
		}
		if (currentTab != null)
			currentTab.handleTabEvent(ICPropertyTab.VISIBLE, visible ? NOT_NULL : null);
	}
	
	protected void handleResize(boolean visible) {
		if (visible && !isNewOpening) return; // do not duplicate
		if (visible) 
			isNewOpening = false;
		
		int saveMode = CDTPrefUtil.getInt(CDTPrefUtil.KEY_POSSAVE);
		if (saveMode == CDTPrefUtil.POSITION_SAVE_NONE) return;
		
		if (internalElement == null && !checkElement()) 
			return; // not initialized. Do not process
		IProject prj = getProject();
		if (prj == null) 
			return;	// preferences. Do not process. 
		QualifiedName WIDTH  = new QualifiedName(prj.getName(),".property.page.width"); //$NON-NLS-1$
		QualifiedName HEIGHT = new QualifiedName(prj.getName(),".property.page.height"); //$NON-NLS-1$
		QualifiedName XKEY = new QualifiedName(prj.getName(),".property.page.x"); //$NON-NLS-1$
		QualifiedName YKEY = new QualifiedName(prj.getName(),".property.page.y"); //$NON-NLS-1$
		Rectangle r = getShell().getBounds();
		try {
			if (visible) {
				String w = prj.getPersistentProperty(WIDTH);
				String h = prj.getPersistentProperty(HEIGHT);
				if (w != null) r.width  = Integer.parseInt(w);
				if (h != null) r.height = Integer.parseInt(h);
				if (saveMode == CDTPrefUtil.POSITION_SAVE_BOTH) {
					String x = prj.getPersistentProperty(XKEY);
					String y = prj.getPersistentProperty(YKEY);
					if (x != null) r.x = Integer.parseInt(x);
					if (y != null) r.y = Integer.parseInt(y);
				}
				getShell().setBounds(r);
			} else {
				prj.setPersistentProperty(WIDTH,  String.valueOf(r.width));
				prj.setPersistentProperty(HEIGHT, String.valueOf(r.height));
				prj.setPersistentProperty(XKEY, String.valueOf(r.x));
				prj.setPersistentProperty(YKEY, String.valueOf(r.y));
			}
		} catch (CoreException e) {}
	}

	public IPreferenceStore getPreferenceStore() {
		return CUIPlugin.getDefault().getPreferenceStore();
	}

	public Preferences getPreferences()	{
		return CUIPlugin.getDefault().getPluginPreferences();
	}
	
	public void enableConfigSelection (boolean enable) {
		if (configSelector != null) configSelector.setEnabled(enable);
		if (manageButton != null) manageButton.setEnabled(enable);
	}
	
	/**
	 * Returns configuration descriptions for given project
	 */
	public ICConfigurationDescription[] getCfgsReadOnly(IProject p) {
		ICProjectDescription prjd = CoreModel.getDefault().getProjectDescription(p, false); 
		if (prjd != null) 
			return prjd.getConfigurations();
		return null;
	}

	/**
	 * Returns loaded configuration descriptions for current project
	 */
	public ICConfigurationDescription[] getCfgsEditable() {
		return cfgDescs;
	}
	
	/** Checks whether project is new CDT project
	 * 
	 * @param p - project to check
	 * @returns true if it's new-style project. 
	 */ 
	public static boolean isCDTPrj(IProject p) {
		ICProjectDescription prjd = CoreModel.getDefault().getProjectDescription(p, false); 
		if (prjd == null) return false; 
		ICConfigurationDescription[] cfgs = prjd.getConfigurations();
		return (cfgs != null && cfgs.length > 0);
	}
	
	public boolean isCDTProject(IProject p) {
		return isCDTPrj(p);
	}
	
	public ICResourceDescription getResDesc() {
		if (resd == null) {
			if (cfgDescs == null) {
				populateConfigurations();
				if (cfgDescs == null || cfgDescs.length == 0) return null;
			}
			resd = getResDesc(cfgDescs[cfgIndex]);			
		}
		return resd;
	}

	public ICResourceDescription getResDesc(ICConfigurationDescription cf) {
		IAdaptable ad = getElement();
		
		if (isForProject()) 
			return cf.getRootFolderDescription();
		ICResourceDescription out = null;
		IResource res = (IResource)ad; 
		IPath p = res.getProjectRelativePath();
		if (isForFolder() || isForFile()) {
			out = cf.getResourceDescription(p, false);
			if (! p.equals(out.getPath()) ) {
				try {
					if (isForFolder())
						out = cf.createFolderDescription(p, (ICFolderDescription)out);
					else
						out = cf.createFileDescription(p, out);
				} catch (CoreException e) {
					System.out.println(UIMessages.getString("AbstractPage.10") + //$NON-NLS-1$
							p.toOSString() + "\n" + e.getLocalizedMessage()); //$NON-NLS-1$
				}
			}
		}
		return out;
	}
	
	protected void cfgChanged(ICConfigurationDescription _cfgd) {
		
		CConfigurationStatus st = _cfgd.getConfigurationStatus();
		if (st.isOK()) {
			errPane.setVisible(false);
		} else {
			errMessage.setText(st.getMessage());
			errPane.setVisible(true);
		}

		resd = getResDesc(_cfgd);
		
		if (excludeFromBuildCheck != null) {
			excludeFromBuildCheck.setEnabled(resd.canExclude(!resd.isExcluded()));
			excludeFromBuildCheck.setSelection(resd.isExcluded());
		}
		int x = CDTPropertyManager.getPagesCount();
		for (int i=0; i<x; i++) {
			Object p = CDTPropertyManager.getPage(i);
			if (p == null || !(p instanceof AbstractPage))
				continue;
			AbstractPage ap = (AbstractPage)p;
			if (ap.displayedConfig)
				ap.forEach(ICPropertyTab.UPDATE,getResDesc());
		}
	}
	
	public void dispose() {
		if (displayedConfig) forEach(ICPropertyTab.DISPOSE);
		
		if (!isNewOpening)
			handleResize(false); // save page size 
		isNewOpening = true;
		// clear static variables
		if (CDTPropertyManager.getPagesCount() == 0) {
			resd = null;
			cfgDescs = null;
		}
	} 
	
	/**
	 * The only method to be redefined in descendants
	 * @return 
	 * 		true if single page is required
	 * 		false if multiple pages are possible
	 */
	abstract protected boolean isSingle(); 
	
	/**
	 * Apply specified method to all tabs
	 */
	protected void forEach(int m) { forEach(m, null); }
	protected void forEach(int m, Object pars) {
		Iterator it = itabs.iterator();
		while(it.hasNext()) {
			InternalTab tab = (InternalTab)it.next();
			if (tab != null) tab.tab.handleTabEvent(m, pars);
		}
	}
	
	// redefine page width
	/*
	public Point computeSize() {
		Point p = super.computeSize();
		if (p.x > MAX_WIDTH) p.x = MAX_WIDTH;
		return p;
	}
	*/
	
	public static String getWeight(IConfigurationElement e) {
		String s = e.getAttribute(WEIGHT_NAME);
		return (s == null) ? EMPTY_STR : s;
	}
	
	private synchronized void loadExtensionsSynchronized(Composite parent)
	{
		// Get the extensions
		IExtensionPoint extensionPoint = Platform.getExtensionRegistry()
				.getExtensionPoint(EXTENSION_POINT_ID);
		if (extensionPoint == null) return;
		IExtension[] extensions = extensionPoint.getExtensions();
		if (extensions == null) return;
		
		for (int i = 0; i < extensions.length; ++i)	{
			IConfigurationElement[] elements = extensions[i].getConfigurationElements();
			
			Arrays.sort(elements, CDTListComparator.getInstance());
			
			for (int k = 0; k < elements.length; k++) {
				if (elements[k].getName().equals(ELEMENT_NAME)) {
					if (loadTab(elements[k], parent)) return;
				} else {
					System.out.println(UIMessages.getString("AbstractPage.13") + elements[k].getName()); //$NON-NLS-1$
				}
			}
		}
	}

	/**
	 * 
	 * @param element
	 * @param parent
	 * @return true if we should exit (no more loadings)
	 *         false if we should continue extensions scan.  
	 * @throws BuildException
	 */
	private boolean loadTab(IConfigurationElement element, Composite parent) {
		//	MBSCustomPageData currentPageData;
		// Check whether it's our tab
		if (!this.getClass().getName().equals(element.getAttribute(PARENT_NAME))) return false;
		
		ICPropertyTab page = null;
		try {
			page = (ICPropertyTab) element.createExecutableExtension(CLASS_NAME);
		} catch (CoreException e) {
			System.out.println(UIMessages.getString("AbstractPage.14") +  //$NON-NLS-1$
					e.getLocalizedMessage());
			return false; 
		}
		if (page == null) return false;
		
		Image _img = getIcon(element);
		if (_img != null) page.handleTabEvent(ICPropertyTab.SET_ICON, _img);
		
		if (isSingle()) {
			// note that name, image and tooltip
			// are ignored for single page.
			page.createControls(parent, this);
			InternalTab itab = new InternalTab(parent, EMPTY_STR, null, page, null);
			itabs.add(itab);
			currentTab = page;
			return true; // don't load other tabs
		} else {  // tabbed page
			String _name   = element.getAttribute(TEXT_NAME);
			String _tip = element.getAttribute(TIP_NAME);

			Composite _comp = new Composite(folder, SWT.NONE);
			page.createControls(_comp, this);	    
			InternalTab itab = new InternalTab(_comp, _name, _img, page, _tip);
			itab.createOn(folder);
			itabs.add(itab);
			return false;
		}
	}
	
	private Image getIcon(IConfigurationElement config) {
		ImageDescriptor idesc = null;
		try {
			String iconName = config.getAttribute(IMAGE_NAME);
			if (iconName != null) {
				URL pluginInstallUrl = Platform.getBundle(config.getDeclaringExtension().getContributor().getName()).getEntry("/"); //$NON-NLS-1$			
				idesc = ImageDescriptor.createFromURL(new URL(pluginInstallUrl, iconName));
			}
		} catch (MalformedURLException exception) {}
		return (idesc == null) ? null : idesc.createImage();
	}
	
	public void informAll(int code, Object data) {
		for (int i=0; i<CDTPropertyManager.getPagesCount(); i++) {
			Object p = CDTPropertyManager.getPage(i);
			if (p == null || !(p instanceof AbstractPage))
				continue;
			AbstractPage ap = (AbstractPage)p;
			ap.forEach(code, data);
		}
	}

	public void informPages(int code, Object data) {
		for (int i=0; i<CDTPropertyManager.getPagesCount(); i++) {
			Object p = CDTPropertyManager.getPage(i);
			if (p == null || !(p instanceof AbstractPage))
				continue;
			AbstractPage ap = (AbstractPage)p;
			ap.handleMessage(code, data);
		}
	}

	public void handleMessage(int code, Object data) {
		switch (code) {
			// First re-check visibility of all tabs.
		    // While tab deletion can be made on the fly,
		    // tabs adding will be made by re-creation
		    // of all elements, to preserve their order 
			case ICPropertyTab.MANAGEDBUILDSTATE:
				if (folder == null) {
					if (itabs == null || itabs.size() == 0) 
						return;
					ICPropertyTab t = ((InternalTab)itabs.get(0)).tab;
					if (! t.canBeVisible())
						t.handleTabEvent(ICPropertyTab.VISIBLE, null);
					return;
				}
				boolean willAdd = false;
				TabItem[] ts = folder.getItems();
				int x = folder.getSelectionIndex();
				String currHeader = (x == -1) ? null : ts[x].getText();
				for (int i=0; i<itabs.size(); i++) {
					InternalTab itab = (InternalTab)itabs.get(i);
					TabItem ti = null;
					for (int j=0; j<ts.length; j++) {
						if (ts[j].isDisposed()) continue;
						if (ts[j].getData() == itab.tab) {
							ti = ts[j];
							break;
						}
					}
					if (itab.tab.canBeVisible()) {
						if (ti == null)	{
							willAdd = true;
							break;
						}
					} else {
						if (ti != null) ti.dispose();
					}
				}
				// in case of new tab added, 
				// we have to dispose and re-create all tabs
				if (willAdd) {
					for (int j=0; j<ts.length; j++) 
						if (ts[j] != null && !ts[j].isDisposed())
							ts[j].dispose();
					TabItem ti = null;
					for (int i=0; i<itabs.size(); i++) {
						InternalTab itab = (InternalTab)itabs.get(i);
						if (itab.tab.canBeVisible()) {
							TabItem currTI = itab.createOn(folder);
							if (currHeader != null && currHeader.equals(itab.text))
								ti = currTI;
						}
					}
					if (ti != null) folder.setSelection(ti);
				}
				break;
		}
	}
	
	/**
	 * Performs conversion of incoming element to internal representation.
	 */
	protected boolean checkElement() {
		IAdaptable el = super.getElement();
		if (el instanceof ICElement) 
			internalElement = ((ICElement)el).getResource();
		else if (el instanceof IResource) 
			internalElement = (IResource)el;
		if (internalElement == null) return false;
		isProject = internalElement instanceof IProject;
		isFolder  = internalElement instanceof IFolder;
		isFile    = internalElement instanceof IFile;
		return true;
	}
	
	// override parent's method to use proper class
	public IAdaptable getElement() {
		if (internalElement == null && !checkElement()) 
			throw (new NullPointerException(UIMessages.getString("AbstractPage.15"))); //$NON-NLS-1$
		return internalElement; 
	}
	
	public boolean isForProject()  { return isProject; }
	public boolean isForFolder()   { return isFolder; }
	public boolean isForFile()     { return isFile; }
	public boolean isForPrefs()    { return false; }
	public boolean isMultiCfg()    { return resd instanceof ICMultiItemsHolder; }
	
	/**
	 * Checks whether CDT property pages can be open for given object.
	 * In particular, header files and text files are not allowed.
	 * 
	 * Note, that org.eclipse.cdt.ui.plugin.xml contains appropriate
	 * filters to avoid displaying CDT pages for unwanted objects. 
	 * So this check is only backup, it would prevent from NullPointer
	 * exceptions in case when xml filters were modified somehow.  
	 * 
	 * @return - true if element is applicable to CDT pages.
	 */
	public boolean isApplicable() {
		if (internalElement == null && !checkElement())
			return false; // unknown element
		if (isForFile()) // only source files are applicable
			return true; //CoreModel.isValidSourceUnitName(getProject(), internalElement.getName());
		else
			return true; // Projects and folders are always applicable
	}

	// update views (in particular, display resource configurations)
	public static void updateViews(IResource res) {
		if (res == null) return;  
		IWorkbenchPartReference refs[] = CUIPlugin.getActiveWorkbenchWindow().getActivePage().getViewReferences();
		for (int k = 0; k < refs.length; k++) {
			IWorkbenchPart part = refs[k].getPart(false);
			if (part != null && part instanceof IPropertyChangeListener)
				((IPropertyChangeListener)part).propertyChange(new PropertyChangeEvent(res, PreferenceConstants.PREF_SHOW_CU_CHILDREN, null, null));
		}
	}
	
	public void resize() {
		Shell sh = parentComposite.getShell();
		Point p0 = sh.getLocation();
		Point p1 = sh.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		Rectangle r = sh.getDisplay().getClientArea();
		p1.x = Math.min(p1.x, (r.width - p0.x));
		p1.y = Math.min(p1.y, (r.height - p0.y));
		sh.setSize(p1);
	}
	
}
