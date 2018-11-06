package org.ntlab.sampleanalyzer.analyzerProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.FileLocator;
import org.ntlab.traceAnalysisPlatform.IAdditionalLaunchConfiguration;

public class SampleAnalyzerLaunchConfiguration implements IAdditionalLaunchConfiguration {
	public static final String ANALYZER_PATH = "org/ntlab/sampleanalyzer/analyzerProvider/SampleAnalyzer.class";
	public static final String ANALYZER_PACKAGE = "org.ntlab.sampleanalyzer.analyzerProvider";
	public static final String ANALYZER_CLASS = "SampleAnalyzer";
	public static final String TRACE = "org.ntlab.traceAnalysisPlatform.tracer.trace";
	
	@Override
	public String[] getAdditionalClasspath() {
		try {
			List<String> classPathList = new ArrayList<>();
			String analyzerClassPath = FileLocator.resolve(this.getClass().getClassLoader().getResource(ANALYZER_PATH)).getPath();
			String classPath = analyzerClassPath.substring(0, analyzerClassPath.length() - ANALYZER_PATH.length());
			classPathList.add(classPath);
			return classPathList.toArray(new String[classPathList.size()]);
		} catch (IOException e) {
			e.printStackTrace();
		}
		throw new IllegalStateException();
	}
}
