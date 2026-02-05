## Phase 5 — Delete the `references/Zone.kt` Comment File

**Files changed**: 1 deletion

The file `scripting/references/Zone.kt` is just a comment explaining that `Zone` exists elsewhere. Now that we have a proper typealias in `scripting/events/Zone.kt`, this comment file is dead weight.

```
Delete: scripting/references/Zone.kt
```

**Verification**: Project compiles. No code references this file.
