$client database - answer: defines predefined answers to questions
+_id:ObjectId	required	+indextype=unique
+question_id:ObjectId	required	+ref=questions
+answer_index:Long	required
+text:String	trim	+minlength=1
