import json
import jsonschema

with open('scratch/postman_v21_schema.json', 'r', encoding='utf-8') as f:
    schema = json.load(f)

with open('Postman_Testing/Infosys_Project_API_Collection.json', 'r', encoding='utf-8') as f:
    collection = json.load(f)

validator = jsonschema.Draft4Validator(schema)
errors = sorted(validator.iter_errors(collection), key=lambda e: e.path)

print(f"Total schema errors found: {len(errors)}")
for i, err in enumerate(errors[:10]):
    print(f"\nError {i+1}:")
    print("Path:", list(err.path))
    print("Message:", err.message)

