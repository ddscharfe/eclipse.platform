/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.core.mapping;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.diff.*;
import org.eclipse.team.core.diff.provider.ThreeWayDiff;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.history.provider.FileRevision;
import org.eclipse.team.core.mapping.IResourceDiff;
import org.eclipse.team.core.mapping.provider.ResourceDiff;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantComparator;

/**
 * Covert a SyncInfo into a IDiff
 */
public class SyncInfoToDiffConverter {

	public static final class ResourceVariantFileRevision extends FileRevision {
		private final IResourceVariant variant;

		private ResourceVariantFileRevision(IResourceVariant variant) {
			this.variant = variant;
		}

		public IStorage getStorage(IProgressMonitor monitor) throws CoreException {
			return variant.getStorage(monitor);
		}

		public String getName() {
			return variant.getName();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.team.internal.core.FileRevision#getContentIndentifier()
		 */
		public String getContentIdentifier() {
			return variant.getContentIdentifier();
		}

		public IResourceVariant getVariant() {
			return variant;
		}

		public boolean isPropertyMissing() {
			return false;
		}

		public IFileRevision withAllProperties(IProgressMonitor monitor) throws CoreException {
			return this;
		}
	}

	private static class PrecalculatedSyncInfo extends SyncInfo {
		public int kind;
		public PrecalculatedSyncInfo(int kind, IResource local, IResourceVariant base, IResourceVariant remote, IResourceVariantComparator comparator) {
			super(local, base, remote, comparator);
			this.kind = kind;
		}

		protected int calculateKind() throws TeamException {
			return kind;
		}
	}
	
	public static int asDiffFlags(int syncInfoFlags) {
		if (syncInfoFlags == SyncInfo.IN_SYNC)
			return IDiff.NO_CHANGE;
		int kind = SyncInfo.getChange(syncInfoFlags);
		int diffFlags = 0;
		switch (kind) {
		case SyncInfo.ADDITION:
			diffFlags = IDiff.ADD;
			break;
		case SyncInfo.DELETION:
			diffFlags = IDiff.REMOVE;
			break;
		case SyncInfo.CHANGE:
			diffFlags = IDiff.CHANGE;
			break;
		}
		int direction = SyncInfo.getDirection(syncInfoFlags);
		switch (direction) {
		case SyncInfo.INCOMING:
			diffFlags |= IThreeWayDiff.INCOMING;
			break;
		case SyncInfo.OUTGOING:
			diffFlags |= IThreeWayDiff.OUTGOING;
			break;
		case SyncInfo.CONFLICTING:
			diffFlags |= IThreeWayDiff.CONFLICTING;
			break;
		}
		return diffFlags;
	}
	
	private static int asSyncInfoKind(IThreeWayDiff diff) {
		int kind = diff.getKind();
		if (diff.getKind() == IDiff.NO_CHANGE)
			return SyncInfo.IN_SYNC;
		int syncKind = 0;
		switch (kind) {
		case IDiff.ADD:
			syncKind = SyncInfo.ADDITION;
			break;
		case IDiff.REMOVE:
			syncKind = SyncInfo.DELETION;
			break;
		case IDiff.CHANGE:
			syncKind = SyncInfo.CHANGE;
			break;
		}
		int direction = diff.getDirection();
		switch (direction) {
		case IThreeWayDiff.INCOMING:
			syncKind |= SyncInfo.INCOMING;
			break;
		case IThreeWayDiff.OUTGOING:
			syncKind |= SyncInfo.OUTGOING;
			break;
		case IThreeWayDiff.CONFLICTING:
			syncKind |= SyncInfo.CONFLICTING;
			break;
		}
		return syncKind;
	}
	
	public static IDiff getDeltaFor(SyncInfo info) {
		if (info.getComparator().isThreeWay()) {
			ITwoWayDiff local = getLocalDelta(info);
			ITwoWayDiff remote = getRemoteDelta(info);
			return new ThreeWayDiff(local, remote);
		} else {
			if (info.getKind() != SyncInfo.IN_SYNC) {
				IResourceVariant remote = info.getRemote();
				IResource local = info.getLocal();
				int kind;
				if (remote == null) {
					kind = IDiff.REMOVE;
				} else if (!local.exists()) {
					kind = IDiff.ADD;
				} else {
					kind = IDiff.CHANGE;
				}
				if (local.getType() == IResource.FILE) {
					IFileRevision after = asFileState(remote);
					IFileRevision before = FileRevision.getFileRevisionFor((IFile)local);
					return new ResourceDiff(info.getLocal(), kind, 0, before, after);
				}
				// For folders, we don't need file states
				return new ResourceDiff(info.getLocal(), kind);
			}
			return null;
		}
	}

	private static ITwoWayDiff getRemoteDelta(SyncInfo info) {
		int direction = SyncInfo.getDirection(info.getKind());
		if (direction == SyncInfo.INCOMING || direction == SyncInfo.CONFLICTING) {
			IResourceVariant ancestor = info.getBase();
			IResourceVariant remote = info.getRemote();
			int kind;
			if (ancestor == null) {
				kind = IDiff.ADD;
			} else if (remote == null) {
				kind = IDiff.REMOVE;
			} else {
				kind = IDiff.CHANGE;
			}
			// For folders, we don't need file states
			if (info.getLocal().getType() == IResource.FILE) {
				IFileRevision before = asFileState(ancestor);
				IFileRevision after = asFileState(remote);
				return new ResourceDiff(info.getLocal(), kind, 0, before, after);
			}

			return new ResourceDiff(info.getLocal(), kind);
		}
		return null;
	}

	private static IFileRevision asFileState(final IResourceVariant variant) {
		if (variant == null)
			return null;
		return new ResourceVariantFileRevision(variant);
	}

	private static ITwoWayDiff getLocalDelta(SyncInfo info) {
		int direction = SyncInfo.getDirection(info.getKind());
		if (direction == SyncInfo.OUTGOING || direction == SyncInfo.CONFLICTING) {
			IResourceVariant ancestor = info.getBase();
			IResource local = info.getLocal();
			int kind;
			if (ancestor == null) {
				kind = IDiff.ADD;
			} else if (!local.exists()) {
				kind = IDiff.REMOVE;
			} else {
				kind = IDiff.CHANGE;
			}
			if (local.getType() == IResource.FILE) {
				IFileRevision before = asFileState(ancestor);
				IFileRevision after = FileRevision.getFileRevisionFor((IFile)local);
				return new ResourceDiff(info.getLocal(), kind, 0, before, after);
			}
			// For folders, we don't need file states
			return new ResourceDiff(info.getLocal(), kind);
			
		}
		return null;
	}

	public static IResourceVariant getRemoteVariant(IThreeWayDiff twd) {
		IResourceDiff diff = (IResourceDiff)twd.getRemoteChange();
		if (diff != null)
			return asResourceVariant(diff.getAfterState());
		diff = (IResourceDiff)twd.getLocalChange();
		if (diff != null)
			return asResourceVariant(diff.getBeforeState());
		return null;
	}

	public static IResourceVariant getBaseVariant(IThreeWayDiff twd) {
		IResourceDiff diff = (IResourceDiff)twd.getRemoteChange();
		if (diff != null)
			return asResourceVariant(diff.getBeforeState());
		diff = (IResourceDiff)twd.getLocalChange();
		if (diff != null)
			return asResourceVariant(diff.getBeforeState());
		return null;
	}
	
	public static SyncInfo asSyncInfo(IDiff diff, IResourceVariantComparator comparator) {
		if (diff instanceof ResourceDiff) {
			ResourceDiff rd = (ResourceDiff) diff;
			IResource local = rd.getResource();
			IFileRevision afterState = rd.getAfterState();
			IResourceVariant remote = asResourceVariant(afterState);
			int kind;
			if (remote == null) {
				kind = SyncInfo.DELETION;
			} else if (!local.exists()) {
				kind = SyncInfo.ADDITION;
			} else {
				kind = SyncInfo.CHANGE;
			}
			PrecalculatedSyncInfo info = new PrecalculatedSyncInfo(kind, local, null, remote, comparator);
			try {
				info.init();
			} catch (TeamException e) {
				// Ignore
			}
			return info;
		} else if (diff instanceof IThreeWayDiff) {
			IThreeWayDiff twd = (IThreeWayDiff) diff;
			IResource local = getLocal(twd);
			if (local != null) {
				IResourceVariant remote = getRemoteVariant(twd);
				IResourceVariant base = getBaseVariant(twd);
				int kind = asSyncInfoKind(twd);
				PrecalculatedSyncInfo info = new PrecalculatedSyncInfo(kind, local, base, remote, comparator);
				try {
					info.init();
				} catch (TeamException e) {
					// Ignore
				}
				return info;
			}
		}
		return null;
	}

	private static IResource getLocal(IThreeWayDiff twd) {
		IResourceDiff diff = (IResourceDiff)twd.getRemoteChange();
		if (diff != null)
			return diff.getResource();
		diff = (IResourceDiff)twd.getLocalChange();
		if (diff != null)
			return diff.getResource();
		return null;
	}

	public static IResourceVariant asResourceVariant(IFileRevision revision) {
		if (revision == null)
			return null;
		if (revision instanceof ResourceVariantFileRevision) {
			ResourceVariantFileRevision rvfr = (ResourceVariantFileRevision) revision;
			return rvfr.getVariant();
		}
		if (revision instanceof IAdaptable) {
			IAdaptable adaptable = (IAdaptable) revision;
			Object o = adaptable.getAdapter(IResourceVariant.class);
			if (o instanceof IResourceVariant) {
				return (IResourceVariant) o;
			}
		}
		return null;
	}
}
