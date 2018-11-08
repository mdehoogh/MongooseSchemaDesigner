$unique integer sensor id, typically consisting of sensor type id and within sensor type id, as present in device type
+_id:ObjectId
+id:Int32	required	+indextype=unique
+name:String	required	trim	+minlength=1
+unit:String	trim	+minlength=1
+valid_minimum:Number
+valid_maximum:Number
