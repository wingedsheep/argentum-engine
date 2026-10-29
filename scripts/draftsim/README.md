# Draftsim table refresh

The engine AI drafts and deckbuilds with Draftsim's pick ratings, vendored under
`ai/src/main/resources/draftai/` and loaded by `DraftsimData`. These two stdlib-only
scripts regenerate those files from Draftsim's own draft app, so a refresh is
reproducible rather than hand-edited.

```bash
just draftsim-refresh            # fetch the live bundle, diff against draftai/ (writes nothing)
just draftsim-refresh write      # regenerate draftai/
just draftsim-refresh check path/to/index-XXXX.js   # or a saved .har, instead of fetching
```

- `extract_from_bundle.py <bundle.js|capture.har> <out-dir>` reads the
  `../data/<CODE>.txt` (tab-separated rating tables) and `../data/removal/<CODE>.json`
  (curated removal lists) that the Vite bundle at `https://draftsim.com/draft/` inlines,
  resolving the minifier's variables and hoisted constants. It interprets nothing.
- `import_draftsim_sets.py <out-dir> [--check]` turns those into `ratings/`, `archetypes/`,
  `removal/` and `ratings/_manifest.json` the way the draft app's own loaders read them.
  The module docstring has the column rules. `--check` reports each file as identical,
  "reordered only", or changed (with values that moved and names added or removed).

## What a refresh never writes

The importer is hard-wired never to write these:

- **First-party ratings**: `ATQ SCG LGN POR DOM MRD ONS INV BIG`. These are hand-curated
  (see `DraftsimData`'s class doc).
- **`archetypes/FDN.json`**: also first-party.
- **`removal/_overrides.json`**: manual removal entries that `DraftsimData` merges in at
  load time. Edit this file by hand. Draftsim's bundle ships its own overrides list, and
  it's worth comparing against ours on each refresh.

A table that upstream ships but whose rows yield no ratings is reported and left alone.
Today that's `AFR`: it's headerless with the rating in column 5, so Draftsim's own app
reads nothing from it. Our `AFR.json` comes from an older snapshot.

Run the gate with `just loop-gate`. `DraftsimDataTest` checks that every manifest code
has a non-empty table, that keys are already in nameKey form, and that the name join
works for sets in our registry. When a set is newly vendored and our registry has its
cards, add it to that test's join list.
