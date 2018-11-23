+_id:ObjectId
+alertcondition_id:ObjectId	+ref=alertcondition
+evaluation_change:Mixed
+evaluation_time:String	+match=^([0-1]?\d|2[0-3])(?::([0-5]?\d))?(?::([0-5]?\d))?$
+alertreceiver_ids:[ObjectId]
