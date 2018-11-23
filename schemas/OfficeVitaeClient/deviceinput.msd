$client database - deviceinput: stores sensordata from devices
+_id:ObjectId
+deviceid:Int32	required
+values:Map of Number	required
+devicetypeid:Int32
+deviceinput_id:ObjectId	+ref=deviceinput
