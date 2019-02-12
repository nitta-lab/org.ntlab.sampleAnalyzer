package org.ntlab.sampleanalyzer.analyzerProvider;

import java.util.List;

import org.ntlab.traceAnalysisPlatform.tracer.trace.MethodExecution;
import org.ntlab.traceAnalysisPlatform.tracer.trace.ThreadInstance;
import org.ntlab.traceAnalysisPlatform.tracer.trace.TraceJSON;

public class SampleAnalyzer {
	private static int count = 0;
	private static long lastAnalysisTime = 0L;
	
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

	public static void resetStatus() {
		count = 0;
		lastAnalysisTime = 0L;
	}
	
	public static int countMethodExecuetionTraceless(String targetSignature, String threadId) { 
		ThreadInstance thread = TraceJSON.getThreadInstance(threadId);
		MethodExecution currentMe = thread.getCurrentMethodExecution();
		countMethodExecutionTraceless(thread.getRoot(), targetSignature, "--------");
		lastAnalysisTime = System.nanoTime();
		
		removeChildMethodInTrace(currentMe, currentMe.getChildren().size(), currentMe.getStatements().size());
		currentMe = currentMe.getParent();
		while (currentMe != null) {
			removeChildMethodInTrace(currentMe, currentMe.getChildren().size() - 1, currentMe.getStatements().size() - 1);
			currentMe = currentMe.getParent();
		}
		return count;
	}
	
	private static void countMethodExecutionTraceless(List<MethodExecution> methodExecutions, String targetSignature, String indent) {
		if (methodExecutions == null) return;
		for (int i = 0; i < methodExecutions.size(); i++) {
			MethodExecution me = methodExecutions.get(i);
			if (me == null) continue;
			String signature = me.getSignature();
			if (signature.equals(targetSignature) && me.getEntryTime() > lastAnalysisTime) {
				count++;
			}
			List<MethodExecution> children = me.getChildren();
			countMethodExecutionTraceless(children, targetSignature, (indent + "--------"));
		}
	}
	
	private static void removeChildMethodInTrace(MethodExecution me, int size, int statementSize) {
		for (int i = size - 1; i >= 0; i--) {
			// eme‚©‚çŽqme‚Ö‚ÌŽQÆ‚ðØ‚é
			me.getChildren().set(i, null);
		}
		for (int i = statementSize - 1; i >= 0; i--) {
			me.getStatements().set(i, null);
		}
	}
}
