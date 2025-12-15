from pymongo import MongoClient

class AnimalShelter(object):
    """ CRUD operations for Animal collection in MongoDB """

    def __init__(self, username, password, host, port):
        """
        Initialize connection to MongoDB using credentials and connection info.
        Parameters:
            username (str): MongoDB username
            password (str): MongoDB password
            host (str): MongoDB server address
            port (int): MongoDB port number
        """
        # Database and Collection names are hard-coded as per assignment instructions
        DB = 'AAC'
        COL = 'animals'

        # Create a MongoDB client using the connection string
        self.client = MongoClient(f"mongodb://{username}:{password}@{host}:{port}/AAC")
        
        # Access the database
        self.database = self.client[DB]
        
        # Access the collection within the database
        self.collection = self.database[COL]

    def create(self, data):
        """
        Insert a document into the collection.
        Parameters:
            data (dict): A dictionary representing the document to insert
        Returns:
            bool: True if insert was acknowledged, otherwise False
        """
        if data:
            try:
                # Attempt to insert the document into the collection
                result = self.collection.insert_one(data)
                return result.acknowledged
            except Exception as e:
                # Print error if insertion fails
                print(f"An error occurred during insert: {e}")
                return False
        else:
            # Raise an error if the input data is None or empty
            raise Exception("Nothing to save: data parameter is empty.")

    def read(self, query):
        """
        Query documents from the collection based on a filter.
        Parameters:
            query (dict): The filter used to find matching documents
        Returns:
            list: A list of matching documents or an empty list
        """
        try:
            # Use find() to retrieve all documents matching the query
            results = self.collection.find(query)
            
            # Return the documents as a list
            return list(results)
        except Exception as e:
            # Print error if the query fails
            print(f"Error reading from database: {e}")
            return []
        
    def update(self, query, new_values):
        """
        Update document(s) in the collection based on a filter.
        Parameters:
            query (dict): Filter to find document(s) to update
            new_values (dict): Dictionary of fields to update
        Returns:
            int: Number of documents modified
        """
        try:
            result = self.collection.update_many(query, {'$set': new_values})
            return result.modified_count
        except Exception as e:
            print(f"Error updating document(s): {e}")
            return 0

    def delete(self, query):
        """
        Delete document(s) from the collection based on a filter.
        Parameters:
            query (dict): Filter to find document(s) to delete
        Returns:
            int: Number of documents deleted
        """
        try:
            result = self.collection.delete_many(query)
            return result.deleted_count
        except Exception as e:
            print(f"Error deleting document(s): {e}")
            return 0