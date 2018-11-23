+_id:ObjectId	required	+indextype=unique
+question_id:ObjectId	+ref=question
+language:String
+text:String
+answers:[ObjectId]	$if a multiple choice question the possible answers to the question for the user to select from	+validate=return(v.length!=1);
