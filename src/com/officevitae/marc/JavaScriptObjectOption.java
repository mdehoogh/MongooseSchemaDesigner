package com.officevitae.marc;

public class JavaScriptObjectOption extends Option<JavaScriptObject>{
	public JavaScriptObjectOption(String name,String info,JavaScriptObject _default){
		super(name,info,_default);
	}
	@Override
	public void parseValue(String valueText)throws Exception{
		super.value=new JavaScriptObject(valueText);
	}
}
