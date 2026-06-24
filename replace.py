import os

root_dir = "app/src/main/java/com/shishocompanion"

for subdir, dirs, files in os.walk(root_dir):
    for file in files:
        if file.endswith(".kt"):
            filepath = os.path.join(subdir, file)
            with open(filepath, 'r') as f:
                content = f.read()
            
            new_content = content.replace("com.example.myapplication", "com.shishocompanion")
            new_content = new_content.replace("MyApplicationTheme", "ShishoCompanionTheme")
            
            if new_content != content:
                with open(filepath, 'w') as f:
                    f.write(new_content)
                print(f"Updated {filepath}")
