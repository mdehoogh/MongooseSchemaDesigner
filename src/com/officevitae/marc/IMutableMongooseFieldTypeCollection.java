package com.officevitae.marc;

import java.util.Set;

public interface IMutableMongooseFieldTypeCollection{

	interface Listener{
		void fieldTypeAdded(IMutableMongooseFieldTypeCollection fieldTypeCollection,IFieldType fieldType);
		void fieldTypeRemoved(IMutableMongooseFieldTypeCollection fieldTypeCollection,IFieldType fieldType);
		///////void fieldTypeCollectionChanged(IMutableMongooseFieldTypeCollection fieldTypeCollection);
	}

	Set<IFieldType> getFieldTypes(); // may expose the current list of field types
	void setListener(IMutableMongooseFieldTypeCollection.Listener listener);

}
