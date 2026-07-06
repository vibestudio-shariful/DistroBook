
def check_braces(file_path):
    with open(file_path, 'r') as f:
        content = f.read()
    
    stack = []
    for i, char in enumerate(content):
        if char == '{':
            stack.append(i)
        elif char == '}':
            if not stack:
                print(f"Extra closing brace at {i}")
                return
            stack.pop()
    
    if stack:
        print(f"Missing closing braces for: {stack}")
    else:
        print("Braces are balanced")

check_braces('/app/src/main/java/com/example/ui/screens/ProfileScreen.kt')
