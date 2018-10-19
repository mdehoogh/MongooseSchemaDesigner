// MDH@25Sep2018: defines a a table

const mongoose=require('mongoose');
const mongooseLong=require('mongoose-long');

const aSchema=mongoose.Schema({
				a:{type:Number},
			});

// DO NOT FORGET TO initialize THE PLUGIN IN YOUR app.js WITH mongoose.connection!
const autoIncrement=require('mongoose-plugin-autoinc');
aSchema.plugin(autoIncrement,{model:'A',field:'a',startAt:3,incrementBy:1}););
const A=mongoose.connection.model('A',ASchema);
a=new A();
book.save(function(err){book.nextCount(function(err,count){book.resetCount(function(err,nextCount){});});});

module.exports=mongoose.model('A',aSchema);
