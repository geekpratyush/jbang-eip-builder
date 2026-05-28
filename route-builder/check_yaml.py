import os
import re

def check_file(filepath):
    with open(filepath, 'r') as f:
        lines = f.readlines()
    
    for i in range(len(lines)):
        line = lines[i]
        match_from = re.match(r'^([ ]+)from:', line)
        if match_from:
            indent = match_from.group(1)
            # Look for steps: with the same indentation in the following lines
            # but stop if we find another route or onException
            for j in range(i + 1, len(lines)):
                next_line = lines[j]
                if re.match(r'^([ ]*)' + indent + 'steps:', next_line):
                    return True
                if re.match(r'^[ ]*- route:', next_line) or re.match(r'^[ ]*- onException:', next_line):
                    break
    return False

root_dir = 'src/main/resources/sampleproject'
for root, dirs, files in os.walk(root_dir):
    for file in files:
        if file.endswith('.yaml'):
            filepath = os.path.join(root, file)
            if check_file(filepath):
                print(filepath)
