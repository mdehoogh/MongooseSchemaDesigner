package com.officevitae.marc;

import java.util.Vector;

public class OptionCollection extends Vector<Option> implements ITextLinesContainer{

	private static final Vector<OptionInfo> OPTIONINFOS=new Vector<OptionInfo>();

	public static OptionInfo getOptionInfo(int optionIndex){return OPTIONINFOS.get(optionIndex);}

	static {
		// let's add all possible options with their default
		OPTIONINFOS.add(new OptionInfo<Boolean>("autoIndex","Create indexes at start (default: true).",Boolean.TRUE));
		OPTIONINFOS.add(new OptionInfo<Boolean>("autoCreate","Create collection before creating indexes (default: false).",Boolean.FALSE));
		OPTIONINFOS.add(new OptionInfo<Boolean>("bufferCommands","Buffer commands when connection goes down until reconnect (default: true).",Boolean.TRUE));
		OPTIONINFOS.add(new OptionInfo<JavaScriptObject>("capped","size: maximum collection size in bytes (no default), max: (optional) maximum number of docments in collection (default: unrestricted), autoIndexId (default: false).",new JavaScriptObject("{size:0,max:0,autoIndexId:false}")));
		OPTIONINFOS.add(new OptionInfo<String>("collection","Name of the collection (default: plural of schema name).",""));
		OPTIONINFOS.add(new OptionInfo<String>("id","getter function to return the document id (default: _id).",""));
		////////if(getParent()!=null)super.add(new Option<Boolean>("_id","Add _id automatically (default: true).",Boolean.TRUE));
		OPTIONINFOS.add(new OptionInfo<Boolean>("minimize","Remove empty objects (default: true)",Boolean.TRUE));
		// TODO "read" is actually a complex option, and we need to allow these too
		OPTIONINFOS.add(new OptionInfo<String>("read","Read preferences to apply (default: p). Specify p, pp, s, sp or n.","p"));
		OPTIONINFOS.add(new OptionInfo<JavaScriptObject>("writeConcern","The write concern (w (0|1|'majority'|<tag set>), j flag and wtimeout (>1) attributes).",new JavaScriptObject("{w:0,j:false,wtimeout:0}")));
		OPTIONINFOS.add(new OptionInfo<Boolean>("safe","Write concern shortcut (default: false = writeConcern:{w:0}).",Boolean.FALSE));
		OPTIONINFOS.add(new OptionInfo<String>("shardKey","The shard key (tag and name attributes) to use in sharded collection insert/update operations (default: undefined).",""));
		OPTIONINFOS.add(new OptionInfo<Boolean>("strict","Prevents values in the model that are undefined in the schema to be saved (default: true).",Boolean.TRUE));
		OPTIONINFOS.add(new OptionInfo<Boolean>("strictQuery","Strict mode on/off switch for the filter parameter to queries (default: false).",Boolean.FALSE));
		OPTIONINFOS.add(new OptionInfo<JavaScriptObject>("toJSON","Same as toObject but only when the documents toJSON method is called.",new JavaScriptObject("{getters:false,virtuals:false,minimize:true,transform:null,depopulate:false,versionKey:true}")));
		// TODO look up all possible toObject() options
		OPTIONINFOS.add(new OptionInfo<JavaScriptObject>("toObject","Default options for each toObject() document method call.",new JavaScriptObject("{getters:false,virtuals:false,minimize:true,transform:null,depopulate:false,versionKey:true}")));
		OPTIONINFOS.add(new OptionInfo<String>("typeKey","Name of key to declare the type with (default: 'type').","type"));
		OPTIONINFOS.add(new OptionInfo<Boolean>("validatedBeforeSave","Validate before save (default: true).",Boolean.TRUE));
		OPTIONINFOS.add(new OptionInfo<String>("versionKey","The path to use for versioning (default: '__v'). Use 'false' to disable automatic versioning.","__v"));
		OPTIONINFOS.add(new OptionInfo<JavaScriptObject>("collation","The default collation (locale (e.g. 'en_US') and strength (1=ignore case and diacritics) attribute) for every query and aggregation (default: ?).",new JavaScriptObject("{locale:'',strength:0}")));
		OPTIONINFOS.add(new OptionInfo<String>("skipVersioning","Paths (comma-delimited) to exclude from versioning (default: none).",""));
		// TODO instead of true, other names for createdAt and updatedAt can be used as well
		OPTIONINFOS.add(new OptionInfo<Boolean>("timestamps","If set, Mongoose assigns createdAt and updatedAt fields to the schema.",Boolean.FALSE));
		OPTIONINFOS.add(new OptionInfo<Boolean>("useNestedStrict","Use strict mode on subschema documents as well (default: false)",Boolean.FALSE));
		OPTIONINFOS.add(new OptionInfo<Boolean>("selectPopulatedPaths","Select populated paths automatically (default: true).",Boolean.TRUE));
		OPTIONINFOS.add(new OptionInfo<Boolean>("storeSubdocValidationError","Record a validation error in the single nested schema path as well when there is a validation error in a subpath (default: true).",Boolean.TRUE));
	}
	private Object host=null;

	public interface OptionChangeListener{
		void optionChanged(int optionIndex);
	}
	private OptionChangeListener optionChangeListener=null;
	public void setOptionChangeListener(OptionChangeListener optionChangeListener){
		this.optionChangeListener=optionChangeListener;
	}
	public void optionChanged(int optionIndex){
		if(optionChangeListener!=null)try{optionChangeListener.optionChanged(optionIndex);}catch(Exception ex){}
	}

	// allow setting a parent, as backup for default options which is ONLY applicable in producing text line
	private OptionCollection parent=null;
	public OptionCollection setParent(OptionCollection parent){this.parent=parent;return this;}

	public Option getOptionWithName(String name){
		for(Option option:this)if(option.getName().equals(name))return option;
		return null;
	}
	public void parseOptionValue(String[] optionNameValuePair) throws Exception{
		getOptionWithName(optionNameValuePair[0]).parseValue(optionNameValuePair[1]);
	}

	// a lot of things can go wrong, trying to set the value of an option using another option
	public void updateOption(Option option)throws Exception{
		getOptionWithName(option.getName()).setValue(option.getValue());
	}

	// an option may request the default of the option
	// which should be the actual value that the parent exposes for that option
	// I guess we should do that by index
	public Object getDefault(int optionIndex){
		// either the value as stored in the parent, or otherwise the default stored in the OPTIONINFOS!!
		return(parent!=null?parent.get(optionIndex).getValue():OPTIONINFOS.get(optionIndex).getDefault());
	}

	private Vector<String> producedTextLines=null;
	public void produceTextLines()throws Exception{
		producedTextLines=new Vector<String>();
		for(Option option:this)if(!producedTextLines.add(option.toString()))throw new Exception("Failed to return option '"+option.getName()+"'.");
		Utils.setInfo(host,"Number of non-default options: "+producedTextLines.size()+".");
	}
	public String[] getProducedTextLines(){
		return(producedTextLines.isEmpty()?new String[]{}:(String[])producedTextLines.toArray(new String[producedTextLines.size()]));
	}
	public void setTextLines(String[] textLines){
		// we can't actually remove options, so the only thing we can do is replace the value
		for(String textLine:textLines)
			try{
				int colonpos=textLine.indexOf(":"); // additional colons might be present (e.g. in JavaScriptOption things)
				getOptionWithName(textLine.substring(0,colonpos)).parseValue(textLine.substring(colonpos+1));
			}catch(Exception ex){
				Utils.setInfo(host,"ERROR: '"+ex.getLocalizedMessage()+"' parsing option line '"+textLine+"'.");
			}
	}
	public OptionCollection(Object host){
		this.host=host;
		// very convenient
		OptionInfo optionInfo;
		for(int optionIndex=0;optionIndex<OPTIONINFOS.size();optionIndex++){
			optionInfo=OPTIONINFOS.get(optionIndex);
			if(optionInfo.getDefault() instanceof JavaScriptObject)
				super.add(new JavaScriptObjectOption(this,optionIndex));
			else super.add(new Option(this,optionIndex));
		}
	}
	// TODO there must be a way to improve this!!
	public Option getNonDefaultOption(Option option){
		if(option==null||!option.isDefault())return option; // the option passed
		if(parent==null)return null;
		return parent.getNonDefaultOption(parent.getOptionWithName(option.getName()));
	}
	public Vector<String> getTextLines()throws Exception{
		Vector<String> textLines=new Vector<String>();
		for(Option option:this){
			Option nonDefaultOption=getNonDefaultOption(option);
			if(nonDefaultOption!=null&&!textLines.add(nonDefaultOption.toString()))throw new Exception("Failed to return option '"+option.getName()+"'.");
		}
		Utils.setInfo(host,"Number of non-default options: "+textLines.size()+".");
		return textLines;
	}

	public String toString(){return "Option collection"+(host!=null?" of '"+host.toString()+"'":"");}

}

