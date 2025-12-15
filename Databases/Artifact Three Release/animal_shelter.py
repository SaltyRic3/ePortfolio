from pymongo import MongoClient, errors
from datetime import datetime
import hashlib
import os
from typing import Optional, Dict, Any, List


class AnimalShelter(object):
    """CRUD operations and security features for Animal collection in MongoDB."""

    # -------------------------------------------------------------------------
    # Initialization / Connection
    # -------------------------------------------------------------------------
    def __init__(self, username: str, password: str, host: str, port: int):
        """
        Initialize connection to MongoDB using credentials and connection info.

        Parameters:
            username (str): MongoDB username
            password (str): MongoDB password
            host (str): MongoDB server address
            port (int): MongoDB port number
        """
        # Database and Collection names are hard-coded as per assignment instructions
        DB = "AAC"
        ANIMALS_COL = "animals"
        USERS_COL = "users"
        AUDIT_COL = "audit_log"

        try:
            # Create a MongoDB client using the connection string
            self.client = MongoClient(
                f"mongodb://{username}:{password}@{host}:{port}/{DB}",
                serverSelectionTimeoutMS=5000
            )

            # Force a ping to verify connectivity and authentication
            self.client.admin.command("ping")

        except errors.PyMongoError as e:
            raise ConnectionError(f"Failed to connect to MongoDB: {e}")

        # Access the database and collections
        self.database = self.client[DB]
        self.collection = self.database[ANIMALS_COL]
        self.users_collection = self.database[USERS_COL]
        self.audit_collection = self.database[AUDIT_COL]

        # Tracks the currently authenticated application user (not the Mongo user)
        self.current_user: Optional[str] = None

    # -------------------------------------------------------------------------
    # Password Hashing Helpers (Application-Level User Accounts)
    # -------------------------------------------------------------------------
    @staticmethod
    def _hash_password(password: str, salt: Optional[str] = None) -> Dict[str, str]:
        """
        Create a salted SHA-256 hash of the password.

        Returns:
            dict with 'salt' and 'hash' keys.
        """
        if salt is None:
            # 16 bytes of random salt, hex-encoded
            salt = os.urandom(16).hex()
        salted = (salt + password).encode("utf-8")
        hashed = hashlib.sha256(salted).hexdigest()
        return {"salt": salt, "hash": hashed}

    @staticmethod
    def _verify_password(password: str, salt: str, expected_hash: str) -> bool:
        """
        Verify a password against a stored salt and hash.
        """
        salt_hash = AnimalShelter._hash_password(password, salt)
        return salt_hash["hash"] == expected_hash

    # -------------------------------------------------------------------------
    # Application User Management / Authentication
    # -------------------------------------------------------------------------
    def create_app_user(self, username: str, password: str, roles: Optional[List[str]] = None) -> bool:
        """
        Create an application-level user for the CRUD module.

        Parameters:
            username (str): Desired username
            password (str): Plain-text password (will be hashed)
            roles (list[str]): Optional list of roles such as ["admin"]

        Returns:
            bool: True if created successfully, False if user already exists or insert fails.
        """
        if not username or not password:
            raise ValueError("Username and password are required")

        if roles is None:
            roles = ["user"]

        # Check for existing user
        if self.users_collection.find_one({"username": username}):
            print("User already exists.")
            return False

        pw_data = self._hash_password(password)
        doc = {
            "username": username,
            "password_hash": pw_data["hash"],
            "salt": pw_data["salt"],
            "roles": roles,
            "created_at": datetime.utcnow(),
        }

        try:
            result = self.users_collection.insert_one(doc)
            return result.acknowledged
        except Exception as e:
            print(f"Error creating app user: {e}")
            return False

    def authenticate_user(self, username: str, password: str) -> bool:
        """
        Authenticate an application-level user.

        On success, sets self.current_user.

        Returns:
            bool: True if authentication succeeds, False otherwise.
        """
        try:
            user = self.users_collection.find_one({"username": username})
            if not user:
                print("Invalid username or password.")
                return False

            if not self._verify_password(password, user["salt"], user["password_hash"]):
                print("Invalid username or password.")
                return False

            # Authentication succeeded
            self.current_user = username
            return True

        except Exception as e:
            print(f"Error during authentication: {e}")
            return False

    # -------------------------------------------------------------------------
    # Input Validation / Sanitization
    # -------------------------------------------------------------------------
    @staticmethod
    def _validate_document(doc: Dict[str, Any]) -> None:
        """
        Basic validation to help protect against injection-style misuse and invalid keys.

        Rules:
            - Document must be a dict.
            - Field names cannot start with '$'.
            - Field names cannot contain '.' (disallowed by MongoDB).
        """
        if not isinstance(doc, dict):
            raise ValueError("Document must be a dictionary.")

        for key in doc.keys():
            if not isinstance(key, str):
                raise ValueError("All field names must be strings.")
            if key.startswith("$"):
                # Prevent inserting documents that try to inject operators
                raise ValueError("Field names may not start with '$' to prevent injection attacks.")
            if "." in key:
                raise ValueError("Field names may not contain '.' per MongoDB restrictions.")

    # -------------------------------------------------------------------------
    # Audit Logging
    # -------------------------------------------------------------------------
    def _log_action(
        self,
        action: str,
        query: Optional[Dict[str, Any]] = None,
        data: Optional[Any] = None,
        result_count: Optional[int] = None,
        success: bool = True,
        error_message: Optional[str] = None,
    ) -> None:
        """
        Store an audit log entry in the audit_log collection.

        Parameters:
            action (str): CRUD operation name ("create", "read", "update", "delete", "aggregate")
            query (dict): Query/filter used
            data (Any): Data inserted/updated or other payload
            result_count (int): Number of documents affected or returned
            success (bool): Whether the operation succeeded
            error_message (str): Error details if any
        """
        try:
            log_entry = {
                "timestamp": datetime.utcnow(),
                "action": action,
                "user": self.current_user or "anonymous",
                "query": query,
                "data": data,
                "result_count": result_count,
                "success": success,
                "error": error_message,
            }
            self.audit_collection.insert_one(log_entry)
        except Exception as e:
            # Audit logging failure should not break main operation, but we report it.
            print(f"Failed to write audit log: {e}")

    # -------------------------------------------------------------------------
    # CRUD Operations (Enhanced)
    # -------------------------------------------------------------------------
    def create(self, data: Dict[str, Any]) -> bool:
        """
        Insert a document into the collection with validation and auditing.

        Parameters:
            data (dict): A dictionary representing the document to insert

        Returns:
            bool: True if insert was acknowledged, otherwise False
        """
        if not data:
            raise Exception("Nothing to save: data parameter is empty.")

        try:
            # Validate document keys to reduce injection risk
            self._validate_document(data)

            # Attempt to insert the document into the collection
            result = self.collection.insert_one(data)
            success = result.acknowledged
            self._log_action(
                action="create",
                query=None,
                data=data,
                result_count=1 if success else 0,
                success=success,
            )
            return success

        except Exception as e:
            print(f"An error occurred during insert: {e}")
            self._log_action(
                action="create",
                query=None,
                data=data,
                result_count=0,
                success=False,
                error_message=str(e),
            )
            return False

    def read(self, query: Dict[str, Any]) -> List[Dict[str, Any]]:
        """
        Query documents from the collection based on a filter.

        Parameters:
            query (dict): The filter used to find matching documents

        Returns:
            list: A list of matching documents or an empty list
        """
        try:
            results_cursor = self.collection.find(query)
            results = list(results_cursor)

            self._log_action(
                action="read",
                query=query,
                data=None,
                result_count=len(results),
                success=True,
            )
            return results

        except Exception as e:
            print(f"Error reading from database: {e}")
            self._log_action(
                action="read",
                query=query,
                data=None,
                result_count=0,
                success=False,
                error_message=str(e),
            )
            return []

    def update(self, query: Dict[str, Any], new_values: Dict[str, Any]) -> int:
        """
        Update document(s) in the collection based on a filter.

        Parameters:
            query (dict): Filter to find document(s) to update
            new_values (dict): Dictionary of fields to update

        Returns:
            int: Number of documents modified
        """
        if not new_values:
            raise ValueError("new_values cannot be empty.")

        try:
            # Validate only the fields being set to avoid operator injection in $set
            self._validate_document(new_values)

            result = self.collection.update_many(query, {"$set": new_values})
            modified_count = result.modified_count

            self._log_action(
                action="update",
                query=query,
                data=new_values,
                result_count=modified_count,
                success=True,
            )
            return modified_count

        except Exception as e:
            print(f"Error updating document(s): {e}")
            self._log_action(
                action="update",
                query=query,
                data=new_values,
                result_count=0,
                success=False,
                error_message=str(e),
            )
            return 0

    def delete(self, query: Dict[str, Any]) -> int:
        """
        Delete document(s) from the collection based on a filter.

        Parameters:
            query (dict): Filter to find document(s) to delete

        Returns:
            int: Number of documents deleted
        """
        try:
            result = self.collection.delete_many(query)
            deleted_count = result.deleted_count

            self._log_action(
                action="delete",
                query=query,
                data=None,
                result_count=deleted_count,
                success=True,
            )
            return deleted_count

        except Exception as e:
            print(f"Error deleting document(s): {e}")
            self._log_action(
                action="delete",
                query=query,
                data=None,
                result_count=0,
                success=False,
                error_message=str(e),
            )
            return 0

    # -------------------------------------------------------------------------
    # Aggregation / Dashboard Helpers
    # -------------------------------------------------------------------------
    def get_adoption_stats_by_outcome(self, match_filter: Optional[Dict[str, Any]] = None) -> List[Dict[str, Any]]:
        """
        Use an aggregation pipeline to compute adoption statistics grouped by outcome_type.

        Parameters:
            match_filter (dict): Optional filter to restrict results (e.g., {"animal_type": "Dog"})

        Returns:
            list of dicts: [{'_id': 'Adoption', 'count': 120}, ...]
        """
        pipeline = []
        if match_filter:
            pipeline.append({"$match": match_filter})

        pipeline.extend([
            {"$group": {"_id": "$outcome_type", "count": {"$sum": 1}}},
            {"$sort": {"count": -1}},
        ])

        try:
            results = list(self.collection.aggregate(pipeline))
            self._log_action(
                action="aggregate_outcome_stats",
                query=match_filter,
                data=None,
                result_count=len(results),
                success=True,
            )
            return results
        except Exception as e:
            print(f"Error running adoption stats aggregation: {e}")
            self._log_action(
                action="aggregate_outcome_stats",
                query=match_filter,
                data=None,
                result_count=0,
                success=False,
                error_message=str(e),
            )
            return []

    def get_adoption_stats_by_breed(self, match_filter: Optional[Dict[str, Any]] = None, limit: int = 10) -> List[Dict[str, Any]]:
        """
        Use an aggregation pipeline to compute adoption statistics grouped by breed.

        Parameters:
            match_filter (dict): Optional filter to restrict results (e.g., {"outcome_type": "Adoption"})
            limit (int): Max number of breeds to return, sorted by count descending.

        Returns:
            list of dicts: [{'_id': 'German Shepherd', 'count': 25}, ...]
        """
        pipeline = []
        if match_filter:
            pipeline.append({"$match": match_filter})

        pipeline.extend([
            {"$group": {"_id": "$breed", "count": {"$sum": 1}}},
            {"$sort": {"count": -1}},
            {"$limit": limit},
        ])

        try:
            results = list(self.collection.aggregate(pipeline))
            self._log_action(
                action="aggregate_breed_stats",
                query=match_filter,
                data={"limit": limit},
                result_count=len(results),
                success=True,
            )
            return results
        except Exception as e:
            print(f"Error running breed stats aggregation: {e}")
            self._log_action(
                action="aggregate_breed_stats",
                query=match_filter,
                data={"limit": limit},
                result_count=0,
                success=False,
                error_message=str(e),
            )
            return []
