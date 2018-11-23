+_id:ObjectId
+project_id:ObjectId	required	+ref=project
+devicetype_id:Int32	required
+device_ids:[ObjectId]	$list of offline devices	required	+validate=return(v.length>0);
+count:Long
