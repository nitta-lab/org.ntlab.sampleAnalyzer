package org.ntlab.onlineAccessor;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.internal.debug.core.model.JDIDebugTarget;

import com.sun.jdi.VirtualMachine;

@SuppressWarnings("restriction")
public class JDIDebuggingVirtualMachine {

	public static VirtualMachine getDebuggingVirtualMachine() 
			throws NotExecutedException, NotSuspendedException, NotDebuggedException {
		ILaunchManager lm = DebugPlugin.getDefault().getLaunchManager();
		ILaunch[] launches = lm.getLaunches();
		if (launches.length == 0) {
			throw new NotExecutedException(); // 一度もJavaプログラムを実行していないとき
		} else {
			ILaunch debugLaunch = null;
			for (int i = 0; i < launches.length; i++) {
				System.out.print(launches[i].getLaunchConfiguration().getName() + ":");
				System.out.print(launches[i].getDebugTarget());
				if (launches[i].getDebugTarget() != null) {
					debugLaunch = launches[i];
					break;
				}
			}
			if (debugLaunch != null) {
				JDIDebugTarget debugTarget = ((JDIDebugTarget)debugLaunch.getDebugTarget());
				VirtualMachine vm = debugTarget.getVM();
				if (vm != null) {
					return vm;
				} else {
					throw new NotSuspendedException(); // Javaプログラムをデバッグ実行中の停止状態にしていないとき
				} 
			} else {
				throw new NotDebuggedException(); // Javaプログラムをデバッグ実行していないとき
			}
		}
	}
}
