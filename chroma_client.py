import sys
import json
import os
import chromadb

def get_client():
    # Store chroma db inside workspace directory to keep it self-contained
    db_path = os.path.abspath("./chroma_db")
    return chromadb.PersistentClient(path=db_path)

def main():
    if len(sys.argv) < 3:
        print(json.dumps({"success": False, "error": "Invalid arguments. Usage: python chroma_client.py <action> <collection_name> [args]"}))
        return

    action = sys.argv[1].lower()
    collection_name = sys.argv[2]
    
    # Chroma requires collection names to be 3-63 chars and start/end with alphanumeric, only contain alphanumeric, hyphens, underscores
    # Let's clean the collection name just in case (e.g. replace UUID hyphens to underscores if needed, though UUID fits)
    collection_name = "pdf_" + collection_name.replace("-", "_")

    client = get_client()

    try:
        if action == "add":
            # Usage: python chroma_client.py add <pdf_id> <chunk_index> <embedding_json> <content>
            chunk_index = sys.argv[3]
            embedding_json = sys.argv[4]
            content = sys.argv[5]
            
            embedding = json.loads(embedding_json)
            collection = client.get_or_create_collection(name=collection_name)
            
            doc_id = f"chunk_{chunk_index}"
            collection.add(
                ids=[doc_id],
                embeddings=[embedding],
                documents=[content],
                metadatas=[{"chunkIndex": int(chunk_index)}]
            )
            print(json.dumps({"success": True}))

        elif action == "query":
            # Usage: python chroma_client.py query <pdf_id> <embedding_json> <limit>
            embedding_json = sys.argv[3]
            limit = int(sys.argv[4])
            
            embedding = json.loads(embedding_json)
            try:
                collection = client.get_collection(name=collection_name)
                results = collection.query(
                    query_embeddings=[embedding],
                    n_results=limit
                )
                
                # Format results to return to Java
                formatted = []
                if results and "ids" in results and results["ids"]:
                    ids = results["ids"][0]
                    documents = results["documents"][0]
                    metadatas = results["metadatas"][0]
                    distances = results["distances"][0] if "distances" in results else [0.0] * len(ids)
                    
                    for i in range(len(ids)):
                        formatted.append({
                            "id": ids[i],
                            "content": documents[i],
                            "chunkIndex": metadatas[i]["chunkIndex"],
                            "distance": distances[i]
                        })
                print(json.dumps({"success": True, "results": formatted}))
            except Exception as e:
                # If collection doesn't exist yet, return empty list
                print(json.dumps({"success": True, "results": []}))

        elif action == "delete":
            # Usage: python chroma_client.py delete <pdf_id>
            try:
                client.delete_collection(name=collection_name)
                print(json.dumps({"success": True}))
            except Exception as e:
                # Ignore if collection doesn't exist
                print(json.dumps({"success": True}))
        else:
            print(json.dumps({"success": False, "error": f"Unknown action: {action}"}))

    except Exception as e:
        print(json.dumps({"success": False, "error": str(e)}))

if __name__ == "__main__":
    main()
