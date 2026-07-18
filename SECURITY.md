# Security Policy

## Supported versions

GreenCap is pre-1.0 and under active development. Security fixes are applied to the latest
released version. Please confirm you can reproduce an issue on the latest version before reporting.

| Version         | Supported |
| --------------- | --------- |
| latest `0.7.x`  | ✅        |
| older releases  | ❌        |

## Reporting a vulnerability

**Please do not report security vulnerabilities through public GitHub issues, discussions,
or pull requests.**

Instead, report them privately through GitHub's private vulnerability reporting:

➡️ **[Report a vulnerability](https://github.com/greencapk8s/greencap-k8s/security/advisories/new)**

On the repository this lives under **Security → Report a vulnerability**.

Please include as much of the following as you can:

- A description of the vulnerability and its impact
- Steps to reproduce, or a proof of concept
- Affected version(s) and environment (cluster provider, deployment method)
- Any suggested mitigation, if you have one

## What to expect

- We'll acknowledge your report as soon as possible.
- We'll investigate and keep you informed of progress.
- Once a fix is ready, we'll coordinate disclosure with you.

Because GreenCap stores encrypted cluster credentials (kubeconfigs) and performs write
operations on real clusters, reports affecting **credential handling, RBAC enforcement, and
cluster access** are treated with particular priority.

Thank you for helping keep GreenCap and its users safe.
