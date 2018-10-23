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
	 * �p�b�P�[�W���ƃN���X���ƃ��\�b�h���ƈ������w�肵�Ă��̃N���X���\�b�h���Ăяo��
	 * @param packageName �Ăт����������\�b�h������N���X�̃p�b�P�[�W�� 
	 * @param className �Ăяo���������\�b�h������N���X��
	 * @param methodName �Ăяo���������\�b�h�� (static)
	 * @param args �Ăяo���������\�b�h�ɓn������(Value �̃N���X�^�ŉϒ�)
	 * @return �Ăяo�������\�b�h����̖߂�l(Value)
	 */
	public Value callStaticMethod(String packageName, String className, String methodName, Value... args) 
			throws InvalidTypeException, ClassNotLoadedException, InvocationException, IncompatibleThreadStateException {
		String fqcn = packageName + "." + className;
		List<ReferenceType> classes = vm.classesByName(fqcn); // �N���X�� (���S����N���X��)
		
		// ���Y�N���X���^�[�Q�b�gVM��Ń��[�h����Ă��Ȃ��ꍇ��, JDI��p���ă^�[�Q�b�gJVM��Ń��t���N�V�����ɂ�铖�Y�N���X�̃��[�h�����s����
		if (classes.isEmpty()) {
			List<ReferenceType> list = vm.classesByName("java.lang.Class");
			ClassType type = (ClassType)list.get(0);
			List<Method> methodsByName = type.methodsByName("forName");
			List<Value> argList = new ArrayList<>();
			argList.add(vm.mirrorOf(fqcn));
			type.invokeMethod(thread, methodsByName.get(0), argList, thread.INVOKE_SINGLE_THREADED);
			classes = vm.classesByName(fqcn); // �N���X�� (���S����N���X��)
		}
		ClassType type = (ClassType)classes.get(0);
		List<Method> methodsByName = type.methodsByName(methodName);
		List<Value> argList = Arrays.asList(args); // ���\�b�h�ɓn�������̃��X�g
		return type.invokeMethod(thread, methodsByName.get(0), argList, thread.INVOKE_SINGLE_THREADED);	// �f�o�b�O���̃v���O�������̃��\�b�h���Ăяo��
	}
}
