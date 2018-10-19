package com.officevitae.marc;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.util.Vector;

public interface ITextLinesProducer{

	String[] getTextLines() throws Exception;

	// Lines provides text through a series of lines
	public class Base extends Vector<String> implements ITextLinesProducer{
		// returning line when succeeded in adding the line!!!
		public boolean addLine(String line){
			// don't add null lines!!!
			return(line!=null&&super.add(line));
		}
		// ITextProvider implementation
		public String[] getTextLines() throws Exception{
			if(super.isEmpty())return new String[]{};
			return (String[])this.toArray(new String[this.size()]);
		}
	}

	// if the text lines originate from a text file, use File
	// essentially if the file presented does not exist or can not be read null will be returned!!!
	// if we do not want
	public class TextFile extends Base{
		private boolean linesRead=false;
		private java.io.File file=null;
		// allow access to data from subclasses (like
		protected File getFile(){return file;}
		public TextFile(File file){
			this.file=file;
			if(this.file==null||!this.file.exists())linesRead=true; // if the file does not exist, we pretend to have read all lines, so an empty array of lines will be returned!!
		}
		// overriding the base implementation to be able to JIT obtain the text lines (i.e. when required)
		// returning null when something goes wrong...
		@Override
		public String[] getTextLines() throws Exception{
			// we should only return null if the file exists but somehow failed to be read completely
			if(!linesRead){ // meaning the file seems to exist, and we should read all its lines
				try{
					BufferedReader br=new BufferedReader(new FileReader(file));
					String line=br.readLine();
					while(line!=null){
						if(!super.addLine(line))throw new Exception("Unable to obtain all text lines.");
						line=br.readLine();
					}
					linesRead=true; // all lines read and stored successfully
					br.close(); // we don't mind if something goes wrong here!!!
				}catch(Exception ex){}
			}
			return(linesRead?super.getTextLines():null);
		}
	}

	// MDH@17OCT2018: Switch is a special TextLines provider in that it can switch between different text line providers of which only one can be active at the time
	//                and contains the actual source whereas the rest is dirty!! why not call it history and make a separate list???
	//                it's job is to keep all providers synchronized
	public class Switch{

	}
}
