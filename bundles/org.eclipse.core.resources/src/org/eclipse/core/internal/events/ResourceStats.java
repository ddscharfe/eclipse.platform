/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.events;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.PerformanceStats;

/**
 * An ResourceStats collects and aggregates timing data about an event such as
 * a builder running, an editor opening, etc.
 */
public class ResourceStats {
	private static final String PERF_LISTENERS = ResourcesPlugin.PI_RESOURCES + "/perf/listeners"; //$NON-NLS-1$
	private static final String PERF_BUILDERS = ResourcesPlugin.PI_RESOURCES + "/perf/builders"; //$NON-NLS-1$
	private static final String PERF_SNAPSHOT = ResourcesPlugin.PI_RESOURCES + "/perf/snapshot"; //$NON-NLS-1$
	/**
	 * The start time of the current build or notification
	 */
	private static long currentStart;

	/**
	 * The event that is currently occurring, maybe <code>null</code>
	 */
	private static PerformanceStats currentStats;
	
	/**
	 * Context information for the event that is occurring.
	 */
	private static String currentContext;

	public static void endBuild() {
		long end = System.currentTimeMillis();
		if (currentStart > 0 && currentStats != null)
			currentStats.addRun(end - currentStart, currentContext);
		currentStats = null;
		currentStart = -1;
	}

	public static void endNotify() {
		long end = System.currentTimeMillis();
		if (currentStart > 0 && currentStats != null)
			currentStats.addRun(end - currentStart, currentContext);
		currentStats = null;
		currentStart = -1;
	}

	public static void endSnapshot() {
		long end = System.currentTimeMillis();
		if (currentStart > 0 && currentStats != null)
			currentStats.addRun(end - currentStart, currentContext);
		currentStats = null;
		currentStart = -1;
	}

	/**
	 * Notifies the stats tool that a resource change listener has been added.
	 */
	public static void listenerAdded(IResourceChangeListener listener) {
		if (listener != null)
			PerformanceStats.getStats(PERF_LISTENERS, listener.getClass().getName());
	}

	/**
	 * Notifies the stats tool that a resource change listener has been removed.
	 */
	public static void listenerRemoved(IResourceChangeListener listener) {
		if (listener != null)
			PerformanceStats.removeStats(PERF_LISTENERS, listener.getClass().getName());
	}

	public static void startBuild(IncrementalProjectBuilder builder) {
		currentStats = PerformanceStats.getStats(PERF_BUILDERS, builder);
		currentContext = builder.getProject().getName();
		currentStart = System.currentTimeMillis();
	}

	public static void startNotify(IResourceChangeListener listener) {
		currentStats = PerformanceStats.getStats(PERF_LISTENERS, listener);
		currentContext = null;
		currentStart = System.currentTimeMillis();
	}

	public static void startSnapshot() {
		currentStats = PerformanceStats.getStats(PERF_SNAPSHOT, ResourcesPlugin.getWorkspace());
		currentContext = null;
		currentStart = System.currentTimeMillis();
	}
}