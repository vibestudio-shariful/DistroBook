
def find_unbalanced_braces(file_path):
    with open(file_path, 'r') as f:
        lines = f.readlines()
    
    stack = []
    for i, line in enumerate(lines):
        for j, char in enumerate(line):
            if char == '{':
                stack.append((i + 1, j + 1))
            elif char == '}':
                if stack:
                    stack.pop()
                else:
                    print(f"Extra '}}' at line {i+1}, col {j+1}")
    
    for line_num, col_num in stack:
        print(f"Unclosed '{{' at line {line_num}, col {col_num}")

print("DashboardScreen.kt:")
find_unbalanced_braces('app/src/main/java/com/example/ui/screens/DashboardScreen.kt')
print("\nShopsScreen.kt:")
find_unbalanced_braces('app/src/main/java/com/example/ui/screens/ShopsScreen.kt')
