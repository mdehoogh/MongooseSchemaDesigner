package com.officevitae.marc;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.util.Vector;

public interface ITextLinesProducer{

	void produceTextLines() throws Exception; // to be called before calling getTextLines() throwing an Exception is something goes wrong
	String[] getProducedTextLines();

	// the Base implementation simply holds the String[] collection for safe-keeping
	public class Base implements ITextLinesProducer{
		// returning line when succeeded in adding the line!!!
		private String[] textLines=null;
		protected ITextLinesProducer setProducedTextLines(String[] producedTextLines){textLines=producedTextLines;return this;}
		public Base(){}
		public Base(String[] textLines){this.textLines=textLines;}
		// ITextProvider implementation
		public void produceTextLines() throws Exception{}
		public String[] getProducedTextLines(){return textLines;}
	}

	// Lines provides text through a series of lines
	public abstract class Sequential extends Vector<String> implements ITextLinesProducer{
		// returning line when succeeded in adding the line!!!
		public boolean addLine(String line){return (line!=null&&super.add(line));}
		// ITextProvider implementation
		public abstract void produceTextLines() throws Exception; // doesn't produce anything itself
		// getPropertyTextLines() is probably not called more than once
		public String[] getProducedTextLines(){
			return(super.isEmpty()?new String[]{}:(String[])this.toArray(new String[this.size()]));
		}
		// call equalsTextList to compare the produced text lines (as kept as Vector) with another list!!
		public boolean equalsTextList(java.util.List<String> textLinesList){return Utils.equalTextLists(textLinesList,this);}
	}

	// if the text lines originate from a text file, use File
	// essentially if the file presented does not exist or can not be read null will be returned!!!
	// if we do not want
	public class TextFile extends Sequential{
		private boolean linesToRead=false;
		private java.io.File file=null;
		// allow access to data from subclasses (like
		protected File getFile(){return file;}
		public TextFile(File file){this.file=file;} // call super() under the hood
		@Override
		public void produceTextLines() throws Exception{
			super.clear(); // clearing whether or not the file exists or not (because we'd be waisting storage then)
			linesToRead=(this.file!=null&&this.file.exists());
			if(linesToRead){
				BufferedReader br=new BufferedReader(new FileReader(file));
				String line=br.readLine();
				while(line!=null){if(!super.add(line))break;line=br.readLine();}
				br.close(); // convenient to close the file even if we fail to store all text lines!!!
				if(line!=null)throw new Exception("Unable to produce all text lines.");
			}
		}
		public String[] getProducedTextLines(){return(linesToRead?super.getProducedTextLines():null);}
	}

}