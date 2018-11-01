$client database - deviceinput: stores sensordata from devices
+_id:ObjectId	required	+indextype=unique
+maplocation_id:ObjectId	+ref=maplocation
+deviceid:ObjectId	+ref=device
+project_id:ObjectId	+ref=project
+sensordata:Array
