+_id:ObjectId
+active:Boolean	+default=true
+alertcondition_id:ObjectId	+ref=alertcondition
+evaluation_changes:[ObjectId]
+report_message_format:String	trim	+minlength=1
+alertreceiver_ids:[ObjectId]
