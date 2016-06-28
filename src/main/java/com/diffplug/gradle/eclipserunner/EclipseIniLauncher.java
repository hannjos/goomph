/*
 * Copyright 2016 DiffPlug
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.diffplug.gradle.eclipserunner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Objects;

import org.eclipse.core.runtime.adaptor.EclipseStarter;
import org.osgi.framework.BundleContext;

import com.diffplug.common.base.Box;
import com.diffplug.common.base.Preconditions;
import com.diffplug.gradle.eclipserunner.launcher.Main;

/**
 * Given a directory containing osgi jars, this class
 * verifies that the core bundles are available, and
 * provides an API for instantiating the OSGi runtime
 * and accessing its {@link BundleContext}.
 *
 * See {@link #setProps(Map)} for precise details on the
 * default framework properties.
 */
public class EclipseIniLauncher {
	final File installationRoot;
	final EclipseIni eclipseIni;

	/**
	 * Wraps a directory of jars in the launcher API, and
	 * ensures the the directory contains the plugins required
	 * to run a barebones equinox instance.
	 */
	public EclipseIniLauncher(File installationRoot) throws FileNotFoundException, IOException {
		this.installationRoot = Objects.requireNonNull(installationRoot);
		// populate the plugins
		eclipseIni = EclipseIni.parseFrom(new File(installationRoot, "eclipse.ini"));
	}

	/**
	 * Opens the eclipse runtime, and returns an instance of
	 * {@link Running} which allows access to the underlying
	 * {@link BundleContext}.
	 */
	public Running open() throws Exception {
		return new Running();
	}

	/** Runs the equinox launcher (calls {@link #open()} and immediately closes it). */
	public void run() throws Exception {
		try (Running running = open()) {
			running.run();
		}
	}

	/**
	 * Represents a running instance of the equinox
	 * OSGi container.  Shuts down the container when
	 * you call {@link #close()}.
	 */
	public class Running implements AutoCloseable {
		final BundleContext bundleContext;

		private Running() throws Exception {
			Box.Nullable<BundleContext> context = Box.Nullable.ofNull();
			Main main = new Main() {
				@Override
				protected void invokeFramework(String[] passThruArgs, URL[] bootPath) throws Exception {
					context.set(EclipseStarter.startup(passThruArgs, splashHandler));
				}
			};
			main.basicRun(eclipseIni.getLinesAsArray());
			this.bundleContext = Objects.requireNonNull(context.get());
		}

		/** The {@link BundleContext} of the running eclipse instance. */
		public BundleContext bundleContext() {
			return bundleContext;
		}

		/** Runs an eclipse application, as specified by the `-application` argument. */
		private void run() throws Exception {
			Object result = EclipseStarter.run(null);
			Preconditions.checkState(Integer.valueOf(0).equals(result), "Unexpected return=0, was: %s", result);
		}

		/** Shutsdown the eclipse instance. */
		@Override
		public void close() throws Exception {
			EclipseStarter.shutdown();
		}
	}

	public static void main(String[] args) throws Exception {
		EclipseIniLauncher launcher = new EclipseIniLauncher(new File("C:\\Users\\ntwigg\\Documents\\DiffPlugDev\\talk-gradle_and_eclipse_rcp\\build\\oomph-ide"));
		try (EclipseIniLauncher.Running running = launcher.open()) {
			System.out.println("bundleContext=" + running.bundleContext());
		}
	}
}