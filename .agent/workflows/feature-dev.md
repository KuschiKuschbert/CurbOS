---
description: Standard feature development workflow (Branch -> Code -> Verify -> Merge)
---

# Feature Development Workflow

Automatically follow this process for all coding tasks.

1.  **Check Main**: Ensure strict synchronization with `main` before starting.
    ```bash
    git checkout main
    git pull origin main
    ```

2.  **Feature Branch**: ALWAYS create a new branch. Never push directly to `main`.
    ```bash
    git checkout -b feature/[concise-name]
    ```

3.  **Development**: Implement changes.

4.  **Verification**: MUST pass before merging.
    - **Web**: `npm run lint && npm run build`
    - **Android**: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew :app:assembleDebug`

5.  **Merge**:
    ```bash
    git add .
    git commit -m "[Description]"
    git checkout main
    git merge feature/[concise-name]
    git branch -d feature/[concise-name]
    ```
