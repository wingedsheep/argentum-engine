# mtg-sdk

Shared contract module: data models, DSLs, scripting primitives. Zero deps on other modules, no
execution logic. Consumed by `mtg-sets` (content) and `rules-engine` (execution).

## Build & Test

```bash
./gradlew :mtg-sdk:test
```

## Where to look

- **Full DSL reference** (effects, triggers, conditions, targets, costs, keywords, dynamic amounts,
  modal shape, replacement effects, etc.): [`../docs/card-sdk-language-reference.md`](../docs/card-sdk-language-reference.md)
- **Architectural reasoning** (ECS, layers, continuations): [`../docs/architecture-principles.md`](../docs/architecture-principles.md)
- **Adding cards / mechanics step-by-step**: [`../docs/api-guide.md`](../docs/api-guide.md)
- Package layout under `com.wingedsheep.sdk/` — read the directory; it mirrors the doc structure.

## Load-bearing rules

- **Update the DSL reference in the same change** as any SDK addition or rename — it's the canonical
  catalog and rots fast otherwise.
- **All `Effect`, `Trigger`, `StaticAbility`, `TargetRequirement`, `Condition`, etc. subtypes must be
  `@Serializable`.** `CardDefinition` is serialized for transport; a missing annotation fails at runtime.
- **Use the facades, not raw constructors** — `Effects.*`, the pattern objects (`Patterns.Library.*`,
  `Patterns.Hand.*`, `Patterns.Group.*`, `Patterns.Exile.*`, `Patterns.CreatureType.*`, `Patterns.Mechanic.*`), `Triggers.*`, `Costs.*`,
  `Conditions.*`, `Filters.*`. Direct data-class construction bypasses the curated API surface and
  ages badly when the underlying shape changes.
