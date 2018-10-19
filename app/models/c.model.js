// MDH@8Oct2018: defines a c table

const mongoose=require('mongoose');
const mongooseLong=require('mongoose-long');

const cSchema=mongoose.Schema({
				a:{type:Mixed},
				b:{type:Mixed},
				c:{type:Number,min:1.0,max:0.0},
			});

module.exports=mongoose.model('C',cSchema);
