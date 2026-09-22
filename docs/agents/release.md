# Release

A version comes from a git tag, never from a branch; the why is in `docs/adr/0023-trunk-unico-com-release-por-tag.md`. `build.gradle.kts` resolves it in order: `GREENCAP_VERSION` when set (as `publish-image.yml` does); else the tag pointing exactly at `HEAD`, minus the `v` (`v0.7.10` → `0.7.10`, `v0.7.10-rc.1` → `0.7.10-rc.1`); else `version.base` from `gradle.properties` plus `-dev`.

## Steps

1. **Release candidate** (optional): tag `vX.Y.Z-rc.N` on `main`, push the tag, and publish its GitHub Release as a pre-release (`gh release create vX.Y.Z-rc.N --prerelease`). `publish-image.yml` publishes `ghcr.io/greencapk8s/platform:X.Y.Z-rc.N` without moving `:latest`. Validate with `PLATFORM_IMAGE_TAG=X.Y.Z-rc.N ./setup/setup.sh`; a fix is merged into `main` and tagged `-rc.N+1`.
2. **Release**: tag `vX.Y.Z` on `main`, push the tag, and publish its GitHub Release. Only a tag without a hyphen moves `:latest`.
3. **Bump right away**: raise `version.base` in `gradle.properties` to the next target, through a PR like any change to `main`. Until it merges, new commits build as `X.Y.Z-dev`, which reads as older than the release just published.

## Fixing a published release

Only when a release needs a fix after `main` has moved on: create `release-X.Y` from its tag, fix there and tag from it.
