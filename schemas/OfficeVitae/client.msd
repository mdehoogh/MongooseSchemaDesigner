$Office Vitae database - client: defines a client by name and database
dbconnection
	+_id:ObjectId
	+server_url:String	required	+minlength=1
	+server_port:Int32	required
	+login_name:String	required	+minlength=1
	+login_password:String	required	+minlength=8
	+database:String	required	trim	+minlength=1
	+text:String	virtual	+get=return 'mongodb://'+this.login_name+':'+this.login_password+'@'+this.server_url+':'+String(this.serverport)+'/'+this.database;
+_id:ObjectId
+name:String	required	+minlength=1
+dbconnection:client_dbconnectionSchema
