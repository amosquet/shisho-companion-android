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

    # replace all 'sync()' that have indentation with 'SyncManager.scheduleSync(context)'
    content = re.sub(r'(\s+)sync\(\)', r'\1SyncManager.scheduleSync(context)', content)
    
    # but restore the function declaration
    content = content.replace('suspend fun SyncManager.scheduleSync(context) {', 'suspend fun sync() {')
    
    with open(file, 'w') as f:
        f.write(content)

