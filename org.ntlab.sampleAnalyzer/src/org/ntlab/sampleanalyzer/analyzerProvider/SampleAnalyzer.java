package org.ntlab.sampleanalyzer.analyzerProvider;

import java.util.List;

import org.ntlab.traceAnalysisPlatform.tracer.trace.MethodExecution;

public class SampleAnalyzer {
	public static int countMethodExecution(List<MethodExecution> methodExecutions, 
			String targetSignature, int count, String indent) {
		if (methodExecutions == null || methodExecutions.isEmpty()) {
			return count;
		}
		for (int i = 0; i < methodExecutions.size(); i++) {
			MethodExecution me = methodExecutions.get(i);
			String signature = me.getSignature();
//			System.out.println(indent + signature);
			if (targetSignature.equals(signature)) {
				count++;
			}
			List<MethodExecution> children = me.getChildren();
			count = countMethodExecution(children, targetSignature, count, indent + "--------");
		}
		return count;
	}
}
