import re
with open("app/src/main/java/com/example/myapplication/MainActivity.kt", "r") as f:
    lines = f.readlines()
with open("app/src/main/java/com/example/myapplication/MainActivity.kt", "w") as f:
    for line in lines:
        if "VerticalDragHandle" in line or "paneExpansionDraggable" in line or "PaneExpansionState" in line:
            continue
        f.write(line)
