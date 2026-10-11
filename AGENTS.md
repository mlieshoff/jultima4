## Workspace Boundaries — Mandatory

The agent is strictly restricted to the current Git project directory.

### Allowed Operations

- Read, search, create, modify and delete files only within the project directory.
- Execute Maven builds and unit tests for the current project.
- Execute Git commands for the current project.
- Create feature and bugfix branches.
- Commit and push changes to the project's remote repository without additional approval.
- Use temporary and build directories located inside the project.

### Forbidden Operations

- NEVER read, search, scan, modify or delete files outside the project directory.
- NEVER access parent directories or sibling projects.
- NEVER scan the user's home directory.
- NEVER inspect unrelated repositories or system directories.
- NEVER use recursive filesystem searches outside the project.
- NEVER access files through symlinks pointing outside the project.
- NEVER access external files through absolute paths or path traversal (`../`).
- NEVER modify global Git, Maven, Java or operating system configuration.
- NEVER execute commands that affect files outside the project directory.
- NEVER use `sudo` or elevated privileges.

### Command Execution

- Always execute commands from the project root or its subdirectories.
- Restrict file searches to the project directory.
- Do not use unrestricted filesystem commands such as `find /`, `grep -R /` or searches across `$HOME`.
- Use project-local configuration and temporary directories whenever possible.
- Do not execute scripts that access or modify files outside the project.
- If an operation requires access outside the project directory, STOP and ask the user for explicit permission.

### Git Permissions

The agent MAY autonomously:

1. Create a task branch from `main`.
2. Modify project files.
3. Implement and run unit tests.
4. Execute Maven verification.
5. Commit changes.
6. Push the task branch to `origin`.

The agent MUST NOT:

- Commit or push directly to `main`.
- Force-push.
- Create, approve or merge Merge Requests.
- Modify unrelated repositories.
- Change global Git configuration.

### Scope Enforcement

Before executing any command, verify that its intended filesystem access remains within the project directory.

**These restrictions take precedence over all other development instructions.**