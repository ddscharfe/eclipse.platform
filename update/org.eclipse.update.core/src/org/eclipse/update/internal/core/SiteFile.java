package org.eclipse.update.internal.core;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.*;
import org.eclipse.update.core.*;
import org.eclipse.update.core.model.*;

/**
 * Site on the File System
 */
public class SiteFile extends Site {



	/**
	 * plugin entries 
	 */
	private List pluginEntries = new ArrayList(0);	

	/**
	 * 
	 */
	public ISiteContentConsumer createSiteContentConsumer(IFeature targetFeature) throws CoreException {
		SiteFileContentConsumer consumer = new SiteFileContentConsumer(targetFeature);
		consumer.setSite(this);
		return consumer;
	}
	
	/**
	 * @see ISite#getDefaultInstallableFeatureType()
	 */	
	public String getDefaultPackagedFeatureType() {
		return DEFAULT_INSTALLED_FEATURE_TYPE;
	}	
	
	/*
	 * @see ISite#install(IFeature, IProgressMonitor)
	 */	
	public IFeatureReference install(IFeature sourceFeature, IProgressMonitor progress) throws CoreException {

		if (sourceFeature==null) return null;

		// make sure we have an InstallMonitor		
		InstallMonitor monitor;
		if (progress == null)
			monitor = null;
		else if (progress instanceof InstallMonitor)
			monitor = (InstallMonitor) progress;
		else
			monitor = new InstallMonitor(progress);

		// create new executable feature and install source content into it
		IFeature localFeature = createExecutableFeature(sourceFeature);
		IFeatureReference localFeatureReference = sourceFeature.install(localFeature, monitor);
		if (localFeature instanceof FeatureModel)
			 ((FeatureModel) localFeature).markReadOnly();
		this.addFeatureReference(localFeatureReference);

		// add the installed plugins directories as archives entry
		SiteFileFactory archiveFactory = new SiteFileFactory();		
		ArchiveReferenceModel archive = archiveFactory.createArchiveReferenceModel();
		IPluginEntry[] pluginEntries = localFeatureReference.getFeature().getPluginEntries();
		for (int i = 0; i <pluginEntries.length; i++) {
			String pluginID = Site.DEFAULT_PLUGIN_PATH + pluginEntries[i].toString() + FeaturePackagedContentProvider.JAR_EXTENSION;
			archive.setPath(pluginID);
			try {
				URL url = new URL(getURL(), Site.DEFAULT_PLUGIN_PATH + pluginEntries[i].toString());
				archive.setURLString(url.toExternalForm());
				this.addArchiveReferenceModel(archive);
			} catch (MalformedURLException e){
				IStatus status = new Status(IStatus.ERROR,"org.eclipse.update.core",IStatus.OK,"Unable to URL for location:"+pluginEntries[i].toString(),e);
				throw new CoreException(status);
			}
		}
		return localFeatureReference;
	}

	/*
	 * @see ISite#remove(IFeature, IProgressMonitor)
	 */
	public void remove(IFeature feature, IProgressMonitor progress) throws CoreException {

		// make sure we have an InstallMonitor		
		InstallMonitor monitor;
		if (progress == null)
			monitor = null;
		else if (progress instanceof InstallMonitor)
			monitor = (InstallMonitor) progress;
		else
			monitor = new InstallMonitor(progress);

		// remove the feature and the plugins if they are not used and not activated
		// get the plugins from the feature
		IPluginEntry[] pluginsToRemove = getPluginEntriesOnlyReferencedBy(feature);

		//finds the contentReferences for this IPluginEntry
		for (int i = 0; i < pluginsToRemove.length; i++) {
			remove(feature, pluginsToRemove[i], monitor);
		}

		// remove the feature content
		ContentReference[] references = feature.getFeatureContentProvider().getFeatureEntryArchiveReferences(monitor);
		for (int i = 0; i < references.length; i++) {
			try {
				UpdateManagerUtils.removeFromFileSystem(references[i].asFile());
			} catch (IOException e) {
				String id = UpdateManagerPlugin.getPlugin().getDescriptor().getUniqueIdentifier();
				IStatus status = new Status(IStatus.ERROR, id, IStatus.OK, Policy.bind("SiteFile.CannotRemoveFeature", feature.getVersionedIdentifier().getIdentifier(), getURL().toExternalForm()), e); //$NON-NLS-1$
				throw new CoreException(status);
			}
		}

		// remove feature reference from the site
		IFeatureReference[] featureReferences = getFeatureReferences();
		if (featureReferences != null) {
			for (int indexRef = 0; indexRef < featureReferences.length; indexRef++) {
				IFeatureReference element = featureReferences[indexRef];
				if (element.equals(feature)) {
					removeFeatureReferenceModel((FeatureReferenceModel) element);
					break;
				}
			}
		}


	}

	/**
	 * returns the download size
	 * of the feature to be installed on the site.
	 * If the site is <code>null</code> returns the maximum size
	 * 
	 * If one plug-in entry has an unknown size.
	 * then the download size is unknown.
	 * 
	 * @see ISite#getDownloadSize(IFeature)
	 * 
	 */
	public long getDownloadSizeFor(IFeature feature) {
		long result = 0;
		IPluginEntry[] entriesToInstall = feature.getPluginEntries();
		IPluginEntry[] siteEntries = this.getPluginEntries();
		entriesToInstall = UpdateManagerUtils.diff(entriesToInstall, siteEntries);

		// FIXME Intersection for NonPluginEntry (using Install Handler)
		try {
			result = feature.getFeatureContentProvider().getDownloadSizeFor(entriesToInstall, /* non plugin entry []*/
			null);
		} catch (CoreException e) {
			UpdateManagerPlugin.getPlugin().getLog().log(e.getStatus());
			result = ContentEntryModel.UNKNOWN_SIZE;
		}
		return result;
	}

	/**
	 * returns the download size
	 * of the feature to be installed on the site.
	 * If the site is <code>null</code> returns the maximum size
	 * 
	 * If one plug-in entry has an unknown size.
	 * then the download size is unknown.
	 * 
	 * @see ISite#getDownloadSizeFor(IFeature)
	 * 
	 */
	public long getInstallSizeFor(IFeature feature) {
		long result = 0;
		IPluginEntry[] entriesToInstall = feature.getPluginEntries();
		IPluginEntry[] siteEntries = this.getPluginEntries();
		entriesToInstall = UpdateManagerUtils.diff(entriesToInstall, siteEntries);

		// FIXME Intersection for NonPluginEntry (using Install Handler)
		try {
			result = feature.getFeatureContentProvider().getInstallSizeFor(entriesToInstall, /* non plugin entry []*/
			null);
		} catch (CoreException e) {
			UpdateManagerPlugin.getPlugin().getLog().log(e.getStatus());
			result = ContentEntryModel.UNKNOWN_SIZE;
		}

		return result;
	}

	/**
	 * Adds a plugin entry 
	 * Either from parsing the file system or 
	 * installing a feature
	 * 
	 * We cannot figure out the list of plugins by reading the Site.xml as
	 * the archives tag are optionals
	 */
	public void addPluginEntry(IPluginEntry pluginEntry) {
		pluginEntries.add(pluginEntry);
	}

	/**
	 * @see IPluginContainer#getPluginEntries()
	 */
	public IPluginEntry[] getPluginEntries() {
		IPluginEntry[] result = new IPluginEntry[0];
		if (!(pluginEntries == null || pluginEntries.isEmpty())) {
			result = new IPluginEntry[pluginEntries.size()];
			pluginEntries.toArray(result);
		}
		return result;
	}
	
	/**
	 * @see IPluginContainer#getPluginEntryCount()
	 */
	public int getPluginEntryCount() {
		return getPluginEntries().length;
	}	

	/**
	 * 
	 */
	private IFeature createExecutableFeature(IFeature sourceFeature) throws CoreException {
		IFeature result = null;
		IFeatureFactory factory = FeatureTypeFactory.getInstance().getFactory(DEFAULT_INSTALLED_FEATURE_TYPE);
		result = factory.createFeature(/*URL*/null, this);

		// at least set the version identifier to be the same
		((FeatureModel) result).setFeatureIdentifier(sourceFeature.getVersionedIdentifier().getIdentifier());
		((FeatureModel) result).setFeatureVersion(sourceFeature.getVersionedIdentifier().getVersion().toString());
		return result;
	}

	/**
	 * adds a feature reference
	 * @param feature The feature reference to add
	 */
	private void addFeatureReference(IFeatureReference feature) {
		addFeatureReferenceModel((FeatureReferenceModel) feature);
	}
	
	/**
	 * 
	 */
	private void remove(IFeature feature, IPluginEntry pluginEntry, InstallMonitor monitor) throws CoreException {

		if (pluginEntry == null)
			return;

		ContentReference[] references = feature.getFeatureContentProvider().getPluginEntryArchiveReferences(pluginEntry, monitor);
		for (int i = 0; i < references.length; i++) {
			try {
				UpdateManagerUtils.removeFromFileSystem(references[i].asFile());
			} catch (IOException e) {
				String id = UpdateManagerPlugin.getPlugin().getDescriptor().getUniqueIdentifier();
				IStatus status = new Status(IStatus.ERROR, id, IStatus.OK, Policy.bind("SiteFile.CannotRemovePlugin", pluginEntry.getVersionedIdentifier().toString(), getURL().toExternalForm()), e); //$NON-NLS-1$
				throw new CoreException(status);
			}
		}
	}
		
}