$Office Vitae database - sensortype: defines what a sensor measures and in what units
+_id:ObjectId
+name:String	required	+indextype=unique	trim	+minlength=1
+id:Int32	required	+indextype=unique
+label:String	-indextype=unique
