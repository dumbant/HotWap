package com.sin.app;

import com.sin.app.action.ActionTest;
import com.sin.app.action.ActionTest2;
import com.sin.application.IApplication;

public class TestApplication implements IApplication{
	String name = "TestApplication1";
	public ActionTest test = null;
	@Override
	public void init() {
		test = new ActionTest();
		System.out.println("test: "+test.getClass().getClassLoader());
		test.print();
		System.out.println(name+"-->init");
	}

	@Override
	public void execute() {
		System.out.println(name+"-->do something");
	}

	@Override
	public void destory() {
		System.out.println(name+"-->destoryed");
	}

}
