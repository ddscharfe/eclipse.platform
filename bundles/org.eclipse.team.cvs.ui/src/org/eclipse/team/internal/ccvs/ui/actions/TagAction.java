/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.window.Window;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.ICVSResource;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.ICVSUIConstants;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ccvs.ui.TagAsVersionDialog;
import org.eclipse.team.internal.ccvs.ui.operations.ITagOperation;
import org.eclipse.team.internal.ccvs.ui.repo.RepositoryManager;

/**
 * TagAction tags the selected resources with a version tag specified by the user.
 */
public abstract class TagAction extends WorkspaceAction {
	
	// remember if the execute action was cancelled
	private boolean wasCancelled = false;

	/**
	 * @see CVSAction#execute(IAction)
	 */
	public void execute(IAction action) throws InvocationTargetException, InterruptedException {
		setWasCancelled(false);
		if (!performPrompting()) {
			setWasCancelled(true);
			return;
		}
		
		// Prompt for the tag name
		final ITagOperation[] result = new ITagOperation[1];
		getShell().getDisplay().syncExec(new Runnable() {
			public void run() {
				result[0] = configureOperation();
				if (result[0] == null)  {
					return;
				}
			}});
		
		if (result[0] == null)  {
			setWasCancelled(true);
			return;
		}
		
		result[0].run();
	}
	
	protected boolean performPrompting()  {
		return true;
	}
	
	/**
	 * Prompts the user for a tag name.
	 * Note: This method is designed to be overridden by test cases.
	 * @return the operation, or null to cancel
	 */
	protected ITagOperation configureOperation() {
		IPreferenceStore store = CVSUIPlugin.getPlugin().getPreferenceStore();
		ITagOperation operation = createTagOperation();
		TagAsVersionDialog dialog = new TagAsVersionDialog(getShell(),
											Policy.bind("TagAction.tagResources"), //$NON-NLS-1$
											operation);
		if (dialog.open() != Window.OK) return null;

		// The user has indicated they want to force a move.  Make sure they really do.		
		if (dialog.shouldMoveTag() && store.getBoolean(ICVSUIConstants.PREF_CONFIRM_MOVE_TAG))  {
			MessageDialogWithToggle confirmDialog = MessageDialogWithToggle.openYesNoQuestion(getShell(), 
				Policy.bind("TagAction.moveTagConfirmTitle"),  //$NON-NLS-1$
				Policy.bind("TagAction.moveTagConfirmMessage", dialog.getTagName()), //$NON-NLS-1$
				null,
				false,
				null,
				null);
			
			if (confirmDialog.getReturnCode() == IDialogConstants.OK_ID)  {
				store.setValue(ICVSUIConstants.PREF_CONFIRM_MOVE_TAG, !confirmDialog.getToggleState());
			} else  {
				return null;
			}
		}
		
		// The user is a cowboy and wants to do it.
		return dialog.getOperation();
	}
	
	protected abstract ITagOperation createTagOperation();

	protected String getErrorTitle() {
		return Policy.bind("TagAction.tagErrorTitle"); //$NON-NLS-1$
	}
	
	protected String getWarningTitle() {
		return Policy.bind("TagAction.tagWarningTitle"); //$NON-NLS-1$
	}
	
	/**
	 * @see org.eclipse.team.internal.ccvs.ui.actions.WorkspaceAction#isEnabledForAddedResources()
	 */
	protected boolean isEnabledForAddedResources() {
		return false;
	}

	public boolean wasCancelled() {
		return wasCancelled;
	}

	public void setWasCancelled(boolean b) {
		wasCancelled = b;
	}

	public static void broadcastTagChange(final ICVSResource[] resources, final CVSTag tag) throws InvocationTargetException, InterruptedException {
		final RepositoryManager manager = CVSUIPlugin.getPlugin().getRepositoryManager();
		manager.run(new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) {
				try {
					for (int i = 0; i < resources.length; i++) {
						ICVSResource resource = resources[i];
						// Cache the new tag creation even if the tag may have had warnings.
						manager.addTags(getRootParent(resource), new CVSTag[] {tag});
					}
				} catch (CVSException e) {
					CVSUIPlugin.log(e);
				}
			}
			private ICVSResource getRootParent(ICVSResource resource) throws CVSException {
				if (!resource.isManaged()) return resource;
				ICVSFolder parent = resource.getParent();
				if (parent == null) return resource;
				// Special check for a parent which is the repository itself
				if (parent.getName().length() == 0) return resource;
				return getRootParent(parent);
			}
		}, new NullProgressMonitor());
	}
}

