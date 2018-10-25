package com.officevitae.marc;

public interface ICompositeFieldType extends IFieldType{
	void setSubFieldType(IFieldType fieldType);
	IFieldType getSubFieldType();
}
