/*
 * Generated by: Office Vitae Mongoose Schema Designer
 * At: 5 november 2018
 * Author: <Enter your name here>
 */
const Client=require('../models/ovmsd.client.model.js);
exports.create=function(req,res){
	// Step 1. TODO check the received input (in req.body) and return a res.status(400).send({error:<error message>}); with an appropriate error message when something is wrong!
	
	// Step 2. Create a new object with the received data (properties of reg.body));
	const client=new Client({
				_id:null, // TODO replace null with the property from req.body that contains the required data
				name:null, // TODO replace null with the property from req.body that contains the required data
				// TODO insert initialization of optional fields (dbconnection) here
			});
	// Step 3. Save the newly created instance
	client.save().then(data=>{
		res.send(data); // or whatever else you want to send
	}).catch(err=>{
		res.status(500).send({
			error:err.message||'Some error occurred trying to add a client'
		});
	});
};
exports.findAll=function(req,res){
	Client.find()
	.then(clients => {
		res.send(clients);
	}).catch(err => {
		res.status(500).send({
			error: err.message || 'Some error occurred while retrieving clients.'
		});
	});
};
exports.findOne=function(req,res){
	Client.findById(req.params.clientId)
	.then(client => {
		if(!client){
			return res.status(404).send({
				error: 'Client with id ' + req.params.clientId + ' not found.'
			});
		}
		res.send(client);
	}).catch(err => {
		if(err.kind=='ObjectId'){
			return res.status(404).send({
				error: 'Client with id ' + req.params.clientId + ' not found.'
			});
		}
		return res.status(500).send({
			error: 'Failed to retrieve the client with id ' + req.params.clientId + '.'
		});
	});
};
exports.update=function(req,res){
	// Step 1. All required fields should all be present
	if(!req.body._id||!req.body.name){
		return res.status(400).send({
			error: 'One of the required fields for updating a client document is missing.'
		});
	}
	Client.findByIdAndUpdate(req.params.clientId,{$set:req.body})
	.then(client => {
		if(!client){
			return res.status(400).send({
				error:'Client with id ' + req.params.clientId + 'not found.'
			});
		}
		res.send({'updated':client});
	}).catch(err => {
		if(err.kind == 'ObjectId'){
			return res.send(404).send({
				error: 'client with id ' + req.params.clientId + ' not found.'
			});
		}
		return res.status(500).send({
			body: req.body,
			error: 'Failed to update the client with id ' + req.params.clientId + '.'
		});
	});
};
exports.delete=function(req,res){
	Client.findByIdAndRemove(req.params.clientId)
	.then(client => {
		if(!client){
			return res.status(400).send({
				error: 'Client with id ' + req.params.clientId + 'not found.'
			});
		}
		res.send({message:'client deleted successfully.'});
	}).catch(err => {
		if(err.kind == 'ObjectId'||err.name=='NotFound'){
			return res.send(404).send({
				error: 'client with id ' + req.params.clientId + ' not found.'
			});
		}
		return res.status(500).send({
			body: req.body,
			error: 'Failed to delete the client with id ' + req.params.clientId + '.'
		});
	});
};
