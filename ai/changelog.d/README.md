# Engine AI changelog entries

One file per PR that changes how the engine AI plays or builds decks, named for the PR's branch
(`<branch-name>.md`) and holding one bullet:

```
- #<pr> <what the AI now does differently, in a sentence>
```

A file per PR is what lets AI PRs be open in parallel: nobody edits a shared list, so nothing
conflicts. The rules for what earns an entry, and the archive of older ones, are in
[`../CHANGELOG.md`](../CHANGELOG.md). `game-server`'s `generateEngineAiHistory` task bakes this
directory and that archive together, ordered by when each entry merged. This README is skipped.
