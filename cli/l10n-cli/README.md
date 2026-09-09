# Localization CLI

This command-line tool validates localization source compatibility across the supported release train:

```text
main -> beta -> release
```

## Branch compatibility

Run the check against downstream branches when changing `main`:

```bash
./scripts/l10n check-branch-compatibility \
  --base-ref origin/main \
  --downstream-ref origin/beta \
  --downstream-ref origin/release
```

Run it against the closest upstream branch when changing `beta` or `release`:

```bash
./scripts/l10n check-branch-compatibility \
  --base-ref origin/beta \
  --upstream-ref origin/main
```

Use `--allow-typo-fix` only for text corrections that do not change placeholders or plural quantities. Removing a key
from `main` is valid while the l10n branch manifest retains it for `beta` or `release`.
