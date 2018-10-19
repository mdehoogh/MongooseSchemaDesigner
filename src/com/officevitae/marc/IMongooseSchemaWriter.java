package com.officevitae.marc;

public interface IMongooseSchemaWriter {

	// produces a list of errors, warnings, notes whenever it is told to save a Mongoose schema
	String[] write(MongooseSchema mongooseSchema);

}
