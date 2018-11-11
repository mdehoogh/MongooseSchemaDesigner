package com.officevitae.marc;

public class JavaScriptObjectOption extends Option<JavaScriptObject>{
	public JavaScriptObjectOption(OptionCollection optionCollection,OptionInfo<JavaScriptObject> optionInfo){
		super(optionCollection,optionInfo);
	}
	@Override
	public void parseValue(String valueText)throws Exception{
		super.value=new JavaScriptObject(valueText);
	}
}
