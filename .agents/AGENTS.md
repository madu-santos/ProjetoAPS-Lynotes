# Rules for AI Assistant

## Version Control (Git/GitHub)

### FORBIDDEN:
- **NEVER** commit, push, or perform any operation that modifies the remote repository (GitHub) without explicit user permission
- **NEVER** run git commands that alter history (amend, rebase, force push) without confirmation
- **NEVER** create, delete, or modify remote branches without permission

### ALLOWED (with user confirmation):
- Run `git status`, `git log`, `git diff` and other **read-only** commands
- Suggest commit messages **without executing them**
- Fetch information from remote repositories with `git fetch` or `git pull` **ONLY after user confirms**

### Mandatory workflow for any write action:
1. **ANNOUNCE** what you're going to do
2. **WAIT** for explicit user confirmation (response like "yes", "ok", "go ahead", etc.)
3. **EXECUTE** only after confirmation

### Exceptions:
- Commands that the user **types manually** in the terminal are not the AI's responsibility

---

**Note:** This rule applies to ALL repositories, including this one.

---

**Whenever you need to run a git command, first show me the full command and wait for my response before executing it.**