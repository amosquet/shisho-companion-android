import os
import re

ui_dir = "/home/sadako/Documents/github/shisho-companion-android/app/src/main/java/com/example/myapplication/ui"
strings_file = "/home/sadako/Documents/github/shisho-companion-android/app/src/main/res/values/strings.xml"

# Find all strings that require arguments
format_strings = set()
with open(strings_file, 'r') as f:
    for line in f:
        if '%1$d' in line or '%1$s' in line:
            match = re.search(r'name="([^"]+)"', line)
            if match:
                format_strings.add(match.group(1))

print("Format strings:", format_strings)

# Check all kotlin files for stringResource calls without arguments
for root, _, files in os.walk(ui_dir):
    for file in files:
        if file.endswith(".kt"):
            with open(os.path.join(root, file), 'r') as f:
                content = f.read()
                
                # Find all stringResource(R.string.XXX) or stringResource(R.string.XXX, ...)
                # specifically look for stringResource(R.string.XXX) missing the second arg
                for fs in format_strings:
                    # pattern: stringResource(R.string.fs) where it ends with ) immediately
                    pattern = r'stringResource\(\s*R\.string\.' + fs + r'\s*\)'
                    matches = re.finditer(pattern, content)
                    for match in matches:
                        print(f"File {file} missing argument for {fs}: {match.group(0)}")
