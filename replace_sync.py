import sys
import re

files = [
    "app/src/main/java/com/shishocompanion/data/NoteRepository.kt",
    "app/src/main/java/com/shishocompanion/data/ReminderRepository.kt",
    "app/src/main/java/com/shishocompanion/data/ReadingListRepository.kt"
]

for file in files:
    with open(file, 'r') as f:
        content = f.read()

    # We want to replace:
    # try {
    #     sync()
    # } catch (e: Exception) {
    #     e.printStackTrace()
    # }
    # with:
    # SyncManager.scheduleSync(context)

    # First, let's just replace exact block if it exists
    pattern = re.compile(r'try\s*\{\s*sync\(\)\s*\}\s*catch\s*\([^\)]+\)\s*\{\s*e\.printStackTrace\(\)\s*\}')
    content = pattern.sub('SyncManager.scheduleSync(context)', content)

    # Also there are cases like `sync()` directly.
    # Let's manually review the files.
    with open(file, 'w') as f:
        f.write(content)

