/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.mappings;

import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.diff.*;
import org.eclipse.team.core.mapping.IMergeContext;
import org.eclipse.ui.IWorkbenchPart;

public class ModelReplaceOperation extends ModelUpdateOperation {

	public ModelReplaceOperation(IWorkbenchPart part, ResourceMapping[] selectedMappings, ResourceMappingContext context) {
		super(part, selectedMappings, context);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.operations.ResourceMappingMergeOperation#isAttemptHeadlessMerge()
	 */
	protected boolean isAttemptHeadlessMerge() {
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.operations.ResourceMappingMergeOperation#hasChangesOfInterest()
	 */
	protected boolean hasChangesOfInterest() {
		IMergeContext context = (IMergeContext)getContext();
		return !context.getDiffTree().isEmpty();
	}
	
	protected boolean performMerge(IProgressMonitor monitor) throws CoreException {
		// TODO: cancel must free context to avoid leaking
		if (!hasLocalChanges() || promptForOverwrite()) {
			// Merge all changes ignoring any locla changes
			IDiffNode[] diffs = getContext().getDiffTree().getDiffs(getScope().getTraversals());
			((IMergeContext)getContext()).merge(diffs, true /* overwrite */, monitor);
			getContext().dispose();
			return true;
		}
		return false;
	}

	/*
	 * Mde porotected to be overriden by test cases.
	 */
	protected boolean promptForOverwrite() {
		final int[] result = new int[] { 1 };
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
		        MessageDialog dialog = new MessageDialog(getShell(), "Overwrite Local Changes?", null, // accept
		                // the
		                // default
		                // window
		                // icon
		        		"The selected elements contain local changes. You may choose to overwrite the changes or preview them.", 
		        		MessageDialog.QUESTION, new String[] { "&Overwrite", "&Preview",
		                        IDialogConstants.CANCEL_LABEL }, result[0]); // preview is the default
		        
		        result[0] = dialog.open();

			};
		});
        if (result[0] == 2)
        	throw new OperationCanceledException();
        return result[0] == 0;
	}

	private boolean hasLocalChanges() {
		return hasChangesMatching(getContext().getDiffTree(), new FastDiffNodeFilter() {
			public boolean select(IDiffNode node) {
				if (node instanceof IThreeWayDiff) {
					IThreeWayDiff twd = (IThreeWayDiff) node;
					int direction = twd.getDirection();
					if (direction == IThreeWayDiff.OUTGOING || direction == IThreeWayDiff.CONFLICTING) {
						return true;
					}
				} else {
					// Return true for any two-way change
					return true;
				}
				return false;
			}
		});
	}
}
