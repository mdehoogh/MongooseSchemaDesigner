const C=require('../models/c.model.js);
exports.create=function(req,res){
	// Step 1. Check the received input (in req.body) and return a res.status(400).send({error:<error message>}); with an appropriate error message when something is wrong!
	
	// Step 2. Create a new object with the received data (properties of reg.body));
	const c=new C({
				// TODO insert initialization of optional fields a b c here
			});
	// Step 3. Save the newly created instance
	user.save().then(data=>{
		res.send(data); // or whatever else you want to send
	}).catch(err=>{
		res.status(500).send({
			error:err.message||'Some error occurred trying to add a c.
		});
	});
}
exports.findAll=function(req,res){
}
exports.findOne=function(req,res){
}
exports.update=function(req,res){
}
exports.delete=function(req,res){
}
