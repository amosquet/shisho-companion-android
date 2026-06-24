import re

with open("app/src/main/java/com/example/myapplication/MainActivity.kt", "r") as f:
    content = f.read()

# We need to remove:
# paneExpansionState = paneExpansionState,
# paneExpansionDragHandle = { state ->
#     val interactionSource = remember { MutableInteractionSource() }
#     VerticalDragHandle(
#         modifier = Modifier.paneExpansionDraggable(
#             state = state,
#             orientation = Orientation.Vertical,
#             interactionSource = interactionSource
#         ),
#         interactionSource = interactionSource
#     )
# },

# And we should also remove `val paneExpansionState = rememberPaneExpansionState()`

content = re.sub(r'val paneExpansionState = rememberPaneExpansionState\(\)\n\s*', '', content)
content = re.sub(r'paneExpansionState = paneExpansionState,\n\s*', '', content)

pattern = r'paneExpansionDragHandle = \{ state ->.*?(?=\b(?:listPane|detailPane)\b = \{)'
content = re.sub(pattern, '', content, flags=re.DOTALL)

with open("app/src/main/java/com/example/myapplication/MainActivity.kt", "w") as f:
    f.write(content)

