import re

with open('tessera-builder/src/main/java/com/tessera/ui/RouteBuilderApp.java', 'r') as f:
    content = f.read()

content = re.sub(r'    private void showManual\(\) \{.*?(?=    @Override)', '', content, flags=re.DOTALL)

with open('tessera-builder/src/main/java/com/tessera/ui/RouteBuilderApp.java', 'w') as f:
    f.write(content)
