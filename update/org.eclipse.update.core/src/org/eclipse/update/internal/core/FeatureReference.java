package org.eclipse.update.internal.core;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.*;
import org.eclipse.update.core.*;
import org.eclipse.update.core.model.*;
import org.eclipse.update.core.model.FeatureReferenceModel;
import org.eclipse.update.core.model.SiteModel;

/**
 *
 * 
 */

public class FeatureReference
	extends FeatureReferenceModel
	implements IFeatureReference, IWritable {

	private IFeature feature;

	/**
	 * category : delegate to teh site
	 */
	private List categories;
	
		

	/**
	 * Constructor
	 */
	public FeatureReference() {
		super();
	}

	/**
	 * Returns the array of categories the feature belong to.
	 * 
	 * The categories are declared in the <code>site.xml</code> file.
	 * 
	 * @see ICategory
	 * @return the array of categories this feature belong to. Returns an empty array
	 * if there are no cateopries.
	 */
	public ICategory[] getCategories() {

		if (categories == null) {
			categories = new ArrayList();
			String[] categoriesAsString = getCategoryNames();
			for (int i = 0; i < categoriesAsString.length; i++) {
				categories.add(getSite().getCategory(categoriesAsString[i]));
			}
		}

		ICategory[] result = new ICategory[0];

		if (!(categories == null || categories.isEmpty())) {
			result = new ICategory[categories.size()];
			categories.toArray(result);
		}
		return result;
	}

	/**
	 * Returns the feature this reference points to
	 *  @return the feature on the Site
	 */
	public IFeature getFeature() throws CoreException {

		String type = getType();
		if (feature == null) {

			if (type == null || type.equals("")) {
				// ask the Site for the default type 
				type = getSite().getDefaultPackagedFeatureType();
			}
			
			feature = createFeature(type, getURL(), getSite());
		}

		return feature;
	}

	/*
	 * @see IFeatureReference#addCategory(ICategory)
	 */
	public void addCategory(ICategory category) {
		this.addCategoryName(category.getName());
	}

	/*
	 * @see IWritable#write(int, PrintWriter)
	 */
	public void write(int indent, PrintWriter w) {
		String gap = "";
		for (int i = 0; i < indent; i++)
			gap += " ";
		String increment = "";
		for (int i = 0; i < IWritable.INDENT; i++)
			increment += " ";

		w.print(gap + "<feature ");
		// feature type
		if (getType() != null) {
			w.print("type=\"" + Writer.xmlSafe(getType() + "\""));
			w.print(" ");
		}

		// feature URL
		String URLInfoString = null;
		if (getURL() != null) {
			URLInfoString = UpdateManagerUtils.getURLAsString(getSite().getURL(), getURL());
			w.print("url=\"" + Writer.xmlSafe(URLInfoString) + "\"");
		}
		w.println(">");

		String[] categoryNames = getCategoryNames();
		for (int i = 0; i < categoryNames.length; i++) {
			String element = categoryNames[i];
			w.println(
				gap + increment + "<category name=\"" + Writer.xmlSafe(element) + "\"/>");
		}
		w.println("</feature>");
	}

	/**
	 * create an instance of a class that implements IFeature
	 */
	private IFeature createFeature(String featureType, URL url, ISite site)
		throws CoreException {
		IFeature feature = null;
		IFeatureFactory factory =
			FeatureTypeFactory.getInstance().getFactory(featureType);
		feature = factory.createFeature(url, site);
		return feature;
	}

	/*
	 * @see IFeatureReference#setURL(URL)
	 */
	public void setURL(URL url) throws CoreException {
		if (url != null) {
			setURLString(url.toExternalForm());
			try {
				resolve(url, null);
			} catch (MalformedURLException e) {
				String id =
					UpdateManagerPlugin.getPlugin().getDescriptor().getUniqueIdentifier();
				IStatus status =
					new Status(
						IStatus.WARNING,
						id,
						IStatus.OK,
						"Cannot resolve URL:" + url.toExternalForm(),
						e);
				throw new CoreException(status);
			}
		}
	}

	/*
	 * @see IFeatureReference#getSite()
	 */
	public ISite getSite() {
		return (ISite) getSiteModel();
	}

	/*
	 * @see IFeatureReference#setSite(ISite)
	 */
	public void setSite(ISite site) {
		setSiteModel((SiteModel) site);
	}

	
	


	private CoreException newCoreException(String s, Throwable e) throws CoreException {
		return new CoreException(new Status(IStatus.ERROR,"org.eclipse.update.examples.buildzip",0,s,e));
	}

}