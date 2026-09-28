# Repository layout

Status: Proposed. Directories are created when the phase that needs them starts.

```
VoidManager/
  README.md
  LICENSE                     pending decision
  SECURITY.md                 pending reporting channel
  CONTRIBUTING.md
  CHANGELOG.md
  settings.gradle.kts
  build.gradle.kts
  gradle.properties
  version.properties          single source for app version
  gradle/
    libs.versions.toml        pinned dependency versions
    verification-metadata.xml dependency checksum verification
  build-logic/                convention plugins (kmp-library, compose, detekt, ...)
  config/
    detekt/
    lint/
  app/
    android/
    desktop/
  core/
    common/
    model/
    crypto/
    security/
    database/
    data/
    media/
    backup/
    sharing/
    export/
    platform/
    designsystem/
    navigation/
    testing/                  fakes, fixtures, synthetic data builders
  feature/
    home/
    people/
    profile/
    editor/
    personal/
    search/
    vault/
    share/
    transfer/
    trash/
    settings/
  docs/
    adr/
    security/
  assets/
    brand/
    screenshots/
  scripts/
  website/                    Phase 10
  .github/
    workflows/
    dependabot.yml
    ISSUE_TEMPLATE/
```

Notes:

- Module names may change in Phase 1 if a split turns out to add cost without value.
- `core:testing` holds synthetic fixtures only. No real personal data is committed anywhere.
- `assets/screenshots` contains demo data only.
