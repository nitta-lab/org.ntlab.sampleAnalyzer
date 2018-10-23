package org.ntlab.onlineAccessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.ClassType;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.InvocationException;
import com.sun.jdi.LongValue;
import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;

public class JDIStaticMethodCaller {
	protected VirtualMachine vm;
	protected ThreadReference thread;
	
	public JDIStaticMethodCaller(VirtualMachine vm, ThreadReference thread) {
		this.vm = vm;
		this.thread = thread;
	}

	public VirtualMachine getVm() {
		return vm;
	}

	public ThreadReference getThread() {
		return thread;
	}
	
	public long getThreadId() {
		ClassType type = (ClassType)thread.type();
		List<Method> methodsByName = type.methodsByName("getId");
		List<Value> argList = new ArrayList<>();
		try {
			return ((LongValue)thread.invokeMethod(thread, methodsByName.get(0), argList, thread.INVOKE_SINGLE_THREADED)).value();
		} catch (InvalidTypeException | ClassNotLoadedException
				| IncompatibleThreadStateException | InvocationException e) {
			e.printStackTrace();
		}
		throw new IllegalStateException();
	}

	/**
	 * パッケージ名とクラス名とメソッド名と引数を指定してそのクラスメソッドを呼び出す
	 * @param packageName 呼びたしたいメソッドがあるクラスのパッケージ名 
	 * @param className 呼び出したいメソッドがあるクラス名
	 * @param methodName 呼び出したいメソッド名 (static)
	 * @param args 呼び出したいメソッドに渡す引数(Value のクラス型で可変長)
	 * @return 呼び出したメソッドからの戻り値(Value)
	 */
	public Value callStaticMethod(String packageName, String className, String methodName, Value... args) 
			throws InvalidTypeException, ClassNotLoadedException, InvocationException, IncompatibleThreadStateException {
		String fqcn = packageName + "." + className;
		List<ReferenceType> classes = vm.classesByName(fqcn); // クラス名 (完全限定クラス名)
		
		// 当該クラスがターゲットVM上でロードされていない場合は, JDIを用いてターゲットJVM上でリフレクションによる当該クラスのロードを試行する
		if (classes.isEmpty()) {
			List<ReferenceType> list = vm.classesByName("java.lang.Class");
			ClassType type = (ClassType)list.get(0);
			List<Method> methodsByName = type.methodsByName("forName");
			List<Value> argList = new ArrayList<>();
			argList.add(vm.mirrorOf(fqcn));
			type.invokeMethod(thread, methodsByName.get(0), argList, thread.INVOKE_SINGLE_THREADED);
			classes = vm.classesByName(fqcn); // クラス名 (完全限定クラス名)
		}
		ClassType type = (ClassType)classes.get(0);
		List<Method> methodsByName = type.methodsByName(methodName);
		List<Value> argList = Arrays.asList(args); // メソッドに渡す引数のリスト
		return type.invokeMethod(thread, methodsByName.get(0), argList, thread.INVOKE_SINGLE_THREADED);	// デバッグ中のプログラム内のメソッドを呼び出す
	}
}
