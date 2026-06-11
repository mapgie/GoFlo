#!/usr/bin/env python3
"""Validates that a PR adds one or more well-formed changelog fragments.

Used by .github/workflows/changelog-check.yml.
"""

import json
import subprocess
import sys
from pathlib import Path

VALID_BUMPS = {"patch", "minor", "major"}
VALID_SECTIONS = {"added", "changed", "fixed"}


FRAGMENTS_DIR = Path("changelog/unreleased")


def added_fragment_files(base_ref):
    diff = subprocess.run(
        ["git", "diff", "--name-status", f"{base_ref}...HEAD"],
        capture_output=True, text=True, check=True,
    ).stdout

    added = []
    for line in diff.splitlines():
        status, _, path = line.partition("\t")
        if status == "A" and path.startswith("changelog/unreleased/") and path.endswith(".json"):
            added.append(path)
    return added


def all_fragment_files():
    return sorted(str(path) for path in FRAGMENTS_DIR.glob("*.json"))


def validate(path):
    data = json.loads(Path(path).read_text())

    bump = data.get("bump")
    if bump not in VALID_BUMPS:
        return f"{path}: 'bump' must be one of {sorted(VALID_BUMPS)}, got {bump!r}"

    sections = {key: value for key, value in data.items() if key in VALID_SECTIONS}
    if not any(isinstance(value, list) and value for value in sections.values()):
        return f"{path}: must have at least one non-empty 'added'/'changed'/'fixed' list"

    for key, value in sections.items():
        if not isinstance(value, list) or not all(isinstance(item, str) and item.strip() for item in value):
            return f"{path}: '{key}' must be a list of non-empty strings"

    unknown = set(data.keys()) - VALID_SECTIONS - {"bump"}
    if unknown:
        return f"{path}: unknown key(s) {sorted(unknown)}"

    return None


def main():
    base_ref = sys.argv[1] if len(sys.argv) > 1 else "origin/main"
    added = added_fragment_files(base_ref)

    if not added:
        print(
            "::error::No new changelog fragment found. Add a file at "
            "changelog/unreleased/<slug>.json describing this change "
            "(see changelog/unreleased/README.md)."
        )
        return 1

    # Validate every pending fragment, not just the ones this PR adds, so a
    # hand-edit that breaks an existing fragment is caught here rather than
    # at release time.
    errors = [error for path in all_fragment_files() for error in [validate(path)] if error]
    if errors:
        for error in errors:
            print(f"::error::{error}")
        return 1

    for path in added:
        print(f"{path}: OK")
    return 0


if __name__ == "__main__":
    sys.exit(main())
