package com.officevitae.marc;

import javax.swing.*;
import java.awt.*;

public class FieldListCellRenderer implements ListCellRenderer<Field> {

	public Component getListCellRendererComponent(JList<? extends Field> list,Field value,int index,boolean isSelected,boolean cellHasFocus) {

		JCheckBox fieldCheckBox=new JCheckBox(value.toString()); // MDH@19OCT2018: used to be text representation BUT switched over to using toString() which only gives the name and the changed status!!!
		fieldCheckBox.setSelected(value.isEnabled());

		Color background;
		Color foreground;

		// check if this cell represents the current DnD drop location
		JList.DropLocation dropLocation = list.getDropLocation();
		if (dropLocation != null
				&& !dropLocation.isInsert()
				&& dropLocation.getIndex() == index) {
			background = Color.BLUE;
			foreground = Color.WHITE;
			// check if this cell is selected
		} else if (isSelected) {
			background = Color.BLUE;
			foreground = Color.WHITE;
			// unselected, and not the DnD drop location
		} else {
			background = Color.WHITE;
			foreground = Color.BLACK;
		}

		fieldCheckBox.setBackground(background);
		fieldCheckBox.setForeground(foreground);

		return fieldCheckBox;
	}

}
