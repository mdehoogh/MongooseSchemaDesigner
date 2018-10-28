package com.officevitae.marc;

public class TextLinesParcel{
	String[] textLines;
	public TextLinesParcel(String[] textLines){this.textLines=textLines;}
	public String[] getTextLines(){String[] textLinesToReturn=textLines;textLines=null;return textLinesToReturn;}
}
