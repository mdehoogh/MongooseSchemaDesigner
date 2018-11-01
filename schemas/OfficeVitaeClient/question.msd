$client database - question: defines a question (text) in a questionnaire
+_id:ObjectId	required	+indextype=unique
+text:String	required	+minlength=1
+questionnaire_id:ObjectId	+ref=questionnaires
