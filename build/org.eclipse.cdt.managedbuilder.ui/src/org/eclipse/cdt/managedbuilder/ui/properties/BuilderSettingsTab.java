/*******************************************************************************
 * Copyright (c) 2007 Intel Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Intel Corporation - Initial API and implementation
 * IBM Corporation
 *******************************************************************************/
package org.eclipse.cdt.managedbuilder.ui.properties;

import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.cdt.managedbuilder.core.IBuilder;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.internal.buildmodel.BuildProcessManager;
import org.eclipse.cdt.managedbuilder.internal.core.Configuration;
import org.eclipse.cdt.newmake.core.IMakeBuilderInfo;
import org.eclipse.cdt.ui.newui.AbstractCPropertyTab;
import org.eclipse.cdt.ui.newui.TriButton;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.accessibility.AccessibleListener;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;


public class BuilderSettingsTab extends AbstractCBuildPropertyTab {
	// Widgets
	//1
	TriButton b_useDefault;
	Combo  c_builderType;
	Text   t_buildCmd; 
	//2
	TriButton b_genMakefileAuto;
	TriButton b_expandVars;
	//3
	TriButton b_stopOnError;
	TriButton b_parallel;

	Button b_parallelOpt;
	Button b_parallelNum;
	Spinner parallelProcesses;
	//4 
	Label  title2;
	Button b_autoBuild;
	Text   t_autoBuild;
	Button b_cmdBuild;
	Text   t_cmdBuild;
	Button b_cmdClean;
	Text   t_cmdClean;	
	//5
	Text   t_dir;
	Button b_dirWsp;
	Button b_dirFile;
	Button b_dirVars;

	protected final int cpuNumber = BuildProcessManager.checkCPUNumber(); 
	IBuilder bld;
	Configuration cfg;
	
	public void createControls(Composite parent) {
		super.createControls(parent);
		usercomp.setLayout(new GridLayout(1, false));
		
		ScrolledComposite sc = new ScrolledComposite(usercomp, SWT.V_SCROLL | SWT.H_SCROLL);
		sc.setLayoutData(new GridData(GridData.FILL_BOTH));
	    sc.setExpandHorizontal(true);
	    sc.setExpandVertical(true);

	    Composite comp = new Composite(sc, SWT.NONE);
		sc.setContent(comp);
		comp.setLayout(new GridLayout(1, false));

		// Builder group
		Group g1 = setupGroup(comp, Messages.getString("BuilderSettingsTab.0"), 3, GridData.FILL_HORIZONTAL); //$NON-NLS-1$
		setupLabel(g1, Messages.getString("BuilderSettingsTab.1"), 1, GridData.BEGINNING); //$NON-NLS-1$
		c_builderType = new Combo(g1, SWT.READ_ONLY | SWT.DROP_DOWN | SWT.BORDER);
		setupControl(c_builderType, 2, GridData.FILL_HORIZONTAL);
		c_builderType.add(Messages.getString("BuilderSettingsTab.2")); //$NON-NLS-1$
		c_builderType.add(Messages.getString("BuilderSettingsTab.3")); //$NON-NLS-1$
		c_builderType.addSelectionListener(new SelectionAdapter() {
		    public void widgetSelected(SelectionEvent event) {
				cfg.enableInternalBuilder(c_builderType.getSelectionIndex() == 1);
		    	updateButtons();
		 }});
		
		b_useDefault = setupTri(g1, Messages.getString("BuilderSettingsTab.4"), 3, GridData.BEGINNING); //$NON-NLS-1$
		
		setupLabel(g1, Messages.getString("BuilderSettingsTab.5"), 1, GridData.BEGINNING); //$NON-NLS-1$
		t_buildCmd = setupBlock(g1, b_useDefault);
		t_buildCmd.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
	        	String fullCommand = t_buildCmd.getText().trim();
	        	String buildCommand = parseMakeCommand(fullCommand);
	        	String buildArgs = fullCommand.substring(buildCommand.length()).trim();
	        	if(!buildCommand.equals(bld.getCommand()) 
	        			|| !buildArgs.equals(bld.getArguments())){
		        	bld.setCommand(buildCommand);
		        	bld.setArguments(buildArgs);
		        }
			}});
				
		Group g2 = setupGroup(comp, Messages.getString("BuilderSettingsTab.6"), 2, GridData.FILL_HORIZONTAL); //$NON-NLS-1$
		((GridLayout)(g2.getLayout())).makeColumnsEqualWidth = true;
		
		b_genMakefileAuto = setupTri(g2, Messages.getString("BuilderSettingsTab.7"), 1, GridData.BEGINNING); //$NON-NLS-1$
		b_expandVars  = setupTri(g2, Messages.getString("BuilderSettingsTab.8"), 1, GridData.BEGINNING); //$NON-NLS-1$

		// Build setting group
		Group g3 = setupGroup(comp, Messages.getString("BuilderSettingsTab.9"), 2, GridData.FILL_HORIZONTAL); //$NON-NLS-1$
		((GridLayout)(g3.getLayout())).makeColumnsEqualWidth = true;
		((GridLayout)(g3.getLayout())).verticalSpacing = 0;
		
		Composite c1 = new Composite(g3, SWT.NONE);
		setupControl(c1, 1, GridData.FILL_BOTH);
		GridData gd = (GridData)c1.getLayoutData();
		gd.verticalSpan = 2;
		c1.setLayoutData(gd);
		GridLayout gl = new GridLayout(1, false);
		gl.marginWidth = 0;
		c1.setLayout(gl);
		
		b_stopOnError = setupTri(c1, Messages.getString("BuilderSettingsTab.10"), 1, GridData.BEGINNING); //$NON-NLS-1$
		
		Composite c2 = new Composite(g3, SWT.NONE);
		setupControl(c2, 1, GridData.FILL_BOTH);
		gl = new GridLayout(1, false);
		gl.marginWidth = 0;
		c2.setLayout(gl);
		
		b_parallel = setupTri(c2, Messages.getString("BuilderSettingsTab.11"), 1, GridData.BEGINNING); //$NON-NLS-1$

		Composite c3 = new Composite(g3, SWT.NONE);
		setupControl(c3, 1, GridData.FILL_BOTH);
		gl = new GridLayout(2, false);
		gl.marginWidth = 0;
		c3.setLayout(gl);
		
		b_parallelOpt= new Button(c3, SWT.RADIO);
		b_parallelOpt.setText(Messages.getString("BuilderSettingsTab.12")); //$NON-NLS-1$
		setupControl(b_parallelOpt, 2, GridData.BEGINNING);
		((GridData)(b_parallelOpt.getLayoutData())).horizontalIndent = 15;
		b_parallelOpt.addSelectionListener(new SelectionAdapter() {
		    public void widgetSelected(SelectionEvent event) {
				cfg.setParallelDef(b_parallelOpt.getSelection());
				updateButtons();
		 }});
		
		b_parallelNum= new Button(c3, SWT.RADIO);
		b_parallelNum.setText(Messages.getString("BuilderSettingsTab.13")); //$NON-NLS-1$
		setupControl(b_parallelNum, 1, GridData.BEGINNING);
		((GridData)(b_parallelNum.getLayoutData())).horizontalIndent = 15;
		b_parallelNum.addSelectionListener(new SelectionAdapter() {
		    public void widgetSelected(SelectionEvent event) {
				cfg.setParallelDef(!b_parallelNum.getSelection());
				updateButtons();
		 }});

		parallelProcesses = new Spinner(c3, SWT.BORDER);
		setupControl(parallelProcesses, 1, GridData.BEGINNING);
		parallelProcesses.setValues(cpuNumber, 1, 10000, 0, 1, 10);
		parallelProcesses.addSelectionListener(new SelectionAdapter () {
			public void widgetSelected(SelectionEvent e) {
				cfg.setParallelNumber(parallelProcesses.getSelection());
				updateButtons();
			}
		});

		// Workbench behaviour group
		AccessibleListener makeTargetLabelAccessibleListener = new AccessibleAdapter() {
			public void getName(AccessibleEvent e) {
				e.result = Messages.getString("BuilderSettingsTab.16");
			}
		};
		Group g4 = setupGroup(comp, Messages.getString("BuilderSettingsTab.14"), 3, GridData.FILL_HORIZONTAL); //$NON-NLS-1$
		setupLabel(g4, Messages.getString("BuilderSettingsTab.15"), 1, GridData.BEGINNING); //$NON-NLS-1$
		title2 = setupLabel(g4, Messages.getString("BuilderSettingsTab.16"), 2, GridData.BEGINNING); //$NON-NLS-1$
		b_autoBuild = setupCheck(g4, Messages.getString("BuilderSettingsTab.17"), 1, GridData.BEGINNING); //$NON-NLS-1$
		t_autoBuild = setupBlock(g4, b_autoBuild);
		t_autoBuild.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				try {
					bld.setBuildAttribute(IMakeBuilderInfo.BUILD_TARGET_AUTO, t_autoBuild.getText());
				} catch (CoreException ex) {}
			}} );
		t_autoBuild.getAccessible().addAccessibleListener(makeTargetLabelAccessibleListener);
		setupLabel(g4, Messages.getString("BuilderSettingsTab.18"), 3, GridData.BEGINNING); //$NON-NLS-1$
		b_cmdBuild = setupCheck(g4, Messages.getString("BuilderSettingsTab.19"), 1, GridData.BEGINNING); //$NON-NLS-1$
		t_cmdBuild = setupBlock(g4, b_cmdBuild);
		t_cmdBuild.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				try { 
					bld.setBuildAttribute(IMakeBuilderInfo.BUILD_TARGET_INCREMENTAL, t_cmdBuild.getText());
				} catch (CoreException ex) {}
			}} );
		t_cmdBuild.getAccessible().addAccessibleListener(makeTargetLabelAccessibleListener);
		b_cmdClean = setupCheck(g4, Messages.getString("BuilderSettingsTab.20"), 1, GridData.BEGINNING); //$NON-NLS-1$
		t_cmdClean = setupBlock(g4, b_cmdClean);
		t_cmdClean.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				try { 
					bld.setBuildAttribute(IMakeBuilderInfo.BUILD_TARGET_CLEAN, t_cmdClean.getText());
				} catch (CoreException ex) {}
			}} );
		t_cmdClean.getAccessible().addAccessibleListener(makeTargetLabelAccessibleListener);

		// Build location group
		Group g5 = setupGroup(comp, Messages.getString("BuilderSettingsTab.21"), 2, GridData.FILL_HORIZONTAL); //$NON-NLS-1$
		setupLabel(g5, Messages.getString("BuilderSettingsTab.22"), 1, GridData.BEGINNING); //$NON-NLS-1$
		t_dir = setupText(g5, 1, GridData.FILL_HORIZONTAL);
		t_dir.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				bld.setBuildPath(t_dir.getText());
			}} );
		Composite c = new Composite(g5, SWT.NONE);
		setupControl(c, 2, GridData.FILL_HORIZONTAL);
		GridLayout f = new GridLayout(4, false);
		c.setLayout(f);
		Label dummy = new Label(c, 0);
		dummy.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		b_dirWsp = setupBottomButton(c, WORKSPACEBUTTON_NAME);
		b_dirFile = setupBottomButton(c, FILESYSTEMBUTTON_NAME);
		b_dirVars = setupBottomButton(c, VARIABLESBUTTON_NAME);

		sc.setMinSize(comp.computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}

	void setManagedBuild(boolean enable) {
		try {
			bld.setManagedBuildOn(enable);
			page.informPages(MANAGEDBUILDSTATE, null);
			updateButtons();
		} catch (CoreException ex) {}
	}
	
	/**
	 * sets widgets states
	 */
	protected void updateButtons() {
		bld = cfg.getEditableBuilder();
		
		b_genMakefileAuto.setEnabled(cfg.supportsBuild(true));
		b_genMakefileAuto.setSelection(bld.isManagedBuildOn());
		b_useDefault.setSelection(bld.isDefaultBuildCmd());

		c_builderType.select(cfg.isInternalBuilderEnabled() ? 1 : 0);
		c_builderType.setEnabled(
				cfg.canEnableInternalBuilder(true) &&
				cfg.canEnableInternalBuilder(false));
		
		t_buildCmd.setText(getMC());
		
		b_stopOnError.setSelection(bld.isStopOnError());
		b_stopOnError.setEnabled(
				bld.supportsStopOnError(true) &&
				bld.supportsStopOnError(false));
		// parallel
		b_parallel.setSelection(cfg.getInternalBuilderParallel());
		b_parallelOpt.setSelection(cfg.getParallelDef());
		b_parallelNum.setSelection(!cfg.getParallelDef());
		int n = cfg.getParallelNumber();
		if (n < 0) n = -n;
		parallelProcesses.setSelection(n);

		b_parallel.setVisible(bld.supportsParallelBuild());
		b_parallelOpt.setVisible(bld.supportsParallelBuild());
		b_parallelNum.setVisible(bld.supportsParallelBuild());
		parallelProcesses.setVisible(bld.supportsParallelBuild());
		
		if(!bld.canKeepEnvironmentVariablesInBuildfile())
			b_expandVars.setEnabled(false);
		else {
			b_expandVars.setEnabled(true);
			b_expandVars.setSelection(!bld.keepEnvironmentVariablesInBuildfile());
		}
		t_dir.setText(bld.getBuildPath());
		//	cfg.getBuildData().getBuilderCWD().toOSString());
		
		boolean mbOn = bld.isManagedBuildOn();
		t_dir.setEnabled(!mbOn);
		b_dirVars.setEnabled(!mbOn);
		b_dirWsp.setEnabled(!mbOn);
		b_dirFile.setEnabled(!mbOn);
		
		b_autoBuild.setSelection(bld.isAutoBuildEnable());
		t_autoBuild.setText(bld.getBuildAttribute(IBuilder.BUILD_TARGET_AUTO, EMPTY_STR));
		b_cmdBuild.setSelection(bld.isIncrementalBuildEnabled());
		t_cmdBuild.setText(bld.getBuildAttribute(IBuilder.BUILD_TARGET_INCREMENTAL, EMPTY_STR));
		b_cmdClean.setSelection(bld.isCleanBuildEnabled());
		t_cmdClean.setText(bld.getBuildAttribute(IBuilder.BUILD_TARGET_CLEAN, EMPTY_STR));
		
		boolean external = (c_builderType.getSelectionIndex() == 0);
		boolean parallel = b_parallel.getSelection();
		
		b_useDefault.setEnabled(external);
		t_buildCmd.setEnabled(external);
		((Control)t_buildCmd.getData()).setEnabled(external & ! b_useDefault.getSelection());
		
		b_genMakefileAuto.setEnabled(external && cfg.supportsBuild(true));
		b_expandVars.setEnabled(external && b_genMakefileAuto.getSelection());
		b_parallelNum.setEnabled(parallel);
		b_parallelOpt.setEnabled(parallel);
		parallelProcesses.setEnabled(parallel & b_parallelNum.getSelection());
		
		title2.setVisible(external);
		t_autoBuild.setVisible(external);
		((Control)t_autoBuild.getData()).setVisible(external);
		t_cmdBuild.setVisible(external);
		((Control)t_cmdBuild.getData()).setVisible(external);
		t_cmdClean.setVisible(external);
		((Control)t_cmdClean.getData()).setVisible(external);
		
		if (external) {
			checkPressed(b_useDefault);
			checkPressed(b_autoBuild);
			checkPressed(b_cmdBuild);
			checkPressed(b_cmdClean);
		}
	}
	
	Button setupBottomButton(Composite c, String name) {
		Button b = new Button(c, SWT.PUSH);
		b.setText(name);
		GridData fd = new GridData(GridData.CENTER);
		fd.minimumWidth = BUTTON_WIDTH; 
		b.setLayoutData(fd);
		b.setData(t_dir);
		b.addSelectionListener(new SelectionAdapter() {
	        public void widgetSelected(SelectionEvent event) {
	        	buttonVarPressed(event);
	        }});
		return b;
	}
	
	/**
	 * Sets up text + corresponding button
	 * Checkbox can be implemented either by Button or by TriButton
	 */
	Text setupBlock(Composite c, Control check) {
		Text t = setupText(c, 1, GridData.FILL_HORIZONTAL);
		Button b = setupButton(c, VARIABLESBUTTON_NAME, 1, GridData.END);
		b.setData(t); // to get know which text is affected
		t.setData(b); // to get know which button to enable/disable
		b.addSelectionListener(new SelectionAdapter() {
	        public void widgetSelected(SelectionEvent event) {
	        	buttonVarPressed(event);
	        }});
		if (check != null) check.setData(t);
		return t;
	}
	
	/*
	 * Unified handler for "Variables" buttons
	 */
	void buttonVarPressed(SelectionEvent e) {
		Widget b = e.widget;
		if (b == null || b.getData() == null) return; 
		if (b.getData() instanceof Text) {
			String x = null;
			if (b.equals(b_dirWsp)) {
				x = getWorkspaceDirDialog(usercomp.getShell(), EMPTY_STR);
				if (x != null) ((Text)b.getData()).setText(x);
			} else if (b.equals(b_dirFile)) {
				x = getFileSystemDirDialog(usercomp.getShell(), EMPTY_STR);
				if (x != null) ((Text)b.getData()).setText(x);
			} else { 
				x = AbstractCPropertyTab.getVariableDialog(usercomp.getShell(), getResDesc().getConfiguration());
				if (x != null) ((Text)b.getData()).insert(x);
			}
		}
	}
	
    public void checkPressed(SelectionEvent e) {
    	checkPressed((Control)e.widget);
    	updateButtons();
    }
	
	void checkPressed(Control b) {	
		if (b == null) return;
		
		boolean val = false;
		if (b instanceof Button) val = ((Button)b).getSelection();
		else if (b instanceof TriButton) val = ((TriButton)b).getSelection();
		
		if (b.getData() instanceof Text) {
			Text t = (Text)b.getData();
			if (b == b_useDefault) { val = !val; }
			t.setEnabled(val);
			if (t.getData() != null && t.getData() instanceof Control) {
				Control c = (Control)t.getData();
				c.setEnabled(val);
			}
		}
		try {
			if (b == b_autoBuild) {
				bld.setAutoBuildEnable(val);				
			} else if (b == b_cmdBuild) {
				bld.setIncrementalBuildEnable(val);				
			} else if (b == b_cmdClean) {
				bld.setCleanBuildEnable(val);
			} else if (b == b_useDefault) {
				bld.setUseDefaultBuildCmd(!val);
			} else if (b == b_genMakefileAuto) {
				setManagedBuild(val);
			} else if (b == b_expandVars) {
				if(bld.canKeepEnvironmentVariablesInBuildfile()) 
					bld.setKeepEnvironmentVariablesInBuildfile(!val);
			} else if (b == b_stopOnError) {
				bld.setStopOnError(val);
			} else if (b == b_parallel) {
				bld.setParallelBuildOn(val);
			}
		} catch (CoreException e) {}
	}

	/**
	 * get make command
	 * @return
	 */
	private String getMC() {
		String makeCommand = bld.getCommand();
		String makeArgs = bld.getArguments();
		if (makeArgs != null) {	makeCommand += " " + makeArgs; } //$NON-NLS-1$
		return makeCommand;
	}
	/**
	 * Performs common settings for all controls
	 * (Copy from config to widgets)
	 * @param cfgd - 
	 */
	
	public void updateData(ICResourceDescription cfgd) {
		if (cfgd == null) return;
		IConfiguration icfg = getCfg(cfgd.getConfiguration());
		if (!(icfg instanceof Configuration)) return;
		cfg = (Configuration)icfg;
		updateButtons();
	}

	public void performApply(ICResourceDescription src, ICResourceDescription dst) {
		Configuration cfg01 = (Configuration)getCfg(src.getConfiguration());
		Configuration cfg02 = (Configuration)getCfg(dst.getConfiguration());
		cfg02.enableInternalBuilder(cfg01.isInternalBuilderEnabled());
		copyBuilders(cfg01.getBuilder(), cfg02.getEditableBuilder());
	}
	
	private void copyBuilders(IBuilder b1, IBuilder b2) {  	
		try {
			b2.setUseDefaultBuildCmd(b1.isDefaultBuildCmd());
			if (!b1.isDefaultBuildCmd()) {
				b2.setCommand(b1.getCommand());
				b2.setArguments(b1.getArguments());
			} else {
				b2.setCommand(null);
				b2.setArguments(null);
			}
			b2.setStopOnError(b1.isStopOnError());
			b2.setParallelBuildOn(b1.isParallelBuildOn());
			b2.setParallelizationNum(b1.getParallelizationNum());
			if (b2.canKeepEnvironmentVariablesInBuildfile())
				b2.setKeepEnvironmentVariablesInBuildfile(b1.keepEnvironmentVariablesInBuildfile());
			b2.setBuildPath(null);
		
			b2.setAutoBuildEnable((b1.isAutoBuildEnable()));
			b2.setBuildAttribute(IBuilder.BUILD_TARGET_AUTO, (b1.getBuildAttribute(IBuilder.BUILD_TARGET_AUTO, EMPTY_STR)));
			b2.setCleanBuildEnable(b1.isCleanBuildEnabled());
			b2.setBuildAttribute(IBuilder.BUILD_TARGET_CLEAN, (b1.getBuildAttribute(IBuilder.BUILD_TARGET_CLEAN, EMPTY_STR)));
			b2.setIncrementalBuildEnable(b1.isIncrementalBuildEnabled());
			b2.setBuildAttribute(IBuilder.BUILD_TARGET_INCREMENTAL, (b1.getBuildAttribute(IBuilder.BUILD_TARGET_INCREMENTAL, EMPTY_STR)));
		
			b2.setManagedBuildOn(b1.isManagedBuildOn());
		} catch (CoreException ex) {
			ManagedBuilderUIPlugin.log(ex);
		}
	}

	/* (non-Javadoc)
	 * 
	 * @param string
	 * @return
	 */
	private String parseMakeCommand(String rawCommand) {
		String[] result = rawCommand.split("\\s"); //$NON-NLS-1$
		if (result != null && result.length > 0)
			return result[0];
		else
			return rawCommand;
		
	}
	// This page can be displayed for project only
	public boolean canBeVisible() {
		return page.isForProject() || page.isForPrefs();
	}
	
	public void setVisible (boolean b) {
		super.setVisible(b);
	}

	protected void performDefaults() {
		copyBuilders(bld.getSuperClass(), bld);
		updateData(getResDesc());
	}
}
