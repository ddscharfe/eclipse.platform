package org.eclipse.team.internal.ccvs.core.resources;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.Date;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.Policy;
import org.eclipse.team.internal.ccvs.core.util.Assert;
import org.eclipse.team.internal.ccvs.core.util.EntryFileDateFormat;
import org.eclipse.team.internal.ccvs.core.util.Util;

/**
 * Represents handles to CVS resource on the local file system. Synchronization
 * information is taken from the CVS subdirectories. 
 * 
 * @see LocalFolder
 * @see LocalFile
 */
public class LocalFile extends LocalResource implements ICVSFile {

	/**
	 * Constants for file transfer transformations to the CVS server.
	 */
	protected static final String PLATFORM_NEWLINE = System.getProperty("line.separator");
	protected static final String SERVER_NEWLINE = "\n";
	
	protected static final byte[] PLATFORM_NEWBYTE = PLATFORM_NEWLINE.getBytes();
	protected static final byte[] SERVER_NEWBYTE = SERVER_NEWLINE.getBytes();

	
	/**
	 * Create a handle based on the given local resource.
	 */
	public LocalFile(File file) {
		super(file);
	}

	public long getSize() {
		return ioResource.length();
	}

	public void receiveFrom(InputStream in, long size, boolean binary, boolean readOnly, IProgressMonitor monitor) throws CVSException {
		OutputStream out;
		String title;

		title = Policy.bind("LocalFile.receiving", ioResource.getName());

		try {
			// We don't need to buffer here because the methods used below do
			out = new FileOutputStream(ioResource);

			try {
				if (binary) {
					transferWithProgress(in, out, size, title, monitor);
				} else {
					transferText(in, out, size, title, false, monitor);
				}
			} finally {
				out.close();
			}

			if (readOnly) {
				ioResource.setReadOnly();
			}
		} catch (IOException e) {
			throw CVSException.wrapException(e);
		}
	}
	
	public void sendTo(
		OutputStream out,
		boolean binary,
		IProgressMonitor monitor)
		throws CVSException {
		
		InputStream in;
		String title;
		long size = getSize();
		title = Policy.bind("LocalFile.sending",
							new Object[]{ioResource.getName()});
		
		try {
			// We don't need to buffer here because the methods used below do
			in = new FileInputStream(ioResource);
			
			try {
				if (binary) {
					
					// Send the size to the server
					out.write(("" + getSize()).getBytes());
					out.write(SERVER_NEWLINE.getBytes());
					transferWithProgress(in,out,size,title,monitor);
				} else {
					
					// In this case the size has to be computed.
					// Therefore we do send the size in transferText
					transferText(in,out,getSize(),title,true,monitor);
				}
			} finally {
				in.close();
			}
			
		} catch (IOException e) {
			throw CVSException.wrapException(e);
		}	
	}	

	public String getTimeStamp() throws CVSFileNotFoundException {						
		EntryFileDateFormat timestamp = new EntryFileDateFormat();		
		return timestamp.format(ioResource.lastModified());
	}
 
	public void setTimeStamp(String date) throws CVSException {
		long millSec;		
		if (date==null) {
			// get the current time
			millSec = new Date().getTime();
		} else {
			try {
				EntryFileDateFormat timestamp = new EntryFileDateFormat();
				millSec = timestamp.toMilliseconds(date);
			} catch (ParseException e) {
				throw new CVSException(0,0,"Format of the Date for a TimeStamp not parseable",e);
			}
		}		
		ioResource.setLastModified(millSec);
	}

	public boolean isFolder() {
		return false;
	}
	
	protected static void transferText(InputStream in, OutputStream out, long size, String title, boolean toServer, IProgressMonitor monitor) throws IOException {

		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		transferWithProgress(in, buffer, size, title, monitor);

		byte[] contents = null;
		boolean needEOLConversion = !PLATFORM_NEWLINE.equals(SERVER_NEWLINE);
		if (toServer) {
			if (needEOLConversion) {
				contents = Util.replace(buffer.toByteArray(), PLATFORM_NEWBYTE, SERVER_NEWBYTE);
			} else {
				contents = buffer.toByteArray();
			}
			// Send the size to the server
			out.write(("" + contents.length).getBytes());
			out.write(SERVER_NEWLINE.getBytes());
		} else {
			if (needEOLConversion) {
				contents = Util.replace(buffer.toByteArray(), PLATFORM_NEWBYTE, SERVER_NEWBYTE);
				contents = Util.replace(contents, SERVER_NEWBYTE, PLATFORM_NEWBYTE);
			} else {
				contents = buffer.toByteArray();
			}
		}
		out.write(contents);
	}
		
	protected static void transferWithProgress(InputStream in, OutputStream out, long size, String title, IProgressMonitor monitor) throws IOException {

		byte[] BUFFER = new byte[4096];

		// This special transfer utility will show progress to
		// the monitor for files that are bigger than 25K
		boolean progress = size > 25000;
		int read = 0;
		long totalRead = 0;
		long ksize = size / 1024;

		monitor.subTask(Policy.bind("LocalFile.transferNoSize", title));

		// buffer size is smaller than MAXINT...
		int toRead = (int) Math.min(BUFFER.length, size);
		synchronized (BUFFER) {
			while ((totalRead < size) && (read = in.read(BUFFER, 0, toRead)) != -1) {
				if (progress && totalRead > 0) {
					monitor.subTask(Policy.bind("LocalFile.transfer", new Object[] { title, new Long(totalRead / 1024), new Long(ksize)}));
				}
				totalRead += read;
				out.write(BUFFER, 0, read);
				toRead = (int) Math.min(BUFFER.length, size - totalRead);
			}
		}
	}
	
	public boolean isDirty() throws CVSException {
		// XXX Is the below correct for isDirty?
		if (!exists() || !isManaged()) {
			return true;
		} else {
			ResourceSyncInfo info = getSyncInfo();
			if (info.isAdded()) return false;
			if (info.isDeleted()) return true;
			return !getTimeStamp().equals(info.getTimeStamp());
		}
	}

	public boolean isModified() throws CVSException {
		if (!exists() || !isManaged()) {
			return true;
		} else {
			ResourceSyncInfo info = getSyncInfo();
			return !getTimeStamp().equals(info.getTimeStamp());
		}
	}
	
	public void accept(ICVSResourceVisitor visitor) throws CVSException {
		visitor.visitFile(this);
	}

	/*
	 * This is to be used by the Copy handler. The filename of the form .#filename
	 */
	public void moveTo(String filename) throws CVSException {
		
		// Move the file to newFile (we know we do not need the
		// original any more anyway)
		// If this file exists then overwrite it
		LocalFile file;
		try {
			file = (LocalFile)getParent().getFile(filename);
		} catch(ClassCastException e) {
			throw CVSException.wrapException(e);
		}
		
		// We are deleting the old .#filename if it exists
		if (file.exists()) {
			file.delete();
		}
		
		boolean success = ioResource.renameTo(file.getFile());
		
		if (!success) {
			throw new CVSException("Move from " + ioResource + " to " + file + " was not possible");
		}
	}

	File getFile() {
		return ioResource;
	}

	/**
	 * @see ICVSResource#getRemoteLocation()
	 */
	public String getRemoteLocation(ICVSFolder stopSearching) throws CVSException {
		return getParent().getRemoteLocation(stopSearching) + SEPARATOR + getName();
	}	
	/*
	 * @see ICVSResource#unmanage()
	 */
	public void unmanage() throws CVSException {
		Synchronizer.getInstance().deleteResourceSync(ioResource);
	}
}

