import json

with open('Postman_Testing/Infosys_Project_API_Collection.json', 'r', encoding='utf-8') as f:
    col = json.load(f)

print('Top level keys:', list(col.keys()))
print('Number of top-level items (folders):', len(col['item']))
for i, folder in enumerate(col['item']):
    print(f"Folder {i}: name='{folder.get('name')}', count={len(folder.get('item', []))}")
    for j, req in enumerate(folder.get('item', [])):
        if 'name' not in req:
            print(f"  ERR: req {j} missing name")
        if 'request' not in req:
            print(f"  ERR: req {j} missing request")
        r = req.get('request', {})
        if 'method' not in r:
            print(f"  ERR: req {j} missing method")
        if 'url' not in r:
            print(f"  ERR: req {j} missing url")

