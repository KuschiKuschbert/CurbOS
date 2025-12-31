#!/bin/sh

# Regex for Conventional Commits
# Types: build, chore, ci, docs, feat, fix, perf, refactor, revert, style, test
commit_regex='^(build|chore|ci|docs|feat|fix|perf|refactor|revert|style|test)(\([a-z0-9\-]+\))?: .+$'
merge_regex='^Merge .+$'

error_msg="❌ Bad Commit Message!
Your commit message does not follow Conventional Commits formatting.

Format: type(scope): subject
Example: feat(auth): add google sign-in
Types: build, chore, ci, docs, feat, fix, perf, refactor, revert, style, test

Ignored: Merge commits
"

input_file="$1"
commit_msg=$(cat "$input_file")

if echo "$commit_msg" | grep -qE "$merge_regex"; then
    exit 0
fi

if ! echo "$commit_msg" | grep -qE "$commit_regex"; then
    echo "$error_msg"
    exit 1
fi
