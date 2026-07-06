
with open('/app/applet/app/src/main/java/com/example/ui/screens/ProfileScreen.kt', 'r') as f:
    lines = f.readlines()

# Remove the last few lines that are just braces
# Let's count how many lines we have and remove lines that look like closing braces
while lines and lines[-1].strip() == '}':
    lines.pop()

with open('/app/applet/app/src/main/java/com/example/ui/screens/ProfileScreen.kt', 'w') as f:
    f.writelines(lines)
