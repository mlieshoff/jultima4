# AGENTS.md — Development Guidelines

## 1. General Principles

You are a software development assistant working on an existing Java project.

- Follow the user's requirements precisely.
- Prefer simple, maintainable solutions over complex abstractions.
- Read and understand relevant existing code before making changes.
- Follow existing architecture, naming conventions and coding style.
- Make only changes necessary to complete the requested task.
- Do not introduce unrelated refactoring or speculative improvements.
- Do not modify unrelated files.
- Never invent requirements or assume undocumented behavior.
- Ask the user when requirements are ambiguous or decisions have significant consequences.

## 2. Technology Stack

- Language: Java
- Build system: Apache Maven
- Testing: JUnit 5, using the project's existing test libraries
- Version control: Git
- Repository hosting: GitLab

Rules:

- Use the Java version configured in `pom.xml`.
- Prefer standard Java APIs and existing project dependencies.
- Do not introduce additional dependencies without approval.
- Do not upgrade Java, Maven plugins or dependencies unless explicitly requested.
- Do not replace the existing build system.
- Respect existing Maven modules and project structure.

## 3. Git Workflow

All development must take place on dedicated feature or bugfix branches.

### Before starting

1. Inspect the current Git status.
2. Do not overwrite or discard existing local changes.
3. Fetch the latest remote changes.
4. Ensure the working tree is clean before switching branches.
5. Create a new branch based on the latest `origin/main`.

Branch naming:

- `feature/<short-description>`
- `fix/<short-description>`
- `refactor/<short-description>`

Use lowercase letters and hyphens.

If the working tree is dirty, the remote is unavailable, or the branch already exists, ask the user how to proceed.

### During development

- Work exclusively on the task branch.
- Never commit directly to `main`.
- Never modify Git history.
- Never force-push.
- Never rebase or merge branches automatically.
- Never discard uncommitted changes.
- Keep changes small and focused.

### After development

1. Review the changes using `git diff`.
2. Run the required Maven tests and verification.
3. Resolve any issues caused by the changes.
4. Commit the completed changes with a meaningful commit message.
5. Push the task branch to `origin`.
6. Report the branch name, commit and test results.

### Human-controlled integration

The human developer is exclusively responsible for:

- Creating the GitLab Merge Request.
- Reviewing the code.
- Approving the Merge Request.
- Merging changes into `main`.

The agent must NEVER create, approve or merge a Merge Request.

After pushing, stop and wait for further instructions.

## 4. Implementation Rules

- Implement only the requested functionality.
- Prefer straightforward and readable Java code.
- Reuse existing components and utilities where appropriate.
- Keep classes and methods focused on a single responsibility.
- Avoid unnecessary interfaces, factories, wrappers or abstraction layers.
- Avoid premature optimization.
- Do not introduce new architectural patterns without approval.
- Preserve existing public APIs unless changes are explicitly required.
- Handle errors explicitly and appropriately.
- Do not silently swallow exceptions.
- Do not add placeholder implementations or unfinished TODOs as completed functionality.

If a requested change requires substantial architectural modifications, explain the proposed approach and obtain approval before implementing it.

## 5. Unit Testing

Unit tests are mandatory for new functionality and bug fixes.

- Use JUnit 5 unless the project already establishes another testing standard.
- Add tests under `src/test/java`.
- Follow existing test naming and organization conventions.
- Test normal behavior, edge cases and relevant error conditions.
- For bug fixes, add a regression test whenever practical.
- Prefer deterministic tests.
- Mock external dependencies where appropriate.
- Do not require network access or production services for unit tests.
- Never remove, disable or weaken existing tests to make the build pass.
- Do not modify production code solely to bypass failing tests.

Avoid trivial tests that do not verify meaningful behavior.

## 6. Maven Verification

Use the Maven Wrapper (`./mvnw`) if available; otherwise use `mvn`.

Before committing, run:

```bash
mvn verify
```

For multi-module projects, use the appropriate Maven reactor configuration when necessary.

Rules:

- Never skip tests to obtain a successful build.
- Never use `-DskipTests` or `-Dmaven.test.skip=true` for final verification.
- Fix failures caused by your changes.
- Report unrelated or pre-existing failures.
- Never claim tests passed unless they were actually executed successfully.
- If verification cannot run, report the reason explicitly.

## 7. Security and Safety

- Never commit passwords, tokens, credentials or private keys.
- Never expose secrets in logs or test output.
- Never execute destructive operations without explicit approval.
- Never modify CI/CD pipelines, deployment scripts or infrastructure configuration unless requested.
- Never deploy applications automatically.
- Never access production systems unless explicitly authorized.
- Never modify database schemas without approval.
- Never delete or overwrite user data.

## 8. Communication

Before implementation:

- Briefly describe the intended changes.
- Identify ambiguities or important risks.
- Ask for clarification only when necessary.

During implementation:

- Keep the user informed of significant problems.
- Do not ask for approval for routine implementation details.
- Stop and ask before making changes outside the agreed scope.

After implementation, provide a concise summary containing:

- Implemented functionality
- Changed components
- Added or updated tests
- Maven verification result
- Git branch name
- Commit identifier
- Push status
- Any remaining issues

## 9. Definition of Done

A task is complete only when:

- The requested functionality is implemented.
- Appropriate unit tests have been added or updated.
- Maven verification has passed.
- No unrelated changes are included.
- The changes have been committed.
- The task branch has been pushed successfully.
- The human developer has been informed.

The task is NOT considered merged or released.

**The agent's responsibility ends after pushing the branch and reporting the result. Human review and merging are mandatory.**