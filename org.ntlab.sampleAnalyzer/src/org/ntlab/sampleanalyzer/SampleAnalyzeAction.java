package org.ntlab.sampleanalyzer;

import java.util.List;

import org.eclipse.jdt.core.Signature;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.ntlab.onlineAccessor.JDIDebuggingVirtualMachine;
import org.ntlab.onlineAccessor.JDIInstanceMethodCaller;
import org.ntlab.onlineAccessor.NotDebuggedException;
import org.ntlab.onlineAccessor.NotExecutedException;
import org.ntlab.onlineAccessor.NotSuspendedException;
import org.ntlab.sampleanalyzer.analyzerProvider.SampleAnalyzerLaunchConfiguration;

import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.InvocationException;
import com.sun.jdi.LongValue;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.StringReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;

@SuppressWarnings("restriction")
public class SampleAnalyzeAction implements IWorkbenchWindowActionDelegate {

	@Override
	public void run(IAction arg0) {
		try {
			VirtualMachine vm = JDIDebuggingVirtualMachine.getDebuggingVirtualMachine();
			List<ThreadReference> allThreads = vm.allThreads();
			ThreadReference suspendedThread = null;
			ThreadReference targetThread = null;
			for (int i = 0; i < allThreads.size(); i++) {
				ThreadReference thread = allThreads.get(i);
				if (thread.isSuspended()) {
					if (thread.name().equals("OnlineAnalysisThread")) {
						suspendedThread = thread;
					} else {
						targetThread = thread;
					}
				}
			}
			if (suspendedThread != null && targetThread != null) {
				countMethodExecutionTest(vm, suspendedThread, targetThread);
			}
		} catch (NotExecutedException | NotSuspendedException | NotDebuggedException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void selectionChanged(IAction arg0, ISelection arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

	@Override
	public void init(IWorkbenchWindow arg0) {
		// TODO Auto-generated method stub

	}

	private void countMethodExecutionTest(VirtualMachine vm, ThreadReference suspendedThread, ThreadReference targetThread) {
		JDIInstanceMethodCaller mc = new JDIInstanceMethodCaller(vm, suspendedThread, targetThread);
		try {
			long id = ((LongValue)mc.callInstanceMethod("getId")).value();
			StringReference threadId = mc.getVm().mirrorOf(String.valueOf(id));
			ObjectReference threadInstance = (ObjectReference)mc.callStaticMethod(SampleAnalyzerLaunchConfiguration.TRACE, "TraceJSON", "getThreadInstance", threadId);
			ObjectReference roots = (ObjectReference)mc.changeReceiver(threadInstance).callInstanceMethod("getRoot");

			Method method = targetThread.frame(0).location().method();
			String targetSignature = createMethodSignature(method);			
			test(vm, suspendedThread, roots, targetSignature, SpeedTestType.TARGET); // 一次解析を対象プログラム側で行う
//			test(vm, suspendedThread, roots, targetSignature, SpeedTestType.ANALYZER); // 一次解析をアナライザ側で行う
		} catch (InvalidTypeException | ClassNotLoadedException
				| InvocationException | IncompatibleThreadStateException e) {
			e.printStackTrace();
		}
	}

	private String createMethodSignature(Method method) {
		String fqcn = method.declaringType().name().replace("$", ".");
		String methodName = method.name();
		String signature = Signature.toString(method.signature());
		String[] signatureSplit = signature.split("\\s+", 2); // 戻り値の型と引数リスト(両端に括弧つき)に分割
		String returnType = signatureSplit[0];
		String args = signatureSplit[1].replace("/", ".").replace(" ", ""); // 引数のFQCNの区切りの置換と, 一つ一つの引数の間にあるスペースをなくす

		StringBuilder methodSignature = new StringBuilder();
		if (method.isPublic()) methodSignature.append("public ");
		else if (method.isPrivate()) methodSignature.append("private ");
		else if (method.isProtected()) methodSignature.append("protected ");
		if (method.isStatic()) methodSignature.append("static ");
		if (method.isFinal()) methodSignature.append("final ");
		if (method.isSynchronized()) methodSignature.append("synchronized ");

		if (!(method.isConstructor())) methodSignature.append(returnType + " ");
		methodSignature.append(fqcn);
		if (!(method.isConstructor())) methodSignature.append("." + methodName);
		methodSignature.append(args);
		return methodSignature.toString();
	}
	
	private void test(VirtualMachine vm, ThreadReference suspendedThread, ObjectReference roots, String targetSignature, SpeedTestType type)
			throws InvalidTypeException, ClassNotLoadedException, InvocationException, IncompatibleThreadStateException {
		int count = 0;
		long beforeTime = 0, afterTime = 0;
		JDIInstanceMethodCaller mc = new JDIInstanceMethodCaller(vm, suspendedThread, roots);
		switch (type) {
		case ANALYZER:
			beforeTime = System.nanoTime();				
			count = countMethodExecutionInAnalyzer(mc, targetSignature, 0, "--------");
			afterTime = System.nanoTime();
			break;
		case TARGET:
			beforeTime = System.nanoTime();
			count = countMethodExecutionInTarget(mc, targetSignature, 0, "--------");
			afterTime = System.nanoTime();
			break;
		}
		long executionTime = afterTime - beforeTime;
		StringBuilder result = new StringBuilder();
		String lineSeparator = System.lineSeparator();
		String title = "Sample Analyze Result";
		result.append(title);											result.append(lineSeparator);
		result.append("(executed within " + type.getTypeName() + ")");	result.append(lineSeparator);
		result.append(System.lineSeparator());
		result.append("signature: " + targetSignature);					result.append(lineSeparator);
		result.append("count: " + count);								result.append(lineSeparator);
		result.append("time: " + executionTime + " nsec");				result.append(lineSeparator);
		MessageDialog.openInformation(null, title, result.toString());
	}
	
	private int countMethodExecutionInTarget(JDIInstanceMethodCaller mc, String targetSignture, int count, String indent)
			throws InvalidTypeException, ClassNotLoadedException, InvocationException, IncompatibleThreadStateException {
		VirtualMachine vm = mc.getVm();
		String methodName = "countMethodExecution";
		return ((IntegerValue)mc.callStaticMethod(SampleAnalyzerLaunchConfiguration.ANALYZER_PACKAGE, SampleAnalyzerLaunchConfiguration.ANALYZER_CLASS, methodName, 
				mc.getReceiver(), vm.mirrorOf(targetSignture), vm.mirrorOf(count), vm.mirrorOf(indent))).value();
	}

	private int countMethodExecutionInAnalyzer(JDIInstanceMethodCaller mc, String targetSignature, int count, String indent)
			throws InvalidTypeException, ClassNotLoadedException, InvocationException, IncompatibleThreadStateException {
		VirtualMachine vm = mc.getVm();
		ObjectReference methodExecutions = mc.getReceiver();
		if (methodExecutions == null) {
			return count;
		}
		int methodExecutionsSize = ((IntegerValue)mc.callInstanceMethod("size")).value();
		if (methodExecutionsSize == 0) {
			return count;
		}
		for (int i = 0; i < methodExecutionsSize; i++) {
			IntegerValue index = vm.mirrorOf(i);
			ObjectReference methodExecution = (ObjectReference)mc.changeReceiver(methodExecutions).callInstanceMethod("get", index);
			String signature = ((StringReference)mc.changeReceiver(methodExecution).callInstanceMethod("getSignature")).value();
//			System.out.println(indent + signature);
			if (targetSignature.equals(signature)) {
				count++;
			}
			ObjectReference children = (ObjectReference)mc.callInstanceMethod("getChildren");
			count = countMethodExecutionInAnalyzer(mc.changeReceiver(children), targetSignature, count, indent + "--------");
		}
		return count;
	}

	private enum SpeedTestType {
		ANALYZER, TARGET;
		
		public String getTypeName() {
			switch (this) {
			case ANALYZER:
				return "analyzer";
			case TARGET:
				return "target";
			default:
				return "";
			}
		}
	}
}
