package com.officevitae.marc;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.util.Vector;

public interface ITextLinesProducer{

	void produceTextLines() throws Exception; // to be called before calling getTextLines() throwing an Exception is something goes wrong
	String[] getProducedTextLines();

	// Lines provides text through a series of lines
	public class Base extends Vector<String> implements ITextLinesProducer{
		// returning line when succeeded in adding the line!!!
		public boolean addLine(String line){
			// don't add null lines!!!
			return (line!=null&&super.add(line));
		}

		// ITextProvider implementation
		public void produceTextLines() throws Exception{
		}

		public String[] getProducedTextLines(){
			if(super.isEmpty()) return new String[]{};
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
		protected File getFile(){
			return file;
		}

		public TextFile(File file){
			this.file=file;
			if(this.file==null||!this.file.exists()) linesRead=true; // if the file does not exist, we pretend to have read all lines, so an empty array of lines will be returned!!
		}

		// overriding the base implementation to be able to JIT obtain the text lines (i.e. when required)
		// returning null when something goes wrong...
		@Override
		public void produceTextLines() throws Exception{
			// we should only return null if the file exists but somehow failed to be read completely
			super.clear(); // get rid of all lines we requested so far (possibly a retry of reading the file lines)
			if(file.exists()){
				BufferedReader br=new BufferedReader(new FileReader(file));
				String line=br.readLine();
				while(line!=null){
					if(!super.addLine(line))break;
					line=br.readLine();
				}
				br.close(); // convenient to close the file even if we fail to store all text lines!!!
				if(line!=null)throw new Exception("Unable to produce all text lines.");
			}
		}
	}

}