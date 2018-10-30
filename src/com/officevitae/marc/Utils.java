package com.officevitae.marc;

import javax.swing.*;
import java.io.File;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.*;

public class Utils{

	// MDH@29OCT2018: keep track of the info messages per source
	private static Map<Object,Vector<String>> infoMessagesMap=new HashMap<Object,Vector<String>>();
	public interface InfoMessageListener{
		Object getSource();
		void infoMessagesChanged();
	}
	public static Vector<InfoMessageListener> infoMessageListeners=new Vector<InfoMessageListener>();
	private static void informInfoMessageListeners(Object source){
		for(InfoMessageListener infoMessageListener:infoMessageListeners)if(source.equals(infoMessageListener.getSource()))
			try{infoMessageListener.infoMessagesChanged();}catch(Exception ex){}
	}
	public static boolean addInfoMessageListener(InfoMessageListener infoMessageListener){return(infoMessageListener!=null?infoMessageListeners.contains(infoMessageListener)||infoMessageListeners.add(infoMessageListener):null);}
	public static boolean removeInfoMessageListener(InfoMessageListener infoMessageListener){return(infoMessageListener!=null?!infoMessageListeners.contains(infoMessageListener)||infoMessageListeners.remove(infoMessageListener):false);}
	public static boolean hasInfoMessages(Object source){return(infoMessagesMap.containsKey(source)&&!infoMessagesMap.get(source).isEmpty());}
	public static List<String> getInfoMessages(Object source){return(infoMessagesMap.containsKey(source)?Collections.unmodifiableList(infoMessagesMap.get(source)):null);}
	public static void removeInfoMessages(Object source){if(infoMessagesMap.containsKey(source))infoMessagesMap.get(source).clear();informInfoMessageListeners(source);}

	public static File getPath(){
		try{
			return new File(ClassLoader.getSystemClassLoader().getResource(".").getPath());
			////////return new File(Utils.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
		}catch(Exception ex){}
		return null;
	}

	// IInfoViewer stuff
	private static IInfoViewer INFO_VIEWER=null;
	static void setInfoViewer(IInfoViewer infoViewer){INFO_VIEWER=infoViewer;}
	public static void storeInfo(Object source,String info){
		if(source==null||info==null||info.trim().isEmpty())return;
		if(!infoMessagesMap.containsKey(source))infoMessagesMap.put(source,new Vector<String>());
		try{
			infoMessagesMap.get(source).add(0,TIME_SDF.format(new Date())+"\t"+info);
			informInfoMessageListeners(source);
		}catch(Exception ex){}
	}
	public static void setInfo(Object source,String info){
		if(info==null)return;
		storeInfo(source,info);
		String sourcerep=(source!=null?source.toString():null);
		if(INFO_VIEWER!=null)
			INFO_VIEWER.setInfo(sourcerep,info);
		else
			System.out.println((sourcerep!=null?sourcerep+": ":"")+info);
	}

	private static PrintStream CONSOLE=null;
	public static void setConsole(PrintStream console){
		CONSOLE=console;
		if(CONSOLE!=null)System.out.println("Console available!");else System.out.println("No console available.");
	}
	public static void consoleprintln(String toconsole){
		if(CONSOLE!=null)CONSOLE.println(toconsole);else System.out.println(toconsole);
	}
	public static void consoleprint(String toconsole){
		if(CONSOLE!=null)CONSOLE.print(toconsole);else System.out.print(toconsole);
	}

	private static SimpleDateFormat DATE_SDF=new SimpleDateFormat("yyyy-m-d");
	private static SimpleDateFormat TIME_SDF=new SimpleDateFormat("HH:mm:ss");
	public static SimpleDateFormat TIMESTAMP_SDF=new SimpleDateFormat("d MMMM yyyy");
	public static String getTimestamp(){return TIMESTAMP_SDF.format(new Date());}

	public static final String capitalize(String text){return(text==null||text.isEmpty()?"":text.substring(0,1).toUpperCase())+text.substring(1);}
	public static final boolean directoryShouldExist(File directory){return(directory.exists()?directory.isDirectory():directory.mkdir());}

	public static int getInteger(String intText) throws NumberFormatException{return Integer.parseInt(intText);}
	public static boolean isValidInteger(int integer,long minValue,long maxValue){return(integer>=minValue&&integer<=maxValue);}
	public static double getDouble(String doubleText) throws NumberFormatException{return(doubleText==null||doubleText.trim().isEmpty()?Double.NaN:Double.parseDouble(doubleText));}
	public static boolean isValidDouble(double d,double minValue,double maxValue){
		if(Double.isNaN(d))return false;
		if(!Double.isNaN(minValue)&&d<minValue)return false;
		if(!Double.isNaN(maxValue)&&d>maxValue)return false;
		return true;
	}
	public static boolean dateNotAfter(String date1Text,String date2Text){
		// returns false if date1<=date2 but NOTE both can equal Date.now indicating the current date and time
		try{
			Date date1=(date1Text.equalsIgnoreCase("date.now")?new Date():DATE_SDF.parse(date1Text));
			Date date2=(date2Text.equalsIgnoreCase("date.now")?new Date():DATE_SDF.parse(date2Text));
			return !date1.after(date2); // return true if date1 is NOT after date1
		}catch(Exception ex){}
		return false;
	}
	public static int[] DAYS_PER_MONTH=new int[]{0,31,28,31,30,31,30,31,31,30,31,30,31};
	public static boolean isLeapYear(int year){return((year%4)==0?((year%100)==0?((year%400)==0?true:false):true):false);}
	public static boolean isValidMonthday(int year,int month,int monthday){
		if(monthday<=DAYS_PER_MONTH[month])return true;
		if(month!=2)return false; // if not Februari invalid anyway
		if(monthday>29)return false; // not 29
		return(isLeapYear(year)); // if the given year is a leap year, 29 is OK!
	}
	public static boolean isValidDate(String dateText){
		// allowing Date.now as reference to the current date
		if(dateText!=null&&!dateText.isEmpty()){
			try {
				if(dateText.equalsIgnoreCase("date.now")) return true;
				// let's for now require a valid Date format
				if(dateText.charAt(4) != '-' || dateText.charAt(7) != '-' || dateText.charAt(10) != '-') return false;
				int year=getInteger(dateText.substring(0,4));
				if(!isValidInteger(year,1970,Integer.MAX_VALUE))return false; // year not in the proper range
				int month=getInteger(dateText.substring(5,7));
				if(!isValidInteger(month,1,12))return false; // month not in the proper range
				int monthday=getInteger(dateText.substring(9));
				if(!isValidInteger(monthday,1,31))return false;
				if(!Utils.isValidMonthday(year,month,monthday))return false;
				return true;
			}catch(Exception ex){}
		}
		return false;
	}
	public static boolean isHex(String hexText, int length){ // if length is positive this should be the length of the presented hexText, otherwise any even length is acceptable!!
		int l=hexText.length();
		if(length>0){if(l!=length)return false;}else{if((l%2)!=0)return false;}
		while(--l>=0&&Character.digit(hexText.charAt(l),16)>=0);
		return(l<0);
	}

	public static String getHeadingWhitespace(String s){
		int i=0,l=s.length();
		////System.out.print("Whitespace characters at the start of '"+s+"': ");
		while(i<l&&(s.charAt(i)==' '||s.charAt(i)=='\t')){/*System.out.print(" ("+i+")"+s.charAt(i));*/i++;} // for now accept tabs and spaces only for whitespace
		////System.out.println("'");
		// replacing: while(i<l&&Character.isWhitespace(s.charAt(i)))i++;
		return s.substring(0,i);
	}

	public static boolean equalText(String text1,String text2){
		if(text1==null&&text2==null)return true;
		if(text1==null||text2==null)return false;
		if(text1.length()!=text2.length())return false;
		return text1.equals(text2);
	}
	public static boolean equalTextLists(List<String> textLines1,List<String> textLines2){
		if(textLines1==null&&textLines2==null)return true;
		if(textLines1==null||textLines2==null)return false;
		// neither false
		int l=textLines1.size();
		if(l!=textLines2.size())return false;
		while(--l>=0&&equalText(textLines1.get(l),textLines2.get(l))); // as long as the lines are equal keep going
		return(l<0); // the same when all lines were equal
	}

}
