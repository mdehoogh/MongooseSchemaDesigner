package com.officevitae.marc;

public interface IMongooseSchemaReader{

	// exposes a Mongoose Schema (like a producer of schemas i.e. a factory)
	MongooseSchema getMongooseSchema();

}
