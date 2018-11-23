$Office Vitae database - device: defines a device internally as a pair of an integer device type id and with type id and externally as a product
+_id:ObjectId
+devicetypeid:Int32	required
+deviceid:Int32	required
+product_id:ObjectId	+indextype=unique	+ref=product
+projectid:Int32
+maplocationid:Int32
+hardwareid:String
+lorasensordeviceid:Int32
