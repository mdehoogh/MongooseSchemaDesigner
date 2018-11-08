$client database - deviceinput: stores sensordata from devices
sensorvalue
	-_id:ObjectId
	+id:Int32
	+value:Number
+_id:ObjectId
+project_id:ObjectId	+ref=project
+maplocation_id:ObjectId	+ref=maplocation
+devicetypeid:Int32	required
+deviceid:ObjectId	required
+sensorvalues:Array
