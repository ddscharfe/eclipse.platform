/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal.patch;

import java.io.*;
import java.util.List;

import org.eclipse.compare.*;
import org.eclipse.compare.internal.CompareUIPlugin;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.swt.graphics.Image;

public class PatchFileTypedElement implements ITypedElement, IEncodedStreamContentAccessor {

	private final FileDiffResult result;
	private final boolean isAfterState;

	public PatchFileTypedElement(FileDiffResult result, boolean isAfterState) {
		this.result = result;
		this.isAfterState = isAfterState;
	}

	public Image getImage() {
		IFile file = result.getPatcher().getTargetFile(result.getDiff());
		if (file == null) {
			// We don't get a target file if the file doesn't exist
			DiffProject project = result.getDiff().getProject();
			if (project != null) {
				file = project.getFile(result.getDiff().getPath(result.getPatcher().isReversed()));
			} else {
				IResource target = result.getPatcher().getTarget();
				if (target instanceof IFile) {
					file =  (IFile) target;
				} else if (target instanceof IContainer) {
					IContainer container = (IContainer) target;
					file = container.getFile(result.getDiff().getStrippedPath(result.getPatcher().getStripPrefixSegments(), result.getPatcher().isReversed()));
				}
			}
		}
		Image image = null;
		if (file != null) {
			image = CompareUI.getImage(file);
		}
		if (result.containsProblems()) {
			LocalResourceManager imageCache = PatcherCompareEditorInput.getImageCache(result.getPatcher());
			image = HunkTypedElement.getHunkErrorImage(image, imageCache, true);
		}
		return image;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.ITypedElement#getName()
	 */
	public String getName() {
		return result.getDiff().getStrippedPath(result.getPatcher().getStripPrefixSegments(), result.getPatcher().isReversed()).toString();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.ITypedElement#getType()
	 */
	public String getType() {
		return result.getPatcher().getPath(result.getDiff()).getFileExtension();
	}

	public String getCharset() throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	public InputStream getContents() throws CoreException {
		// If there are cached contents, use them
		if (isAfterState && result.getPatcher().hasCachedContents(result.getDiff()))
			return new ByteArrayInputStream(result.getPatcher().getCachedContents(result.getDiff()));
		// Otherwise, get the lines from the diff result
		List lines;
		if (isAfterState) {
			lines = result.getAfterLines();
		} else {
			lines = result.getBeforeLines();
		}
		String contents = result.getPatcher().createString(lines);
		String charSet = getCharset();
		byte[] bytes = null;
		if (charSet != null) {
			try {
				bytes = contents.getBytes(charSet);
			} catch (UnsupportedEncodingException e) {
				CompareUIPlugin.log(e);
			}
		}
		if (bytes == null) {
			bytes = contents.getBytes();
		}
		return new ByteArrayInputStream(bytes);
	}

}
