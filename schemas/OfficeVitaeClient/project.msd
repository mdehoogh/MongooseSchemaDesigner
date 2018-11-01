$client database - project: defines time-restricted projects
+_id:ObjectId	required	+indextype=unique
+name:String	required	trim	+minlength=1
+start_date:Date	+default=Date.now
+end_date:Date	+default=Date.now
