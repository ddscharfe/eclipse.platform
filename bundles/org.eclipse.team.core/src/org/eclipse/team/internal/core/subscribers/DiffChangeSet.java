/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.core.subscribers;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.mapping.IResourceDiffTree;
import org.eclipse.team.core.mapping.provider.ResourceDiffTree;

public class DiffChangeSet extends ChangeSet {

	private final ResourceDiffTree tree = new ResourceDiffTree();
    
    public DiffChangeSet() {
		super();
	}
    
    public DiffChangeSet(String name) {
    	super(name);
    }
    
    /**
     * Return the diff tree that contains the resources that belong to this change set.
     * @return  the diff tree that contains the resources that belong to this change set
     */
    public IResourceDiffTree getDiffTree() {
        return tree;
    }
    
    protected ResourceDiffTree internalGetDiffTree() {
        return tree;
    }

    /**
     * Return the resources that are contained in this set.
     * @return the resources that are contained in this set
     */
    public IResource[] getResources() {
        return tree.getAffectedResources();
    }
    
    /**
     * Return whether the set contains any files.
     * @return whether the set contains any files
     */
    public boolean isEmpty() {
        return tree.isEmpty();
    }

    /**
     * Return true if the given file is included in this set.
     * @param local a local file
     * @return true if the given file is included in this set
     */
    public boolean contains(IResource local) {
        return tree.getDiff(local) != null;
    }
    
    /**
     * Add the resource to this set if it is modified
     * w.r.t. the subscriber.
     * @param diff the diff to be added
     */
    public void add(IDiff diff) {
        if (isValidChange(diff)) {
            tree.add(diff);
        }
    }
    
    /**
     * Return whether the given sync info is a valid change
     * and can be included in this set. This method is used
     * by the <code>add</code> method to filter set additions.
     * @param diff a diff
     * @return whether the sync info is a valid member of this set
     */
    protected boolean isValidChange(IDiff diff) {
        return (diff != null);
    }

    /**
     * Add the resources to this set if they are modified
     * w.r.t. the subscriber.
     * @param diffs the resources to be added.
     */
    public void add(IDiff[] diffs) {
       try {
           tree.beginInput();
           for (int i = 0; i < diffs.length; i++) {
              IDiff diff = diffs[i];
              add(diff);
           }
       } finally {
           tree.endInput(null);
       }
    }
    
    /**
     * Remove the resource from the set.
     * @param resource the resource to be removed
     */
    public void remove(IResource resource) {
        if (contains(resource)) {
            tree.remove(resource);
        }
    }
    
    /**
     * Remove the resource and it's descendants to the given depth.
     * @param resource the resource to be removed
     * @param depth the depth of the removal (one of
     * <code>IResource.DEPTH_ZERO, IResource.DEPTH_ONE, IResource.DEPTH_INFINITE)</code>
     */
    public void rootRemoved(IResource resource, int depth) {
    	IDiff[] diffs = tree.getDiffs(resource, depth);
    	if (diffs.length > 0) {
    		try {
    			tree.beginInput();
    			for (int i = 0; i < diffs.length; i++) {
					IDiff diff = diffs[i];
					IResource r = tree.getResource(diff);
					if (r != null)
						tree.remove(r);
				}
    		} finally {
    			tree.endInput(null);
    		}
    	}
    }
    
	public boolean contains(IPath path) {
		return getDiffTree().getDiff(path) != null;
	}
	
	public boolean containsChildren(IResource resource, int depth) {
		return getDiffTree().getDiffs(resource, depth).length > 0;
	}

	public void remove(IPath[] paths) {
		try {
			tree.beginInput();
			for (int i = 0; i < paths.length; i++) {
				IPath path = paths[i];
				tree.remove(path);
			}
		} finally {
			tree.endInput(null);
		}
	}
	
	public void remove(IResource[] resources) {
		try {
			tree.beginInput();
			for (int i = 0; i < resources.length; i++) {
				IResource resource = resources[i];
				tree.remove(resource);
			}
		} finally {
			tree.endInput(null);
		}
	}
	
	public String getComment() {
		return null;
	}

}
