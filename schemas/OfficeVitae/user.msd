$users
+_id:ObjectId
+id:Int32	required	+indextype=unique
+loginname:String	required	+indextype=unique	+minlength=1
+loginpassword:String
+conf:String
+name:String
+admin:Boolean	+default=false
+email:String
+status:Int32
+projectid:Int32
+roles:String
+dailypushnotifications:Int32	+default=0
+dailypushnotification_startdate:Date
+dailypushnotification_enddate:Date
+dailypushnotificationid:Int32
+nobeacon:Boolean	+default=false
+login_attempts:Long	+default=0
+login_timestamp:Date
+language:String	trim	+minlength=1	+values=EN,NL
+client_id:ObjectId	+ref=Client
