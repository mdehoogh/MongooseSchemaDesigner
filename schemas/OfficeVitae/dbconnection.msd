$Office Vitae database - dbconnection: defines Mongoose connection parameters
+_id:ObjectId	required	+indextype=unique
+server_url:String
+login_username:String
+login_password:String
+to_string:Mixed	virtual	+get=return 'mongodb://'+this.login_username+':'+this.login_password+'@'+this.server_url+'/';
