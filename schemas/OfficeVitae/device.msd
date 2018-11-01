$Office Vitae database - device: defines a device internally as a pair of an integer device type id and with type id and externally as a product
+_id:ObjectId	required	+indextype=unique
+devicetypeid:Int32
+deviceid:Int32
+product_id:ObjectId	+indextype=unique	+ref=product
