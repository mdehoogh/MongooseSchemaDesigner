+_id:ObjectId
+name:String	required	+minlength=1
+active:Boolean	+default=true
+alert_id:ObjectId	required	+ref=alert
+devicetype_id:ObjectId	required	+ref=devicetype
+maplocation_ids:Array
+device_ids:Array
+evaluate_seconds:Int32	+default=0
+seconds_between_measurements:Int32	+default=0
+conditiontext:String	trim	+minlength=1
+lastevaluated_at:Date
+last_evaluation_result:Number
+evaluation_count:Long	+default=0
+last_alertmessage_id:ObjectId
