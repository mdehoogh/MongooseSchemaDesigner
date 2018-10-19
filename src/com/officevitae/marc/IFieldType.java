package com.officevitae.marc;

import java.util.Comparator;

public interface IFieldType {

	// the description is a wrapper around a field type to be used for presentation
	interface Description{
		IFieldType getFieldType();
	}
	// MDH@16OCT2018: although we could define IFieldType without methods it's convenient to be able to retrieve the description of a type, so schemas can append the word 'Schema'!
	//                I suppose we're going to replace . by $, so you get a$a as subschema a of schema a and you can return a$aSchema as schema name!!!
	Description getDescription();
	// MDH@16OCT2018: it's also a good idea to be able to test whether or not a given value text representation represents a valid value!!!
	boolean representsAValidValue(String value);

}
