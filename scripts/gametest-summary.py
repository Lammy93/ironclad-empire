#!/usr/bin/env python3
"""Print one line per game test from build/gametest.xml. Exit 1 if any test failed."""
import sys
import xml.etree.ElementTree as ET

root = ET.parse(sys.argv[1] if len(sys.argv) > 1 else "build/gametest.xml").getroot()
failed = 0
total = 0
for tc in root.iter("testcase"):
    total += 1
    f = tc.find("failure")
    if f is None:
        print("pass", tc.get("name"))
    else:
        failed += 1
        print("FAIL", tc.get("name"), "-", (f.get("message") or "")[:300])
print(f"{total - failed}/{total} passed")
sys.exit(1 if failed else 0)
