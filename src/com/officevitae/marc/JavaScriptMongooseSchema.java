package com.officevitae.marc;

import java.io.File;
import java.util.Stack;
import java.util.Vector;

/**
 * MDH@21OCT2018:
 * a JavaScript Mongoose Schema is a Mongoose schema associated with a model JavaScript file in which it is defined
 *
 */
public class JavaScriptMongooseSchema extends MongooseSchema{

	// the text to read now comes from the associated JavaScript file not the MSD text file
	protected String getAssociatedFilename(){
		return "."+File.separator+"app"+File.separator+"models"+File.separator+getName()+".model.js";
	}

	// obviously the parent needs to be a JavaScriptMongooseSchema as well!!!
	public JavaScriptMongooseSchema(String name,JavaScriptMongooseSchema parentSchema){
		super(name,parentSchema);
	}

	private class TokenAdditionException extends Exception{
		private IToken token;
		public TokenAdditionException(IToken token){
			super("Failed to add a token.");
			this.token=token;
		}
	}

	// parsing from and to text lines (from a JavaScriptFile
	// a single token can span multiple lines, like a comment
	// but it's probably easier to keep the lines itself intact
	// whitespace is everything that separates the keyword identifiers and the like so typically blanks, tabs, separator characters, form feeds,
	public boolean isWhitespace(char c){return(c==' '||c=='\t'||c=='\r'||c=='\n');} // NOTE the last two are quite unlikely as they are typically used in the line separator itself...
	// WORD tokens are typically separated by whitespace to separate them from the rest
	public enum TokenType{
		EMPTY,WHITESPACE,NEWLINE,SINGLECHARACTER,WORD,ASSIGNMENTOPERATOR,SINGLELINECOMMENT,MULTILINECOMMENT,DOUBLEQUOTEDLITERAL,SINGLEQUOTEDLITERAL,SCHEMANAME,SCHEMACONSTRUCTORCALL,FIELDNAME,OPTIONNAME,FIELDTYPE,FIELDPROPERTYNAME,FIELDPROPERTYVALUE;
	}
	// at its minimum a token is of a certain type
	private interface IToken{
		boolean isEmpty();
		String getText();
	}
	// an atomic IToken has a token type, whereas a token sequence itself does not
	private interface ISimpleToken extends IToken{
		TokenType getTokenType();
	}
	// I guess we may construct a SchemaToken for every schema definition in the text lines
	private interface ITokenSequence extends IToken{
		IToken[] getTokens();
		void outputTokens(String prefix);
	}
	private class TokenSequence extends Vector<IToken> implements ITokenSequence{
		private String name;
		public TokenSequence(String name){
			this.name=name;
		}
		public boolean addToken(IToken token){
			Utils.consoleprintln("Token to add: "+token.toString()+".");
			// do NOT add 'empty' tokens
			return(token.isEmpty()||super.add(token));
		}
		public String getText(){
			StringBuilder text=new StringBuilder();
			for(IToken token:this)text.append(token.getText());
			return text.toString();
		}
		public IToken[] getTokens(){return(super.isEmpty()?new IToken[]{}:(IToken[])toArray(new IToken[this.size()]));}
		public void outputTokens(String prefix){
			Utils.consoleprintln(prefix+name+":");
			for(IToken token:this){
				if(token instanceof SimpleToken)
					Utils.consoleprintln(prefix+"\t"+((SimpleToken)token).getTokenType().toString()+":'"+token.getText()+"'");
				else if(token instanceof TokenSequence)
					((TokenSequence)token).outputTokens(prefix+"\t");
			}
		}
		// utility method to location the last word in front of the given index skipping
		public int getLastTokenIndex(int index,TokenType tokenTypeToFind){
			IToken token;
			TokenType tokenType;
			while(--index>=0){
				token=super.get(index);
				if(token instanceof SimpleToken){
					tokenType=((SimpleToken)token).getTokenType();
					Utils.consoleprintln("Checking token #"+index+" with significant text '"+((SimpleToken)token).getSignificantText()+"' of type "+tokenType.toString()+".");
					if(tokenType.equals(tokenTypeToFind))break; // best to check for the type we're looking for immediately (before skipping anything that is whitespacish
					if(tokenType.equals(TokenType.NEWLINE))continue;
					if(tokenType.equals(TokenType.WHITESPACE))continue;
					if(tokenType.equals(TokenType.MULTILINECOMMENT))continue;
					if(tokenType.equals(TokenType.SINGLELINECOMMENT))continue;
				}
				// either not a simple token or not of the right type
				index=-1;
			}
			// not found
			return index;
		}
		public String getName(){return name;}
		public String toString(){return name+":"+getText();}
	}
	// we're keeping a stack of active token sequences the last one is the active one, eventually every token sequence will end up in the main token sequence...
	private Stack<TokenSequence> tokenSequenceStack=null;
	private class SimpleToken implements ISimpleToken{
		private StringBuilder text=new StringBuilder();
		private TokenType tokenType=TokenType.EMPTY;
		// anything without text which is NOT a newline is also empty!!!
		public boolean isEmpty(){return TokenType.EMPTY.equals(tokenType)||(!TokenType.NEWLINE.equals(tokenType)&&text.length()==0);}
		public SimpleToken(){}
		public SimpleToken setTokenType(TokenType tokenType){this.tokenType=tokenType;return this;}
		public TokenType getTokenType(){return tokenType;}
		private int whitespacepos=-1; // the first character that is considered a whitespace!!
		public SimpleToken(TokenType tokenType){this.tokenType=tokenType;}
		public SimpleToken add(char c,boolean whitespace) throws Exception{
			// let's NOT allow nonwhitespace behind whitespace in the same token
			if(whitespace){
				if(whitespacepos<0)whitespacepos=text.length(); // the first position of whitespace
				if(tokenType.equals(TokenType.EMPTY))setTokenType(TokenType.WHITESPACE); // apparently entirely WHITESPACE
			}else{
				if(whitespacepos>=0){
					if(!tokenSequenceStack.peek().addToken(this))throw new TokenAdditionException(this);
					return new SimpleToken().add(c,false); // if the character represents non-whitespace and we're in whitespace, a new word starts which we return, so the result of add should always be the new token
				}
				if(tokenType.equals(TokenType.EMPTY))setTokenType(TokenType.WORD);
			}
			text.append(c);
			return this;
		}
		public String toString(){return tokenType.toString()+"=["+getText()+"]";}
		public boolean ended(){return whitespacepos>0;} // means you cannot add nonwhitespace characters
		public String getSignificantText(){return(whitespacepos>0?text.substring(0,whitespacepos):(whitespacepos<0?text.toString():""));}
		public void add(String s){text.append(s);}
		public char pop(){
			int newl=text.length()-1;
			if(whitespacepos==newl)whitespacepos=-1; // apparently the first whitespace character is popped off
			char c=text.charAt(newl);
			text.setLength(newl);
			return c;
		}
		public String getText(){return text.toString();}
	}
	private static final String SINGLE_CHARACTER_TOKEN_CHARACTERS="=:;{}(),"; // characters considered to be single character words, assignment operator is 0, end of statement is 1
	// a lot of characters should remain single character tokens like =, {, }, ( and ), and comma (,) as well so it will be easy to find them
	private class SingleCharacterToken extends SimpleToken{
		public SingleCharacterToken(char c){
			super.tokenType=TokenType.SINGLECHARACTER;
			super.whitespacepos=1; // if there's any whitespace it will be directly behind the character
			super.text.append(c);
		}
		public char getChar(){return super.text.charAt(0);}
		public String getSignificantText(){return super.text.substring(0,1);}
	}
	private TokenSequence newTokenSequence(String name){
		Utils.consoleprintln("New token sequence: '"+name+"'.");
		return new TokenSequence(name);
	}
	private TokenSequence poppedTokenSequence(){
		TokenSequence popped=tokenSequenceStack.pop();
		Utils.consoleprintln("Popped token sequence '"+popped.toString()+"'.");
		return popped;
	}
	@Override
	public void setTextLines(String[] textLines) throws Exception{
		if(textLines==null)return; // no change apparently
		tokenSequenceStack=new Stack<>();
		tokenSequenceStack.push(newTokenSequence("Schema")); // at the top we have the Schema token sequence, which is getting build
		fieldCollection.clear();
		removeAllSubSchemas();
		// we start with a whitespace current token, we do not add a token until it's finished!!!
		TokenType tokenType=null; // initialize the current token to add as a WHITESPACE token (albeit empty now)
		SimpleToken token=new SimpleToken(); // start with empty token
		IToken sequenceToken;
		boolean tokenEnded=false; // keep track of whether the current token ended or not
		// we're going to try to identify mongoose.Schema definitions format: const|var <schemaname> = mongoose.Schema ( { where blanks can be whitespace
		// I suppose this means we have to identify out of comment and out of string literal text mongoose.Schema
		int l,singleCharacterTokenIndex;
		char c=' ',prevc=' ';
		String currentSchemaName=null,currentFieldName=null,currentOptionName=null; // keeping track of the current schema name and/or current field name and/or current option name
		int currentSchemaConstructorCallArgumentIndex=0;
		String newTokenText=null; // this is the text to set in a new token when the current token ends
		TokenType newTokenType=null;
		int jsobjects=0; // count how many JavaScript objects we encountered...
		for(String textLine:textLines){
			// a single text line might be processed in multiple pieces...
			l=textLine.length();
			while(l>0){
				Utils.consoleprintln("Processing '"+textLine+"'.");
				// every line can continue the current token of course
				// in a multi line comment we have to continue until we come across */ in the line
				tokenType=token.getTokenType();
				if(tokenType.equals(TokenType.MULTILINECOMMENT)){
					// if the length of the line is 1 nothing can end the multi line comment
					if(l>1){
						// locate the end of the multi line comment (if any)
						int i=1;
						for(;i<l;i++)if(textLine.charAt(i-1)=='*'&&textLine.charAt(i)=='/')break;
						i++;
						if(i<=l){ // end of multi line comment found
							token.add(textLine.substring(0,i)); // the entire text including the */ at the end
							textLine=textLine.substring(i);
							// ends the current multiline token
							newTokenText="";
							newTokenType=TokenType.EMPTY;
						}else
							token.add(textLine);
						l-=i;
					}else // a single character
						token.add(textLine); // multiline comments cannot contain 'whitespace'
				}else if(tokenType.equals(TokenType.DOUBLEQUOTEDLITERAL)){
					// a double quote not behind a \ will end the double quoted string
					int i=0;
					for(;i<l;i++)if(textLine.charAt(i)=='"'&&(i==0||textLine.charAt(i-1)!='\\'))break;
					i++;
					if(i<=l){ // end of double quoted string literal found
						token.add(textLine.substring(0,i)); // the entire text including the " at the end
						Utils.consoleprintln("Double quoted literal: '"+token.getText()+"'.");
						textLine=textLine.substring(i);
						newTokenType=TokenType.EMPTY;
						newTokenText="";
					}else
						token.add(textLine);
					l-=i;
				}else if(tokenType.equals(TokenType.SINGLEQUOTEDLITERAL)){
					int i=0;
					for(;i<l;i++)if(textLine.charAt(i)=='\''&&(i==0||textLine.charAt(i-1)!='\\'))break; // not an escaped single quote
					i++;
					if(i<=l){ // end of double quoted string literal found
						token.add(textLine.substring(0,i)); // the entire text including the ' at the end
						Utils.consoleprintln("Single quoted literal: \""+token.getText()+"\".");
						textLine=textLine.substring(i);
						newTokenType=TokenType.EMPTY;
						newTokenText="";
					}else
						token.add(textLine);
					l-=i;
				}else{ // any other (code) token
					// meaning: we're looking for an assignment operator which is a single = sign not behind < or > or ! or in front of another equals sign
					// meaning: we have to identify when we enter a multiline comment, single or double quoted string literal
					int i=0;
					for(;i<l;i++){
						c=textLine.charAt(i);
						// we're going to exit when somehow we enter a multiline comment, single or double quoted string literal
						if(c=='*'&&(i>0&&prevc=='/')){newTokenText=token.pop()+"*";newTokenType=TokenType.MULTILINECOMMENT;break;}
						if(c=='"'){newTokenType=TokenType.DOUBLEQUOTEDLITERAL;newTokenText="\"";break;}
						if(c=='\''){newTokenType=TokenType.SINGLEQUOTEDLITERAL;newTokenText="'";break;}
						if(c=='/'&&(i>0&&prevc=='/')){Utils.consoleprintln("\tSingle comment found at position "+i+".");newTokenType=TokenType.SINGLELINECOMMENT;newTokenText=token.pop()+textLine.substring(i);i=l;break;}
						// if we get here c is not inside a comment or string literal therefore some code character
						// it this character is a single character token character we do not put it in the current token at all unless the current token is empty???

						if(!isWhitespace(c)){ // not a whitespace character!!!
							singleCharacterTokenIndex=SINGLE_CHARACTER_TOKEN_CHARACTERS.indexOf(c);
							if(singleCharacterTokenIndex>=0){ // one of the single character token characters, of which the first one (an equal sign) is especially important to identify...
								Utils.consoleprintln("Single character ("+c+") token index: "+singleCharacterTokenIndex+" at level "+tokenSequenceStack.size()+" encountered.");
								// ok, still not in a comment or string literal, or end of line comment, so we're looking for the equal sign
								if(singleCharacterTokenIndex>0||(i==0||(prevc!='<'&&prevc!='>'&&prevc!='!'&&prevc!='+'&&prevc!='-'&&prevc!='*'&&prevc!='/'&&prevc!='%'&&(i+1>=l||textLine.charAt(i+1)!='=')))){ // assignment equal sign
									// we're only interested in what's in front of the equal sign (the name of the submenu) when the constructor mongoose.Schema is getting called...
									// unfortunately we do not know that right now, so the only thing we can do is create the assignment token????
									// basically we could start an assignment token that ends with the semicolon at the end of the assignment, and analyze that
									// for that we would need the variable being assigned to and the expression being assigned!!
									// we're not interested in var or const or let
									// { and } are important tokens and we want these to start or end a new token sequence
									// 1. whatever's in front of the special character goes into the current token sequence
									if(!tokenSequenceStack.peek().addToken(token))throw new TokenAdditionException(token);
									token=new SingleCharacterToken(c);
									switch(singleCharacterTokenIndex){
										case 0: // =
											token.setTokenType(TokenType.ASSIGNMENTOPERATOR);
											break;
										case 1: // : typically separates the field name from the field definition object (or type) but only at level 2
											if(currentSchemaName!=null){
												if(tokenSequenceStack.size()==2){
													TokenSequence tokenSequence=tokenSequenceStack.peek();
													int nameTokenIndex=tokenSequence.getLastTokenIndex(tokenSequence.size(),TokenType.WORD);
													if(nameTokenIndex>=0){
														sequenceToken=tokenSequence.get(nameTokenIndex);
														if(currentSchemaConstructorCallArgumentIndex==1){
															currentFieldName=((SimpleToken)sequenceToken).getSignificantText();
															((SimpleToken)sequenceToken).setTokenType(TokenType.FIELDNAME); // tag as field name!!
														}else if(currentSchemaConstructorCallArgumentIndex==2){ // an option
															currentOptionName=((SimpleToken)sequenceToken).getSignificantText();
															((SimpleToken)sequenceToken).setTokenType(TokenType.OPTIONNAME); // tag as field name!!
														}
													}else
														Utils.consoleprintln("ERROR: Missing attribute name.");
												}
											}
										case 3: // {
											boolean newSchemaConstructorCallArgument=false;
											if(currentSchemaName!=null){ // schema constructor call argument
												if(tokenSequenceStack.size()==1){ // we're at the right level
													currentSchemaConstructorCallArgumentIndex+=1;
													switch(currentSchemaConstructorCallArgumentIndex){
														case 1:
															tokenSequenceStack.push(newTokenSequence("Field definitions of "+currentSchemaName));
															newSchemaConstructorCallArgument=true;
															break;
														case 2:
															tokenSequenceStack.push(newTokenSequence("Options of "+currentSchemaName));
															newSchemaConstructorCallArgument=true;
															break;
													}
												}else if(tokenSequenceStack.size()==2){
													if(currentSchemaConstructorCallArgumentIndex==1){
														if(currentFieldName!=null){
															tokenSequenceStack.push(newTokenSequence("Field property definitions of "+currentFieldName));
															newSchemaConstructorCallArgument=true;
														}
													}else if(currentSchemaConstructorCallArgumentIndex==2){
														if(currentOptionName!=null){
															tokenSequenceStack.push(newTokenSequence("Option property definitions of "+currentOptionName));
															newSchemaConstructorCallArgument=true;
														}
													}
												}
											}
											if(!newSchemaConstructorCallArgument)tokenSequenceStack.push(newTokenSequence(String.valueOf(++jsobjects)));
											break;
										case 4: // }
											TokenSequence tokenSequence=poppedTokenSequence(); // ends the current JS object token sequence
											if(!tokenSequence.addToken(token)) throw new TokenAdditionException(token); // we push the } onto the new stack
											token=new SimpleToken(); // start a new token (which might end up containing the whitespace following the } single character token)
											tokenSequenceStack.peek().addToken(tokenSequence); // whatever's on top now should received the popped off token sequence as a whole!!
											break;
										case 5: // (
											// might be a schema constructor call, which we now check for, and we want to put all tokens starting with the name of the schema instance in a separate token sequence
											// basically this is difficult because that would mean knowing when the schema definition ends to pop the subschema off the stack
											// so instead we go back and change the token types so the subschema is easily found!!!
											// format: <schemaname>...=...new...mongoose.Schema|Schema...( so basically we're looking for a WORD in front of the (
											// because whitespace can be part of the word we only need to skip newlines, and comments
											// nevertheless all the tokens need to be on a single token sequence, so that's easy
											if(tokenSequenceStack.size()==1){ // on the main token sequence (to look for subschema definitions)
												Utils.consoleprintln("Checking for a Mongoose schema constructor call.");
												tokenSequence=tokenSequenceStack.peek();
												int functionNameTokenIndex=tokenSequence.getLastTokenIndex(tokenSequence.size(),TokenType.WORD);
												if(functionNameTokenIndex>2){
													String functionName=((SimpleToken)tokenSequence.get(functionNameTokenIndex)).getSignificantText();
													if(functionName.equals("Schema")||functionName.equals("mongoose.Schema")){
														int newTokenIndex=tokenSequence.getLastTokenIndex(functionNameTokenIndex,TokenType.WORD);
														if(newTokenIndex>1){
															String newName=((SimpleToken)tokenSequence.get(newTokenIndex)).getSignificantText();
															if(newName.equals("new")){
																int assignmentTokenIndex=tokenSequence.getLastTokenIndex(newTokenIndex,TokenType.ASSIGNMENTOPERATOR);
																if(assignmentTokenIndex>0){
																	int schemaNameTokenIndex=tokenSequence.getLastTokenIndex(assignmentTokenIndex,TokenType.WORD);
																	if(schemaNameTokenIndex>=0){
																		SimpleToken schemaToken=(SimpleToken)tokenSequence.get(schemaNameTokenIndex);
																		schemaToken.setTokenType(TokenType.SCHEMANAME);
																		currentSchemaName=schemaToken.getSignificantText();
																		currentSchemaConstructorCallArgumentIndex=0; // keep track of which argument we are dealing with in the constructor call
																		Utils.consoleprintln("Constructor call to instantiate schema '"+currentSchemaName+"' detected.");
																		token.setTokenType(TokenType.SCHEMACONSTRUCTORCALL);
																	}
																}
															}else
																Utils.consoleprintln("WARNING: Assumed "+functionName+" constructor called without new keyword!");
														}
													}
												}
											}
											break;
										case 6: // )
											if(tokenSequenceStack.size()==1){ // at the top level
												if(currentSchemaName!=null){ // in a schema constructor argument list
													if(currentSchemaConstructorCallArgumentIndex==0)Utils.consoleprintln("WARNING: No fields and options defined in the creation of schema '"+currentSchemaName+"'.");else
													if(currentSchemaConstructorCallArgumentIndex==1)Utils.consoleprintln("WARNING: No options defined in the creation of schema '"+currentSchemaName+"'.");
													Utils.consoleprintln("End of the creation of schema '"+currentSchemaName+"'.");
													currentSchemaName=null;
												}
												break;
											}
											// if we get here we do the same as what we do when we encounter a , therefore there's NO break; here!!!!
										case 7: // ,
											// a comma behind the definition of the type of a field ends the current field name (definition)
											if(tokenSequenceStack.size()==2){
												if(currentFieldName!=null){
													tokenSequence=tokenSequenceStack.peek();
													// if the previous token is composite
													int lastWordTokenIndex=tokenSequence.getLastTokenIndex(tokenSequence.size(),TokenType.WORD);
													if(lastWordTokenIndex>=0)((SimpleToken)tokenSequence.get(lastWordTokenIndex)).setTokenType(TokenType.FIELDTYPE);
													currentFieldName=null;
												}
											}
											break;
									}
									// there might still be whitespace coming, so we won't end the single character token now
								}else // an equal sign which is not an assigment operator
									token=token.add(c,false);
							}else
								token=token.add(c,false);
						}else
							// whitespace is always appended to the current token
							token=token.add(c,true);
						// does the current character end the current token?????
						prevc=c;
					}
					// i is the last character processed
					i++;
					l-=i;
					if(l>0)textLine=textLine.substring(i); // something left to process!!!
				}
				if(newTokenText!=null){
					if(!tokenSequenceStack.peek().addToken(token))throw new TokenAdditionException(token);
					token=new SimpleToken(newTokenType);
					if(!newTokenText.isEmpty())token.add(newTokenText);
					newTokenText=null;
					tokenType=newTokenType;
				}
			}
			// the end of the line ends all tokens except multiline comment or string literals (although the latter is debatable as that would require a string continuation character at the end of the line theoretically)
			if(token!=null)if(!tokenSequenceStack.peek().addToken(token))throw new TokenAdditionException(token);
			if(!tokenSequenceStack.peek().addToken(new SimpleToken(TokenType.NEWLINE)))throw new TokenAdditionException(token); // No need to actually store the line separator
			if(tokenType==TokenType.MULTILINECOMMENT||tokenType==TokenType.DOUBLEQUOTEDLITERAL||tokenType==TokenType.SINGLEQUOTEDLITERAL)
				token=new SimpleToken(tokenType); // continue with the multiline comment or string literal!!
			else // start with a new empty token
				token=new SimpleToken();
		}
		Utils.consoleprintln("Outputting JavaScript tokens in '"+getName()+"'.");
		if(tokenSequenceStack.size()!=1)Utils.consoleprintln("ERROR: The token sequence stack does not contain a single element as it should: it contains "+tokenSequenceStack.size()+" token sequences!");
		tokenSequenceStack.firstElement().outputTokens("");
	}
	/* example:
	const cSchema=mongoose.Schema({
				a:{type:Mixed},
				b:{type:Mixed},
				c:{type:Number,min:1.0,max:0.0},
			});
	 */
	private String getTokenText(IToken token){
		if(token instanceof SimpleToken)return((SimpleToken)token).getText();
		if(token instanceof ITokenSequence){
			StringBuilder tokenText=new StringBuilder();
			for(IToken subToken:((ITokenSequence)token).getTokens())tokenText.append(getTokenText(subToken));
			return tokenText.toString();
		}
		return null; // some unknown token thing
	}
	@Override
	public String[] getTextLines(){
		Vector<String> textLines=new Vector<String>();
		// we should iterate over the tokens and compose the lines to output on the go
		StringBuilder textLine=new StringBuilder(); // the ongoing text line being build
		// technically we could flatten the token until only simple tokens remain
		String tokenText;
		for(IToken token:tokenSequenceStack.peek()){
			if(token instanceof SimpleToken&&((SimpleToken)token).getTokenType().equals(TokenType.NEWLINE)){
				textLines.add(textLine.toString());
				textLine.setLength(0);
			}else
				textLine.append(getTokenText(token));
		}
		// something left to add??
		if(textLine.length()>0)textLines.add(textLine.toString());
		// TODO parse the tokens back (inserting the schema design parameters for field definitions and subschema's
		return (textLines.isEmpty()?new String[]{}:(String[])textLines.toArray(new String[textLines.size()]));
	}

}
