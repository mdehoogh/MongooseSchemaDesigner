package com.officevitae.marc;

public class JavaScriptObjectOption extends Option<JavaScriptObject>{
	public JavaScriptObjectOption(OptionCollection optionCollection,int optionIndex){
		super(optionCollection,optionIndex);
	}
	@Override
	public void parseValue(String valueText)throws Exception{
		super.value=new JavaScriptObject(valueText);
	}
}
