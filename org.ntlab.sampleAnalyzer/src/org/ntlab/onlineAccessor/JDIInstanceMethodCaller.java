package org.ntlab.onlineAccessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jdi.TimeoutException;
import org.eclipse.jdt.core.Signature;

import com.sun.jdi.ClassLoaderReference;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.ClassType;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.InvocationException;
import com.sun.jdi.LongValue;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StringReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;

public class JDIInstanceMethodCaller extends JDIStaticMethodCaller {
	private ObjectReference receiver;
	
	public JDIInstanceMethodCaller(VirtualMachine vm, ThreadReference thread, ObjectReference receiver) {
		super(vm, thread);
		this.receiver = receiver;
	}

	public ObjectReference getReceiver() {
		return receiver;
	}

	public JDIInstanceMethodCaller changeReceiver(ObjectReference receiver) {
		this.receiver = receiver;
		return this;
	}

	/**
	 * メソッド名とObjectReferenceと引数を指定してそのオブジェクトのインスタンスメソッドを呼び出す
	 * @param methodName 呼び出したいメソッド名
	 * @param args 呼び出したいメソッドに渡す引数(Value のクラス型で可変長)
	 * @return 呼び出したメソッドからの戻り値(Value)
	 */
	public Value callInstanceMethod(String methodName, Value... args) 
			throws InvalidTypeException, ClassNotLoadedException, InvocationException, IncompatibleThreadStateException {
		ClassType type = (ClassType)receiver.type();
		List<Method> methodsByName = type.methodsByName(methodName);
		List<Value> argList = Arrays.asList(args); // メソッドに渡す引数のリスト
		return receiver.invokeMethod(thread, methodsByName.get(0), argList, thread.INVOKE_SINGLE_THREADED);	// デバッグ中のプログラム内のメソッドを呼び出す
	}
}
