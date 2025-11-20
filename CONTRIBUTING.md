# If you've reached this document, it means you're interested in participating in the project.

# Thank you for your interest in contributing to **TimeLess**! 🎉

This document is a set of guidelines to help you contribute to this project. Feel free to suggest changes to this document in a Pull Request.

1. [Code of Conduct](#code-of-conduct)
2. [How Can I Contribute?](#how-can-i-contribute)
    - [Reporting Bugs](#reporting-bugs)
    - [Suggesting Improvements](#suggesting-improvements)
    - [Your Pull Request](#your-pull-request)
3. [Development Guide](#development-guide)
    - [PreRequisites](#prerequisites)
    - [Environment Setup](#environment-setup)
    - [Running the Project](#running-the-project)
4. [Style Guide](#style-guide)
    - [Commit Messages](#commit-messages)


## Code of Conduct

This project and all those who participate in it are governed by the [Code of Conduct](https://www.contributor-covenant.org/version/3/0/code_of_conduct/). By participating, you are expected to uphold this code. Please report unacceptable behavior to the project maintainers.


## How Can I Contribute?

### Reporting Bugs

Bugs are tracked as [GitHub Issues](https://github.com/mcruzdev/timeless/issues). When creating an issue, explain the problem and include additional details to help maintainers reproduce the issue:

- Use a clear and descriptive title.
- Describe the exact steps to reproduce the problem.
- Describe the behavior you observed and what you expected.
- Add screenshots or a video if possible.

### Suggestions for Improvements

Suggestions for improvements are also logged as [GitHub Issues](https://github.com/mcruzdev/timeless/issues).

- Use a clear and descriptive title.
- Provide a step-by-step description of the suggested improvement.
- Explain why this improvement would be useful for most users.

### Your Pull Request

1. Fork the repository and clone it locally.
2. Create a branch for your edit: `git checkout -b my-new-feature`.
3. Make your changes and commit: `git commit -m 'feat: Adds new feature'`.
4. Push to the original branch: `git push origin my-new-feature`.
5. Create a Pull Request.

### PreRequisites

To run this project, you will need:

- **JDK 21** or higher.

- **Maven** (optional, as the project includes the `mvnw` wrapper).

### Environment Setup

# Perform the fork of the project.

Clone the repository:

```bash
git clone https://github.com/your-github-profile/timeless
cd timeless
```

### Running the Project

To start Quarkus's development mode (dev mode), which allows live reload:

**Linux/macOS:**
```bash
./mvnw quarkus:dev
```

**Windows:**
```cmd
mvnw.cmd quarkus:dev
```

The site will be available at `http://localhost:8080`.

## Style Guide

### Commit Messages

- Use the [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/) format.
- Use the imperative mode in the subject ("Add feature" and not "Adding feature").
- The first line must have a maximum of 50 characters.
- Reference issues and pull requests liberally after the first line.