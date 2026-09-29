# Repository layout

Status: Proposed. Directories are created when the phase that needs them starts.

```
VoidManager/
  README.md
  LICENSE                     MIT
  SECURITY.md
  CONTRIBUTING.md
  CHANGELOG.md
  settings.gradle.kts
  build.gradle.kts
  gradle.properties
  version.properties          single source for app version
  gradle/
    libs.versions.toml        pinned dependency versions
    verification-metadata.xml dependency checksum verification
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
    brand/                    logo, banner and icon masters, see assets/brand/README.md
    screenshots/
  handoffs/                   one handoff document per phase
  scripts/
  website/                    Phase 10
  .github/
    workflows/
    dependabot.yml
    ISSUE_TEMPLATE/
```

Notes:

- Convention plugins (`build-logic`) are not used yet. Each module has its own build script until the duplication justifies them.
- Module names may change in Phase 1 if a split turns out to add cost without value.
- `core:testing` holds synthetic fixtures only. No real personal data is committed anywhere.
- `assets/screenshots` contains demo data only.
