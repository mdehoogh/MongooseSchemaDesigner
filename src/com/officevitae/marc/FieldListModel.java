package com.officevitae.marc;

import javax.swing.*;

public class FieldListModel extends DefaultListModel<Field>{
	public void fieldChanged(int fieldIndex){
		System.out.println("Field #"+fieldIndex+" changed!");
		super.fireContentsChanged(this,fieldIndex,fieldIndex);
	}
}

