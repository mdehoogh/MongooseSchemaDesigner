package com.officevitae.marc;

import java.math.BigDecimal;
import java.nio.Buffer;
import java.util.*;

import static com.officevitae.marc.Utils.*;

public class Field{

	public static final int
			ARRAY_FIELD=0,BOOLEAN_FIELD=1,BUFFER_FIELD=2,DATE_FIELD=3,DECIMAL128_FIELD=4,INT32_FIELD=5,
			LONG_FIELD=6,MAP_FIELD=7,MIXED_FIELD=8,NUMBER_FIELD=9,OBJECTID_FIELD=10,STRING_FIELD=11;

	// the index types available (in the given order) are for the user to select from
	public static final String[] INDEX_TYPE_NAMES=new String[]{"","unique","index","sparse"}; // NOTE the names are fixed
	/*
	public enum IndexType{
		UNIQUE(INDEX_TYPE_NAMES[1]),INDEX(INDEX_TYPE_NAMES[2]),SPARSE(INDEX_TYPE_NAMES[3]);
		private String description;
		IndexType(String description){this.description=description;}
		public String toString(){return description;}
	}
	*/

	// TODO come up with a better way!!!
	private void setInfo(String info){
		System.out.println("Field info: "+info);
	}

	// all the relevant SchemaType literals we define
	// Default is the first non-abstract class which is going to hold a Text representing the default
	// it's probable a good idea to make separate defaults one for each of the different types with their own validate() method implementation
	public class ArrayValidatedFieldLiteral extends ValidatedFieldLiteral<Object[]>{
		protected boolean isConsideredValid(){
			return super.isConsideredValid();
		}
		public Object[] getValue(){return(isValid()?super.getText().split(","):null);}
	}
	public class BooleanValidatedFieldLiteral extends ValidatedFieldLiteral<Boolean>{
		protected boolean isConsideredValid(){
			boolean consideredValid=super.isConsideredValid();
			if(consideredValid){
				try {
					Boolean.parseBoolean(super.getText());
				}catch(Exception ex){
					consideredValid=false;
				}
			}
			return consideredValid;
		}
		public Boolean getValue(){return(isValid()?new Boolean(super.getText()):null);}
	}
	public class BufferValidatedFieldLiteral extends ValidatedFieldLiteral<Buffer>{
		protected boolean isConsideredValid(){
			return super.isConsideredValid();
		}
		public Buffer getValue(){
			return null;
		}
	}
	// given that minDate and maxDate depend on one another we directly descend DateValidatedFieldLiteral from DependentValidatedFieldLiteral
	public class DateValidatedFieldLiteral extends DependentValidatedFieldLiteral<String>{
		// can be invalidated by a minimum and/or a maximum validated literal
		private DateValidatedFieldLiteral minDateValidatedFieldLiteral=null,maxDateValidatedFieldLiteral=null;
		protected boolean isConsideredValid(){
			boolean consideredValid=super.isConsideredValid();
			if(consideredValid){
				if(Utils.isValidDate(super.getText())){
					// can still be invalid if below the allowed minimum date or above the allowed maximum date!!!
					if(minDateValidatedFieldLiteral!=null&&!minDateValidatedFieldLiteral.isDisabled()&&minDateValidatedFieldLiteral.isValid()&&dateNotAfter(super.getText(),minDateValidatedFieldLiteral.getText()))consideredValid=false;
					else
					if(maxDateValidatedFieldLiteral!=null&&!maxDateValidatedFieldLiteral.isDisabled()&&maxDateValidatedFieldLiteral.isValid()&&!dateNotAfter(super.getText(),maxDateValidatedFieldLiteral.getText()))consideredValid=false;
				}else
					consideredValid=false;
			}
			return consideredValid;
		}
		public String getValue(){
			if(!isValid())return null;
			String dateText=super.getText();
			if(dateText.equals("Date.now"))return dateText; // as is
			// assuming the text represents something that Date can understand
			return "new Date('"+dateText+"')"; // I suppose any date should be enquoted??
		}
		public void setMinDateValidatedFieldLiteral(DateValidatedFieldLiteral minDateValidatedFieldLiteral){
			if(this.minDateValidatedFieldLiteral!=null)this.minDateValidatedFieldLiteral.removeValidatedFieldLiteralChangeListener(this);
			this.minDateValidatedFieldLiteral=minDateValidatedFieldLiteral;
			if(this.minDateValidatedFieldLiteral!=null)this.minDateValidatedFieldLiteral.addValidatedFieldLiteralChangeListener(this);
		}
		public void setMaxDateValidatedFieldLiteral(DateValidatedFieldLiteral minDateValidatedFieldLiteral){
			if(this.maxDateValidatedFieldLiteral!=null)this.maxDateValidatedFieldLiteral.removeValidatedFieldLiteralChangeListener(this);
			this.maxDateValidatedFieldLiteral=minDateValidatedFieldLiteral;
			if(this.maxDateValidatedFieldLiteral!=null)this.maxDateValidatedFieldLiteral.addValidatedFieldLiteralChangeListener(this);
		}
	}
	public class Decimal128ValidatedFieldLiteral extends ValidatedFieldLiteral<BigDecimal>{
		// this is something that is equivalent to the Java BigDecimal
		protected boolean isConsideredValid(){
			boolean consideredValid=super.isConsideredValid();
			if(consideredValid) {
				try {
					new BigDecimal(super.getText().replaceAll(",", ""));
				} catch (Exception ex) {
					consideredValid=false;
				}
			}
			return consideredValid;
		}
		public BigDecimal getValue(){return(isValid()?new BigDecimal(super.getText()):null);}
	}
	// MDH@29OCT2018: convenient to have a fixed range integer field literal
	public class IntegerRangeValidatedFieldLiteral extends DependentValidatedFieldLiteral<Long>{
		private long minValue=Long.MIN_VALUE,maxValue=Long.MAX_VALUE;
		public IntegerRangeValidatedFieldLiteral(long minValue, long maxValue){
			this.minValue=minValue;
			this.maxValue=maxValue;
		}
		protected boolean isConsideredValid(){
			boolean consideredValid=super.isConsideredValid();
			if(consideredValid) {
				System.out.println("\nChecking the validity of integer field literal "+super.getId()+".");
				try{
					long l=Long.parseLong(super.getText());
					if(l<minValue||l>maxValue)consideredValid=false;
				}catch(Exception ex){
					consideredValid=false;
				}
			}
			return consideredValid;
		}
		public Long getValue(){return(isValid()?Long.parseLong(super.getText()):null);}
	}
	// given that minLength and maxLength depend on one another we can make IntegerValidatedFieldLiteral depend on DependentValidatedFieldLiteral<Long> immediately
	public class IntegerValidatedFieldLiteral extends DependentValidatedFieldLiteral<Long>{
		private IntegerValidatedFieldLiteral minIntegerValidatedFieldLiteral=null,maxIntegerValidatedFieldLiteral=null;
		private Long fixedMinimum=null,fixedMaximum=null;
		public IntegerValidatedFieldLiteral(){}
		public IntegerValidatedFieldLiteral(Long fixedMinimum,Long fixedMaximum){
			this.fixedMinimum=fixedMinimum;
			this.fixedMaximum=fixedMaximum;
		}
		protected boolean isConsideredValid(){
			boolean consideredValid=super.isConsideredValid();
			if(consideredValid) {
				System.out.println("\nChecking the validity of integer field literal "+super.getId()+".");
				try {
					long l=Long.parseLong(super.getText());
					// check against the fixed minimum and/or maximum (if any)
					if(fixedMinimum!=null&&l<fixedMinimum.longValue())consideredValid=false;
					else
					if(fixedMaximum!=null&&l>fixedMaximum.longValue())consideredValid=false;
					else
					if(minIntegerValidatedFieldLiteral!=null&&!minIntegerValidatedFieldLiteral.isDisabled()&&minIntegerValidatedFieldLiteral.isValid()&&l<minIntegerValidatedFieldLiteral.getValue())consideredValid=false;
					else
					if(maxIntegerValidatedFieldLiteral!=null&&!maxIntegerValidatedFieldLiteral.isDisabled()&&maxIntegerValidatedFieldLiteral.isValid()&&l>maxIntegerValidatedFieldLiteral.getValue())consideredValid=false;
				} catch (Exception ex) {
					consideredValid=false;
				}
			}
			return consideredValid;
		}
		public Long getValue(){return(isValid()?Long.parseLong(super.getText()):null);}
		public IntegerValidatedFieldLiteral setMinIntegerValidatedFieldLiteral(IntegerValidatedFieldLiteral minIntegerValidatedFieldLiteral){
			if(this.minIntegerValidatedFieldLiteral!=null)this.minIntegerValidatedFieldLiteral.removeValidatedFieldLiteralChangeListener(this);
			this.minIntegerValidatedFieldLiteral=minIntegerValidatedFieldLiteral;
			if(this.minIntegerValidatedFieldLiteral!=null)this.minIntegerValidatedFieldLiteral.addValidatedFieldLiteralChangeListener(this);
			return this;
		}
		public IntegerValidatedFieldLiteral setMaxIntegerValidatedFieldLiteral(IntegerValidatedFieldLiteral minIntegerValidatedFieldLiteral){
			if(this.maxIntegerValidatedFieldLiteral!=null)this.maxIntegerValidatedFieldLiteral.removeValidatedFieldLiteralChangeListener(this);
			this.maxIntegerValidatedFieldLiteral=minIntegerValidatedFieldLiteral;
			if(this.maxIntegerValidatedFieldLiteral!=null)this.maxIntegerValidatedFieldLiteral.addValidatedFieldLiteralChangeListener(this);
			return this;
		}
	}
	public class MapValidatedFieldLiteral extends ValidatedFieldLiteral<Map>{
		public Map getValue(){
			return null;
		}
	}
	public class MixedValidatedFieldLiteral extends ValidatedFieldLiteral<Map<String,String>>{
		// should be parseable as JSON????
		public Map<String,String> getValue(){
			return null;
		}
	}
	public class NumberValidatedFieldLiteral extends DependentValidatedFieldLiteral<Double>{
		private NumberValidatedFieldLiteral minNumberValidatedFieldLiteral=null,maxNumberValidatedFieldLiteral=null;
		protected boolean isConsideredValid(){
			boolean consideredValid=super.isConsideredValid();
			if(consideredValid){
				try{
					double d=Double.parseDouble(super.getText().replaceAll(",",""));
					/// NOTE: no need to do this because valid is already true, with validatedText registered as last known valid text!!! setValid(true); 
					if(minNumberValidatedFieldLiteral!=null&&!minNumberValidatedFieldLiteral.isDisabled()&&minNumberValidatedFieldLiteral.isValid()&&d<minNumberValidatedFieldLiteral.getValue())consideredValid=false;
					else
					if(maxNumberValidatedFieldLiteral!=null&&!maxNumberValidatedFieldLiteral.isDisabled()&&maxNumberValidatedFieldLiteral.isValid()&&d>maxNumberValidatedFieldLiteral.getValue())consideredValid=false;
				}catch(Exception ex){
					// we want exactly one source tree in the Number literal!!!
					if(JavaScriptUtils.getNumberOfJavaScriptSourceTrees(super.getId(),super.getText())!=1)consideredValid=false;
				}
			}
			return consideredValid;
		}
		public Double getValue(){return(isValid()?Double.parseDouble(super.getText()):null);}
		public void setMinNumberValidatedFieldLiteral(NumberValidatedFieldLiteral minNumberValidatedFieldLiteral){
			if(this.minNumberValidatedFieldLiteral!=null)this.minNumberValidatedFieldLiteral.removeValidatedFieldLiteralChangeListener(this);
			this.minNumberValidatedFieldLiteral=minNumberValidatedFieldLiteral;
			if(this.minNumberValidatedFieldLiteral!=null)this.minNumberValidatedFieldLiteral.addValidatedFieldLiteralChangeListener(this);
		}
		public void setMaxNumberValidatedFieldLiteral(NumberValidatedFieldLiteral minNumberValidatedFieldLiteral){
			if(this.maxNumberValidatedFieldLiteral!=null)this.maxNumberValidatedFieldLiteral.removeValidatedFieldLiteralChangeListener(this);
			this.maxNumberValidatedFieldLiteral=minNumberValidatedFieldLiteral;
			if(this.maxNumberValidatedFieldLiteral!=null)this.maxNumberValidatedFieldLiteral.addValidatedFieldLiteralChangeListener(this);
		}
	}
	public class ObjectIdValidatedFieldLiteral extends ValidatedFieldLiteral<Double>{
		protected boolean isConsideredValid(){
			boolean consideredValid=super.isConsideredValid();
			if(consideredValid&&!Utils.isHex(super.getText(),24))consideredValid=false;
			return consideredValid;
		}
		public Double getValue(){return(isValid()?Double.parseDouble(super.getText()):null);}
	}
	// a StringValidatedFieldLiteral like the default can depend on multiple other validated literal
	public class StringValidatedFieldLiteral extends DependentValidatedFieldLiteral<String>{
		private Set<String> options=null; // MDH@25OCT2018: let's allow specifying a list of options that the text should match!!
		private IntegerValidatedFieldLiteral minLengthValidatedFieldLiteral,maxLengthValidatedFieldLiteral;
		private StringArrayValidatedFieldLiteral valuesValidatedFieldLiteral;
		private RegExpValidatedFieldLiteral regExpValidatedFieldLiteral;
		protected boolean isConsideredValid(){
			String text=super.getText();
			boolean consideredValid=super.isConsideredValid();
			if(options!=null)return(consideredValid?options.contains(text):false); // if options are defined, they determine whether the presented text is valid!!
			// replacing: if(consideredValid&&options!=null&&!options.contains(text))consideredValid=false;
			// MDH@19OCT2018: I suppose that its easier to change isValid() so that when the thing is disabled it will return false although that's quite dangerous
			if(consideredValid&&valuesValidatedFieldLiteral!=null&&!valuesValidatedFieldLiteral.isDisabled()&&valuesValidatedFieldLiteral.isValid()&&!Arrays.asList(valuesValidatedFieldLiteral.getValue()).contains(text))consideredValid=false;
			if(consideredValid&&regExpValidatedFieldLiteral!=null&&!regExpValidatedFieldLiteral.isDisabled()&&regExpValidatedFieldLiteral.isValid()&&!text.matches(regExpValidatedFieldLiteral.getText()))consideredValid=false;
			if(consideredValid){
				int l=text.length();
				// MDH@19OCT2018: BUG FIX it's fine to call isValid() BUT if the minimum length is disabled, it's NOT active, so I've forced checking the disabled flag as well as we should
				if (minLengthValidatedFieldLiteral!=null&&!minLengthValidatedFieldLiteral.isDisabled()&&minLengthValidatedFieldLiteral.isValid()&&l<minLengthValidatedFieldLiteral.getValue())consideredValid=false;
				else
				if (maxLengthValidatedFieldLiteral!=null&&!maxLengthValidatedFieldLiteral.isDisabled()&&maxLengthValidatedFieldLiteral.isValid()&&l>maxLengthValidatedFieldLiteral.getValue())consideredValid=false;
			}
			return consideredValid;
		}
		public String getValue(){return(isValid()?"'"+super.getText()+"'":null);} // MDH@24SEP2018: for now decided to return the text enclosed in single quotes!!!
		public void setRegExpValidatedFieldLiteral(RegExpValidatedFieldLiteral regExpValidatedFieldLiteral){
			if(this.regExpValidatedFieldLiteral!=null)this.regExpValidatedFieldLiteral.removeValidatedFieldLiteralChangeListener(this);
			this.regExpValidatedFieldLiteral=regExpValidatedFieldLiteral;
			if(this.regExpValidatedFieldLiteral!=null)this.regExpValidatedFieldLiteral.addValidatedFieldLiteralChangeListener(this);
		}
		// can be restricted in length or in the set of values it may come from
		public void setMinLengthValidatedFieldLiteral(IntegerValidatedFieldLiteral minLengthValidatedFieldLiteral){
			if(this.minLengthValidatedFieldLiteral!=null)this.minLengthValidatedFieldLiteral.removeValidatedFieldLiteralChangeListener(this);
			this.minLengthValidatedFieldLiteral=minLengthValidatedFieldLiteral;
			if(this.minLengthValidatedFieldLiteral!=null)this.minLengthValidatedFieldLiteral.addValidatedFieldLiteralChangeListener(this);
		}
		public void setMaxLengthValidatedFieldLiteral(IntegerValidatedFieldLiteral maxLengthValidatedFieldLiteral){
			if(this.maxLengthValidatedFieldLiteral!=null)this.maxLengthValidatedFieldLiteral.removeValidatedFieldLiteralChangeListener(this);
			this.maxLengthValidatedFieldLiteral=maxLengthValidatedFieldLiteral;
			if(this.maxLengthValidatedFieldLiteral!=null)this.maxLengthValidatedFieldLiteral.addValidatedFieldLiteralChangeListener(this);
		}
		public void setValuesValidatedFieldLiteral(StringArrayValidatedFieldLiteral valuesValidatedFieldLiteral){
			if(this.valuesValidatedFieldLiteral!=null)this.valuesValidatedFieldLiteral.removeValidatedFieldLiteralChangeListener(this);
			this.valuesValidatedFieldLiteral=valuesValidatedFieldLiteral;
			if(this.valuesValidatedFieldLiteral!=null)this.valuesValidatedFieldLiteral.addValidatedFieldLiteralChangeListener(this);
		}
		public StringValidatedFieldLiteral(Set<String> options){
			if(options!=null&&!options.isEmpty()){
				this.options=options;
			}
		}
		public StringValidatedFieldLiteral(){}
		/////////public StringValidatedFieldLiteral(){super.setValid(true);} // force valid to True!!
	} // anything goes as default!!!
	// for keeping track of the values (enum) a String can have
	// obviously the possible values a String can have, depend on the allowed minLength and maxLength
	public class StringArrayValidatedFieldLiteral extends DependentValidatedFieldLiteral<String[]>{
		private IntegerValidatedFieldLiteral minLengthValidatedFieldLiteral,maxLengthValidatedFieldLiteral;
		private RegExpValidatedFieldLiteral regExpValidatedFieldLiteral;
		protected boolean isConsideredValid(){
			boolean consideredValid=super.isConsideredValid();
			if(consideredValid){
				String regExpText=(regExpValidatedFieldLiteral!=null&&!regExpValidatedFieldLiteral.isDisabled()&&regExpValidatedFieldLiteral.isValid()?regExpValidatedFieldLiteral.getText():null);
				List<String> valueTexts=Arrays.asList(super.getText().split(","));
				Set<String> valuesSet=new HashSet<String>(valueTexts);
				if(valuesSet.size()==valueTexts.size()){
					for(String valueText:valueTexts){
						if(regExpText!=null&&!valueText.matches(regExpText)){setInfo("'"+valueText+"' does not match '"+regExpText+"'.");consideredValid=false;break;}
						if(minLengthValidatedFieldLiteral!=null&&!minLengthValidatedFieldLiteral.isDisabled()&&minLengthValidatedFieldLiteral.isValid()&&valueText.length()<minLengthValidatedFieldLiteral.getValue()){setInfo("'"+valueText+"' too short!");consideredValid=false;break;}
						if(maxLengthValidatedFieldLiteral!=null&&!maxLengthValidatedFieldLiteral.isDisabled()&&maxLengthValidatedFieldLiteral.isValid()&&valueText.length()>maxLengthValidatedFieldLiteral.getValue()){setInfo("'"+valueText+"' too long!");consideredValid=false;break;}
					}
				}else{
					setInfo("Duplicate values not allowed!");
					consideredValid=false;
				}
			}
			return consideredValid;
		}
		public String[] getValue(){return(isValid()?super.getText().split(","):null);}
		public void setRegExpValidatedFieldLiteral(RegExpValidatedFieldLiteral regExpValidatedFieldLiteral){
			if(this.regExpValidatedFieldLiteral!=null)this.regExpValidatedFieldLiteral.removeValidatedFieldLiteralChangeListener(this);
			this.regExpValidatedFieldLiteral=regExpValidatedFieldLiteral;
			if(this.regExpValidatedFieldLiteral!=null)this.regExpValidatedFieldLiteral.addValidatedFieldLiteralChangeListener(this);
		}
		public void setMinLengthValidatedFieldLiteral(IntegerValidatedFieldLiteral minLengthValidatedFieldLiteral){
			if(this.minLengthValidatedFieldLiteral!=null)this.minLengthValidatedFieldLiteral.removeValidatedFieldLiteralChangeListener(this);
			this.minLengthValidatedFieldLiteral=minLengthValidatedFieldLiteral;
			if(this.minLengthValidatedFieldLiteral!=null)this.minLengthValidatedFieldLiteral.addValidatedFieldLiteralChangeListener(this);
		}
		public void setMaxLengthValidatedFieldLiteral(IntegerValidatedFieldLiteral maxLengthValidatedFieldLiteral){
			if(this.maxLengthValidatedFieldLiteral!=null)this.maxLengthValidatedFieldLiteral.removeValidatedFieldLiteralChangeListener(this);
			this.maxLengthValidatedFieldLiteral=maxLengthValidatedFieldLiteral;
			if(this.maxLengthValidatedFieldLiteral!=null)this.maxLengthValidatedFieldLiteral.addValidatedFieldLiteralChangeListener(this);
		}
	}
	public class NonNegativeIntegerValidatedFieldLiteral extends IntegerValidatedFieldLiteral{
		protected boolean isConsideredValid(){
			boolean consideredValid=super.isConsideredValid();
			if(consideredValid&&Long.parseLong(super.getText())<=0)consideredValid=false;
			return consideredValid;
		}
	}
	public class FunctionBodyValidatedFieldLiteral extends StringValidatedFieldLiteral{
		protected boolean isConsideredValid(){
			boolean consideredValid=super.isConsideredValid();
			// for now just allow anything that is considered valid JavaScript
			// in order to allow return statements we have to enclose the lot in a function declaration which is exactly what getValue() does!!
			if(consideredValid&&JavaScriptUtils.getNumberOfJavaScriptSourceTrees(super.getId(),getValue())<=0)consideredValid=false;
			return consideredValid;
		}
		@Override
		public String getValue(){return "function(v){"+super.getText()+"}";} // NOTE we actually do NOT need v for getters but it won't hurt????
	}
	public class RegExpValidatedFieldLiteral extends StringValidatedFieldLiteral{
		protected boolean isConsideredValid(){
			boolean consideredValid=super.isConsideredValid();
			// for now just allow anything that is considered valid JavaScript
			if(consideredValid){
				try {
					java.util.regex.Pattern.compile(super.getText());
				} catch (java.util.regex.PatternSyntaxException e) {
					consideredValid=false;
				}
			}
			return consideredValid;
		}
	}
	// end ValidatedFieldLiteral subclasses
	
	private Vector<IFieldChangeListener> fieldChangeListeners=new Vector<IFieldChangeListener>();
	public boolean addFieldChangeListener(IFieldChangeListener fieldChangeListener){
		return (fieldChangeListener!=null?fieldChangeListeners.contains(fieldChangeListener)||fieldChangeListeners.add(fieldChangeListener):false);
	}
	public boolean deleteFieldChangeListener(IFieldChangeListener fieldChangeListener){
		return (fieldChangeListener!=null?!fieldChangeListeners.contains(fieldChangeListener)||fieldChangeListeners.remove(fieldChangeListener):false);
	}

	private String lastLoadedTextRepresentation=null;
	private boolean changed=false;
	public boolean isChanged(){return changed;}

	void setChanged(boolean changed){
		//// BAD IDEA because every change needs to be reported!!! if(this.changed==changed)return;
		this.changed=changed;
		if(!this.changed)lastLoadedTextRepresentation=getTextRepresentation(true,true); // setChanged() should be set to False whenever the field is saved successfully
		for(IFieldChangeListener fieldChangeListener:fieldChangeListeners)try{fieldChangeListener.fieldChanged(this);}catch(Exception ex){}
	}

	void updateChanged(){
		String textRepresentation=getTextRepresentation(true,true); // what would have been written (not what is displayed!!)
		setChanged(!textRepresentation.equals(lastLoadedTextRepresentation));
	}

	private String name;

	private IFieldType type=MongooseFieldType.MIXED; // MDH@16OCT2018: FieldType changed to IFieldType

	// anything entered through text is considered a literal (although Date.now is not actually a literal!!!)
	ValidatedFieldLiteral refLiteral=(ValidatedFieldLiteral)(new StringValidatedFieldLiteral()).setField(this).setId("Ref");
	ValidatedFieldLiteral defaultLiteral=(ValidatedFieldLiteral)(new MixedValidatedFieldLiteral()).setField(this).setId("Default");

	// MDH@02NOV2018: even though the minLengthLiteral is free from being compared with maxLengthLiteral now, it still needs to be positive!!
	IntegerValidatedFieldLiteral
			minLengthLiteral=(IntegerValidatedFieldLiteral)(new IntegerValidatedFieldLiteral(1L,null)).setField(this).setId("Minimum length"),
			maxLengthLiteral=(IntegerValidatedFieldLiteral)(new IntegerValidatedFieldLiteral()).setField(this).setId("Maximum length");

	// MDH@25OCT2018: we can make an integer validated literal for storing the index type number where 0 represents no index type (and is therefore NOT valid)
	//                changed to be a String literal
	// MDH@01NOV2018: NOT allowing direct access to the index type!!! hmm, we do need it to pass to IndexTypeLiteralView so ok fix otherwise
	StringValidatedFieldLiteral indexTypeLiteral=(StringValidatedFieldLiteral)(new StringValidatedFieldLiteral(Set.of(INDEX_TYPE_NAMES))).setField(this).setId("Index");

	IntegerValidatedFieldLiteral startAtLiteral=(IntegerValidatedFieldLiteral)(new IntegerValidatedFieldLiteral()).setField(this).setId("Start at"); // it's most likely that the default startAt value will be 1 for auto-increment Number fields!!

	public NumberValidatedFieldLiteral
			minNumberLiteral=(NumberValidatedFieldLiteral)(new NumberValidatedFieldLiteral()).setField(this).setId("Minimum number"),
			maxNumberLiteral=(NumberValidatedFieldLiteral)(new NumberValidatedFieldLiteral()).setField(this).setId("Maximum number"); // minimum and maximum number should never be NaN!!

	// use the current date as min and max date (NOTE the flags are false (not set) by default!!!
	DateValidatedFieldLiteral
			minDateLiteral=(DateValidatedFieldLiteral)(new DateValidatedFieldLiteral()).setField(this).setId("Minimum date"),
			maxDateLiteral= (DateValidatedFieldLiteral)(new DateValidatedFieldLiteral()).setField(this).setId("Maximum date");

	StringArrayValidatedFieldLiteral valuesLiteral=(StringArrayValidatedFieldLiteral)(new StringArrayValidatedFieldLiteral()).setField(this).setId("Values"); // initialize to an array with a single empty String-element!!!
	RegExpValidatedFieldLiteral matchLiteral=(RegExpValidatedFieldLiteral)(new RegExpValidatedFieldLiteral()).setField(this).setId("Match");

	FunctionBodyValidatedFieldLiteral
			validateLiteral=(FunctionBodyValidatedFieldLiteral)(new FunctionBodyValidatedFieldLiteral()).setField(this).setId("validate"),
			getLiteral=(FunctionBodyValidatedFieldLiteral)(new FunctionBodyValidatedFieldLiteral()).setField(this).setId("get"),
			setLiteral=(FunctionBodyValidatedFieldLiteral)(new FunctionBodyValidatedFieldLiteral()).setField(this).setId("set");

	StringValidatedFieldLiteral aliasLiteral=(StringValidatedFieldLiteral)(new StringValidatedFieldLiteral()).setField(this).setId("alias");

	// all setters mark the field as changed
	public Field setType(IFieldType type){
		if(!this.type.equals(type)){
			String defaultTextNow=defaultLiteral.getText(); // we want to keep whatever text currently stored in the default
			this.type=type;
			// switch the default class type accordingly
			if(this.type instanceof MongooseFieldType)
			switch(((MongooseFieldType)this.type).ordinal()){
				case ARRAY_FIELD:
					defaultLiteral=new ArrayValidatedFieldLiteral();
					if(arrayElementType==null)arrayElementType=MongooseFieldType.MIXED; // so that we won't see Array as type but []
					break;
				case BOOLEAN_FIELD:
					defaultLiteral=new BooleanValidatedFieldLiteral();
					break;
				case BUFFER_FIELD:
					defaultLiteral=new BufferValidatedFieldLiteral();
					break;
				case DATE_FIELD:
					defaultLiteral=new DateValidatedFieldLiteral();
					((DateValidatedFieldLiteral) defaultLiteral).setMaxDateValidatedFieldLiteral(maxDateLiteral);
					((DateValidatedFieldLiteral) defaultLiteral).setMinDateValidatedFieldLiteral(minDateLiteral);
					// MDH@02NOV2018: we're going to let the minimum date be free to choose, and the maximum should exceed the minimum of course
					//////////minDateLiteral.setMaxDateValidatedFieldLiteral(maxDateLiteral);
					maxDateLiteral.setMinDateValidatedFieldLiteral(minDateLiteral);
					break;
				case DECIMAL128_FIELD:
					defaultLiteral=new Decimal128ValidatedFieldLiteral();
					break;
				case INT32_FIELD:
				case LONG_FIELD:
					defaultLiteral=new IntegerValidatedFieldLiteral();
					break;
				case MAP_FIELD:
					defaultLiteral=new MapValidatedFieldLiteral();
					break;
				case MIXED_FIELD:
					defaultLiteral=new MixedValidatedFieldLiteral();
					break;
				case NUMBER_FIELD:
					defaultLiteral=new NumberValidatedFieldLiteral();
					((NumberValidatedFieldLiteral) defaultLiteral).setMaxNumberValidatedFieldLiteral(maxNumberLiteral);
					((NumberValidatedFieldLiteral) defaultLiteral).setMinNumberValidatedFieldLiteral(minNumberLiteral);
					// MDH@02NOV2018: making a cyclic check is guaranteed to cause a stack overflow
					//                so I'm gonna let the minimum to be free, and the maximum not
					////////minNumberLiteral.setMaxNumberValidatedFieldLiteral(maxNumberLiteral);
					maxNumberLiteral.setMinNumberValidatedFieldLiteral(minNumberLiteral);
					break;
				case OBJECTID_FIELD:
					defaultLiteral=new ObjectIdValidatedFieldLiteral();
					break;
				case STRING_FIELD:
					defaultLiteral=new StringValidatedFieldLiteral();
					((StringValidatedFieldLiteral) defaultLiteral).setMaxLengthValidatedFieldLiteral(maxLengthLiteral);
					((StringValidatedFieldLiteral) defaultLiteral).setMinLengthValidatedFieldLiteral(minLengthLiteral);
					((StringValidatedFieldLiteral) defaultLiteral).setValuesValidatedFieldLiteral(valuesLiteral);
					((StringValidatedFieldLiteral)defaultLiteral).setRegExpValidatedFieldLiteral(matchLiteral);
					// MDH@02NOV2018: same here, minLength is free (well it must be positive mind!!)
					///////minLengthLiteral.setMaxIntegerValidatedFieldLiteral(maxLengthLiteral);
					maxLengthLiteral.setMinIntegerValidatedFieldLiteral(minLengthLiteral);
					valuesLiteral.setMinLengthValidatedFieldLiteral(minLengthLiteral);
					valuesLiteral.setMaxLengthValidatedFieldLiteral(maxLengthLiteral);
					valuesLiteral.setRegExpValidatedFieldLiteral(matchLiteral);
					break;
			}
			defaultLiteral.setField(this); // MDH@15OCT2018: pretty essential bro'
			defaultLiteral.setId("Default");
			// plug in the recovered default text we had before!!!
			defaultLiteral.setText(defaultTextNow);
			// the validity of the new default literal depends on

			setInfo("Type of field "+name+" changed to "+type.toString()+".");
			updateChanged();
		}
		return this;
	}
	
	// MDH@01NOV2018: blockable properties
	public void setRequired(boolean required){
		if(requiredBlocked)return; // do NOT allow changing...
		if(this.required==required)return;
		this.required=required;
		updateChanged();
	}
	public void setIndexType(String indexTypeText){
		if(indexBlocked)return;
		if(indexTypeText==null)return;
		if(indexTypeLiteral.getText().equals(indexTypeText))return;
		indexTypeLiteral.setText(indexTypeText);
		updateChanged();
	}
	
	/*
	// MDH@25OCT2018: we can leave these flag methods in
	public void setIndex(boolean index){
		if(index)indexTypeLiteral.setText(INDEX_TYPE_NAMES[1]);
		updateChanged();
	}
	public void setUnique(boolean unique){
		if(unique)indexTypeLiteral.setText(INDEX_TYPE_NAMES[2]);
		updateChanged();
	}
	public void setSparse(boolean sparse){
		if(sparse)indexTypeLiteral.setText(INDEX_TYPE_NAMES[3]);
		updateChanged();
	}
	*/
	public void setLowercase(boolean lowercase){
		if(this.lowercase==lowercase)return;
		this.lowercase=lowercase;
		if(this.lowercase)this.uppercase=false;
		updateChanged();
	}
	public void setUppercase(boolean uppercase){
		if(this.uppercase==uppercase)return;
		this.uppercase=uppercase;
		if(this.uppercase)this.lowercase=false;
		updateChanged();
	}
	/*
	public void setAutoIncremented(boolean autoincremented){
		if(this.autoincremented==autoincremented)return;
		// would it be allowed to auto-increment this field????
		if(autoincremented)if(!type.equals(FieldType.NUMBER)){setInfo("Attempt to turn field "+name+" into an auto-incremented field failed!");return;}
		this.autoincremented=autoincremented;
		updateChanged();
	}
	*/
	public void setTrim(boolean trim){
		if(this.trim==trim)return;
		this.trim=trim;
		updateChanged();
	}
	public void setSelect(boolean select){
		if(this.select==select)return;
		this.select=select;
		updateChanged();
	}
	private String tag="";
	public String getTag(){return tag;}
	public void setTag(String tag){if(tag==null)return;if(tag.equals(this.tag))return;this.tag=tag;setChanged(true);}

	// MDH@18OCT2018: knows the collection it belongs to...
	private MongooseSchema.FieldCollection fieldCollection=null;
	public Field setCollection(MongooseSchema.FieldCollection fieldCollection){
		this.fieldCollection=fieldCollection;
		return this;
	}
	public MongooseSchema.FieldCollection getCollection(){return fieldCollection;}

	// as any valid refText and defaultText should be a non-negative String we use null to indicate no reference/default, so never accept a refText, defaultText that is empty!!!
	// this means that refText and defaultText will either be a non-empty String or null (at the start!!), a user is free to turn the refFlag/defaultFlag off of course
	// we can basically still use the flags although probably it's more convenient to put the flags inside the different ValidatedFieldLiteral elements
	// user should use the flags to turn on/off, if input invalid user can see the current value again by turning on the flag through the check box!!!
	private boolean required=false,select=true; // MDH@01NOV2018: select true by default...

	// String type flags
	private boolean lowercase=false,uppercase=false,trim=false;
	
	// MDH@01NOV2018: some additional flags that determine whether required or the index can change
	private boolean requiredBlocked=false;
	public boolean isRequiredBlocked(){return requiredBlocked;}
	private boolean indexBlocked=false;
	public boolean isIndexBlocked(){return indexBlocked;}
	public Field(String name){
		this.name=name;
		// any _id is block required (if it was not a user could try to create a document without an _id in it)
		if(this.name.equals("_id")){virtualBlocked=true;setRequired(true);requiredBlocked=true;indexTypeLiteral.setText("unique");indexBlocked=true;}
		///////indexTypeLiteral.setText(INDEX_TYPE_NAMES[0]); // MDH@30OCT2018: we might need this...
	}
	// MDH@01NOV@2018: now also possible to define a field as virtual (which technically require at least a get function
	private boolean virtual=false,virtualBlocked=false;
	public boolean isVirtualBlocked(){return virtualBlocked;}
	public boolean isVirtual(){return virtual;}
	public void setVirtual(boolean virtual){if(virtualBlocked)return;if(this.virtual==virtual)return;this.virtual=virtual;updateChanged();}

	public String getName(){return name;}
	public IFieldType getType(){return type;}

	private IFieldType arrayElementType=null;
	public IFieldType getArrayElementType(){return arrayElementType;}

	private boolean enabled=true;
	public boolean isEnabled(){return enabled;}
	public Field setEnabled(boolean enabled) throws IllegalArgumentException{
		if(this.enabled!=enabled){
			if(enabled&&!enamable)throw new IllegalArgumentException("This field cannot be enabled.");
			if(!enabled&&!disabable)throw new IllegalArgumentException("The field cannot be disabled.");
			this.enabled=enabled;
			updateChanged();
		}
		return this;
	} // we can updateChanged() to indicate a change to any

	private boolean enamable=true,disabable=true; // MDH@15OCT2018: by default a field is enabled and NOT disabable...
	public Field setEnamable(boolean enamable) throws IllegalArgumentException{
		// you cannot set enamable to False when disabable is also False
		if(!enamable&&!disabable)throw new IllegalArgumentException("Cannot disallow enabling because we're not allowed to disable.");
		this.enamable=enamable;
		if(!this.enamable&&this.enabled)setEnabled(false);
		return this;
	}
	public boolean isEnamable(){return enamable;}
	public Field setDisabable(boolean disabable) throws IllegalArgumentException{
		// you cannot set enamable to False when disabable is also False
		if(!enamable&&!disabable)throw new IllegalArgumentException("Cannot disallow enabling because we're not allowed to disable.");
		this.disabable=disabable;
		if(!this.disabable&&!this.enabled)setEnabled(true);
		return this;
	}

	// I suppose we can take the type and other flags into acount as well and use them in the getters of the properties to be used in getTextRepresentation()!!
	public boolean isAutoIncremented(){return(type.equals(MongooseFieldType.NUMBER)?!startAtLiteral.isDisabled()&&startAtLiteral.isValid():false);} // takes the field type also into account!!!
	public boolean isReferencing(){return(type.equals(MongooseFieldType.OBJECTID)||type.equals(MongooseFieldType.NUMBER)||type.equals(MongooseFieldType.STRING)||type.equals(MongooseFieldType.BUFFER)?!refLiteral.isDisabled()&&refLiteral.isValid():false);}

	public boolean isRequired(){return(isAutoIncremented()?false:required);}
	public boolean isSelect(){return select;}
	/*
	public boolean isIndex(){return(isAutoIncremented()?false:index);}
	public boolean isUnique(){return(isAutoIncremented()?false:unique);}
	public boolean isSparse(){return(isAutoIncremented()?false:sparse);}
	*/
	public boolean isLowercase(){return(type.equals(MongooseFieldType.STRING)?lowercase:false);}
	public boolean isUppercase(){return(type.equals(MongooseFieldType.STRING)?uppercase:false);}
	public boolean isTrim(){return(type.equals(MongooseFieldType.STRING)?trim:false);}

    public boolean setArrayElementType(IFieldType arrayElementType){
		if(arrayElementType==null){System.out.println("Undefined array element type presented to field '"+name+"'.");return false;}
		if(!type.equals(MongooseFieldType.ARRAY)){System.out.println("Not allowed to set the array element type on a non-array field.");return false;}
		if(arrayElementType.equals(this.arrayElementType))return false;
		this.arrayElementType=arrayElementType;
		updateChanged();
		return true;
	}

	// MDH@18OCT2018: we do NOT want undefined (empty) properties to show up unless they are enabled..
	// MDH@31OCT2018: given that the default disabled flag is now false, it should be just the other way round
	//                i.e. always show when disabled is true (-)
	//                I suppose that even if the literal is invalid we want to set the text from it
	public String getLiteralRepresentation(String literalName,boolean serializing,ValidatedFieldLiteral literal){
		String literalRepresentation=literal.getText();
		if(literalRepresentation!=null){
			/*
			if(!literal.isDisabled()||serializing||literal.isValid())
				return "\t"+(serializing?(literal.isDisabled()?"-":"+"):"")+literalName+"="+literalRepresentation;
			*/
			// if not disabled i.e. enabled prefix a + if serializing, otherwise the literal needs to be valid
			if(literal.isDisabled())return(serializing||literal.isValid()?"\t"+(serializing?"-":"")+literalName+"="+literalRepresentation:"");
			// an enabled literal (the default), we're not returning anything unless we serializing
			if(serializing)if(!literalRepresentation.isEmpty())return "\t+"+literalName+"="+literalRepresentation;
		}
		return "";
	}
	// MDH@25OCT2018: convenient to have a separate method to return the field type representation which cuts off the wrapper that MapFieldType puts around it's representation
	//                which is NOT needed at the top level
	public String getTypeRepresentation(boolean internal){
		// we have to be careful here, because if this is the reference to an subschema field type we need another representation
		// so, when the given type is NOT a predefined field type we also return the 'internal' description (so we'll get something that ends with Schema as we want it to!!)
		String typeRepresentation=(internal||!(type instanceof MongooseFieldType)?type.getDescription().toString():type.toString());
		//// ?? if(typeRepresentation.startsWith("{type:")&&typeRepresentation.endsWith("}"))return typeRepresentation.substring(6,typeRepresentation.length()-1).trim();
		return typeRepresentation;
	}
	// MDH@25OCT2018: published means do not use the internal representation of the type
	public String getTextRepresentation(boolean serialized,boolean internal){
		// ok this depends on both the type and some of the flags what is to be returned!!!
		// default/options/index all require that this thing is NOT autoincremented!!!
		// it's easiest to put the asterisk at the end to indicate the change
		StringBuilder sbRepresentation=new StringBuilder((serialized?(enabled?"+":"-"):"")+name+":"); // prefix the name with whether or not currently enabled (or publishable) but only when serializing!!!

		boolean autoIncremented=isAutoIncremented();

		// MDH@16OCT2018: type is a bit of an issue, because when the type is an array, we use a different representation (i.e. not just Array)
		// so something enclosed in square brackets represents the type of the array elements (typically Mixed)
		/* NOTE I fixed this problem by creating an array specific field type: ArrayFieldType!!
		if(type.equals(MongooseFieldType.ARRAY))
			sbRepresentation.append("["+(arrayElementType==null||MongooseFieldType.MIXED.equals(arrayElementType)?"":arrayElementType.toString())+"]"); // MDH@24OCT2018: switching to using the toString() on the type itself, no longer on the description of the type!!
		else
		*/
		sbRepresentation.append(getTypeRepresentation(serialized?internal:true)); // MDH@24OCT2018: the type itself should know how to show itself

		if(isVirtual())sbRepresentation.append("\tvirtual"); // MDH@01NOV2018: all other properties don't mean much if the thing is tagged as virtual though

		// NOTE as you can see the alias text is written even if undefined BUT in that case isDisabled() should return false, not true
		sbRepresentation.append(getLiteralRepresentation("alias",serialized,aliasLiteral));

		// TODO put tag at the end?????
		if(tag!=null&&!tag.trim().isEmpty())sbRepresentation.append("\t$"+tag); // MDH@30OCT2018

		// when serializing always to store the actual text stored, otherwise only when we have a valid default!!
		sbRepresentation.append(getLiteralRepresentation("default",serialized,defaultLiteral));

		// if auto incremented no need to set the required flag (as it is definitely NOT required in creating the record!!!)
		if(!autoIncremented)if(required)sbRepresentation.append("\trequired");
            /* startAt now determines whether or not a field is an auto-increment field!!!
            else
                sbRepresentation.append("\t-autoincremented");
            */
		if(!select)sbRepresentation.append("\tnoselect"); // MDH@01NOV2018: as select is the default we only write noselect when select is false!!!

		if(autoIncremented)sbRepresentation.append(getLiteralRepresentation("startAt",serialized,startAtLiteral));

		// index flags
		// of course, if the index literal is disabled, not to write the flag!!
		// even better:
		sbRepresentation.append(getLiteralRepresentation("indextype",serialized,indexTypeLiteral));
		// replacing: if(!indexTypeLiteral.isDisabled()&&indexTypeLiteral.isValid())sbRepresentation.append("\t-"+indexTypeLiteral.getText()); // NOTE do NOT call getValue() because getValue() will enquote the text!!!
		/* replacing:
		if (isUnique())sbRepresentation.append("\t-unique");
		if (isIndex())sbRepresentation.append("\t-index");
		if (isSparse())sbRepresentation.append("\t-sparse");
		*/

		// type-specific stuff
		if(!name.equals("_id")){ // don't allow a ref on something called _id
			if(type.equals(MongooseFieldType.OBJECTID)||type.equals(MongooseFieldType.NUMBER)||type.equals(MongooseFieldType.STRING)||type.equals(MongooseFieldType.BUFFER)){ // referencing is allowed on these 4 different types (although OBJECTID is most common)
				sbRepresentation.append(getLiteralRepresentation("ref",serialized,refLiteral));
			}
		}

		// MDH@01NOV2018: don't wanna loose these!!!
		sbRepresentation.append(getLiteralRepresentation("get",serialized,getLiteral));
		sbRepresentation.append(getLiteralRepresentation("set",serialized,setLiteral));
		sbRepresentation.append(getLiteralRepresentation("validate",serialized,validateLiteral));

		// String
		if(type.equals(MongooseFieldType.STRING)) {
			if(isLowercase())sbRepresentation.append("\tlowercase");else if(isUppercase())sbRepresentation.append("\tuppercase");
			if(isTrim())sbRepresentation.append("\ttrim");
			sbRepresentation.append(getLiteralRepresentation("minlength",serialized,minLengthLiteral));
			sbRepresentation.append(getLiteralRepresentation("maxlength",serialized,maxLengthLiteral));
			sbRepresentation.append(getLiteralRepresentation("values",serialized,valuesLiteral)); // assuming I do NOT need to join what getText() returns
			sbRepresentation.append(getLiteralRepresentation("match",serialized,matchLiteral)); // same here!!
			/* replacing:
			if (serialized) sbRepresentation.append("\t" + (minLengthLiteral.isDisabled() ? "-" : "+") + "minlength=" + minLengthLiteral.getText());
			else if (!minNumberLiteral.isDisabled()&&minLengthLiteral.isValid()) sbRepresentation.append("\tminlength=" + minLengthLiteral.getText());
			if (serialized) sbRepresentation.append("\t" + (maxLengthLiteral.isDisabled() ? "-" : "+") + "maxlength=" + maxLengthLiteral.getText());
			else if (!maxLengthLiteral.isDisabled()&&maxLengthLiteral.isValid()) sbRepresentation.append("\tmaxlength=" + maxLengthLiteral.getText());
			if (serialized) sbRepresentation.append("\t" + (valuesLiteral.isDisabled() ? "-" : "+") + "values=" + String.join(",",valuesLiteral.getText()));
			else if (!valuesLiteral.isDisabled()&&valuesLiteral.isValid()) sbRepresentation.append("\tvalues=" + valuesLiteral.getText());
			if (serialized) sbRepresentation.append("\t" + (matchLiteral.isDisabled() ? "-" : "+") + "match=" + String.join(",",matchLiteral.getText()));
			else if (!matchLiteral.isDisabled()&&matchLiteral.isValid()) sbRepresentation.append("\tmatch=" + valuesLiteral.getText());
			*/
		}
		if(type.equals(MongooseFieldType.NUMBER)) {
			// Number
			sbRepresentation.append(getLiteralRepresentation("minNumber",serialized,minNumberLiteral));
			sbRepresentation.append(getLiteralRepresentation("maxNumber",serialized,maxNumberLiteral));
		}
		// Date
		if(type.equals(MongooseFieldType.DATE)){
			sbRepresentation.append(getLiteralRepresentation("minDate",serialized,minDateLiteral));
			sbRepresentation.append(getLiteralRepresentation("maxDate",serialized,maxDateLiteral));
		}
		return sbRepresentation.toString();
	}
	public String toString(){
		String representation=name; // replacing getTextRepresentation(false)
		if(changed)representation+="*";
		return representation;
	} // for showing in the Field list view
	public boolean equals(Object o){
		try{
			return name.equalsIgnoreCase(((Field)o).getName());
		}catch(Exception ex){}
		return false;
	}
}
