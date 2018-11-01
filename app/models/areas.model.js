/*
 * Generated by: Office Vitae Mongoose Schema Designer
 * At: 30 October 2018
 * Author: <Enter your name here>
 * Description: Name of area
 */

const mongoose=require('mongoose');
require('mongoose-long')(mongoose);

const areasSchema=mongoose.Schema({
                                   _id:mongoose.Schema.Types.ObjectId,
                                   name:{type:mongoose.Schema.Types.String,unique:true,required:true},
                                   label:{type:mongoose.Schema.Types.String,required:true},
                                   areatype_id:{type:mongoose.Schema.Types.ObjectId,ref:'areatypes'},
                                   timezone:{type:mongoose.Schema.Types.String,required:true},
                                   latitude:mongoose.Schema.Types.Number,
                                   longitude:mongoose.Schema.Types.Number,
                                   altitude:mongoose.Schema.Types.Number,
                                  });

module.exports=mongoose.model('AREAS',areasSchema);
