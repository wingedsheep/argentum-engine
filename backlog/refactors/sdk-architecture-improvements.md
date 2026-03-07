# SDK Architecture Improvements

Analysis of `mtg-sdk/src/main/kotlin/com/wingedsheep/sdk/` with recommendations for making
the SDK more modern, modular, fast, and maintainable.

**Scope:** SDK module only (core, model, scripting, dsl, serialization).

---

## Summary

The SDK is well-architected overall: immutable data-driven design, clean dependency direction
(DSL -> model -> scripting -> core), zero circular dependencies, and strong use of Kotlin's
sealed interfaces. The main issues are **file size / cohesion** (several 600-2300 line files),
**hard-coded registries in serialization** that don't scale, and **minor API inconsistencies**
in the DSL facades.

### Priority Matrix

| Priority | Area | Effort | Impact |
|----------|------|--------|--------|
| High | Split oversized sealed interface files | Medium | Maintainability, navigation |
| High | Auto-derive serialization registries | Medium | Extensibility, fewer bugs |
| Medium | CardDefinition decomposition | Low | Readability |
| Medium | EffectPatterns splitting | Low | Discoverability |
| Medium | CardScript.allAbilities type safety | Low | Type safety |
| Low | DSL validation | Low | Developer experience |
| Low | Phase/Step declarative sequencing | Low | Extensibility |
| Low | DynamicAmount ergonomics | Low | Developer experience |

---

## 1. Split Oversized Sealed Interface Files

**Problem:** Several files have grown too large as new sealed subtypes accumulate:

| File | Lines | Sealed subtypes |
|------|-------|-----------------|
| `scripting/StaticAbility.kt` | 1,350 | 40+ |
| `scripting/GameEvent.kt` | 791 | 30+ |
| `scripting/ReplacementEffect.kt` | 652 | 25+ |
| `scripting/effects/PermanentEffects.kt` | 805 | 15+ |
| `scripting/effects/PipelineEffects.kt` | 659 | 10+ |
| `scripting/effects/CompositeEffects.kt` | 669 | 7 |
| `scripting/values/DynamicAmount.kt` | 581 | 30+ |
| `scripting/KeywordAbility.kt` | 440 | 20+ |

These are hard to navigate, produce large diffs when touched, and make onboarding harder.

**Solution:** Extract subtypes into categorized files while keeping the sealed interface in
the original file. Kotlin supports sealed subtypes in the same package across files.

Example for `StaticAbility.kt`:

```
scripting/
  StaticAbility.kt              (sealed interface + base docs, ~50 lines)
  staticabilities/
    GrantAbilities.kt            (GrantKeyword, GrantAbility, GrantActivatedAbility, etc.)
    ModifyStats.kt               (ModifyStatsForCreatureGroup, SetPowerToughness, etc.)
    AuraAbilities.kt             (AuraGrantKeyword, ModifyStatsIfEnchanted, etc.)
    PreventionAbilities.kt       (PreventCombatDamage, PreventDamageToCreature, etc.)
    PropertyAbilities.kt         (HasAbility, CantBeBlockedExceptByColor, etc.)
```

Similarly for `GameEvent.kt`, `ReplacementEffect.kt`, and `DynamicAmount.kt`.

**Effort:** Medium (move subtypes to new files, update imports).
**Risk:** Low (purely structural, no behavior change).

---

## 2. Auto-Derive Serialization Registries

**Problem:** Three serialization files maintain hard-coded lists of SDK types that must be
manually updated whenever new effects, triggers, or predicates are added:

1. **`CardValidator.kt`** — `collectTargetIndicesFromEffect()` matches 49 effect subtypes in
   a `when` expression. New effects silently skip validation (`else -> null`).

2. **`CompactJsonTransformer.kt`** — 5 hard-coded key sets (43 keys total) for polymorphic
   JSON compaction. New polymorphic fields remain verbose if not registered.

3. **`FilterQueryLanguage.kt`** — Maps 27+ predicate types to query string tokens. New
   predicates require updates in 4 separate locations.

This creates a hidden maintenance tax: adding a new effect type requires touching these files
or risk silent correctness/serialization bugs.

**Solution options:**

**Option A: Annotation-driven registry (preferred)**
Add a marker annotation to Effect subtypes that carry targets:
```kotlin
@Target(AnnotationTarget.PROPERTY)
annotation class EffectTargetField

// In DealDamageEffect:
data class DealDamageEffect(
    @EffectTargetField val target: EffectTarget,
    val amount: DynamicAmount
) : Effect
```
CardValidator uses reflection at startup to discover all `@EffectTargetField` properties.
Same pattern for CompactJsonTransformer polymorphic keys.

**Option B: Kotlin compile-time code generation (KSP)**
Generate the registries from sealed interface hierarchies at build time. Most robust but
higher setup cost.

**Option C: Exhaustive `when` with `sealed` (partial fix)**
For `CardValidator`, change `else -> null` to no-else so the compiler forces updates when
new Effect subtypes are added. This works because Effect is sealed. Does not solve
CompactJsonTransformer or FilterQueryLanguage.

**Recommended:** Start with Option C (compiler-enforced exhaustive when) as a quick win,
then evaluate Option A for CompactJsonTransformer and FilterQueryLanguage.

**Effort:** Low (Option C), Medium (Option A).
**Risk:** Low.

---

## 3. CardDefinition Decomposition

**Problem:** `model/CardDefinition.kt` is 455 lines containing:
- The main data class (68+ fields)
- `Ruling` and `ScryfallMetadata` data classes
- `Rarity` enum
- 11 factory methods with duplicate overloads (e.g., two `instant()` variants)

**Solution:**

1. Extract `ScryfallMetadata`, `Ruling`, and `Rarity` to `model/CardMetadata.kt` (~50 lines).
2. Extract factory methods to `model/CardDefinitionFactory.kt` (~200 lines) as extension
   functions on `CardDefinition.Companion`.
3. Merge duplicate `instant()` overloads using default parameters.
4. Move niche factories (`kindredInstant`, `kindredEnchantment`, `doubleFacedCreature`) to
   a separate `model/CardDefinitionExtensions.kt`.

**Effort:** Low.
**Risk:** Low (extension functions on companion maintain the same call-site API).

---

## 4. Split EffectPatterns.kt

**Problem:** `dsl/EffectPatterns.kt` is 2,275 lines — the largest file in the SDK. It's a
collection of 60+ factory methods for composing atomic effects into reusable pipelines. Hard
to navigate and discover the right pattern.

**Solution:** Split by mechanic category:

```
dsl/
  EffectPatterns.kt              (object with delegating imports, ~50 lines)
  patterns/
    LibraryPatterns.kt           (scry, surveil, mill, searchLibrary, lookAtTop)
    SacrificePatterns.kt         (sacrificeFor, reflexiveTrigger, sacrificeAndEffect)
    ExilePatterns.kt             (exileUntilLeaves, linkedExile, temporaryExile)
    DrawDiscardPatterns.kt       (wheel, looting, rummaging)
    SelectionPatterns.kt         (chooseCreatureType, modalPatterns)
    TokenPatterns.kt             (createTokenWithEffect, tokenCreation)
```

Keep the `EffectPatterns` object as a facade that re-exports from sub-files, so existing
call sites (`EffectPatterns.scry(2)`) continue to work unchanged.

**Effort:** Low-Medium.
**Risk:** Low (no API change).

---

## 5. Type-Safe `CardScript.allAbilities`

**Problem:** `CardScript.allAbilities` returns `List<Any>`:
```kotlin
val allAbilities: List<Any>
    get() = triggeredAbilities + activatedAbilities + staticAbilities + replacementEffects
```
Callers must use `filterIsInstance<>()` or `when` with type checks.

**Solution:** Introduce a sealed interface:
```kotlin
sealed interface Ability {
    // Marker interface — TriggeredAbility, ActivatedAbility, StaticAbility,
    // ReplacementEffect all implement this
}
```
Then `allAbilities: List<Ability>` becomes type-safe and exhaustive `when` is possible.

**Effort:** Low (add interface, add `: Ability` to 4 classes).
**Risk:** Low.

---

## 6. DSL Builder Validation

**Problem:** DSL builders allow invalid configurations that only fail at runtime:
- `SpellBuilder` permits null effect (no-op spell)
- `ModalBuilder` permits empty modes
- `StaticAbilityBuilder.buildFromEffect()` has fragile auto-conversion logic for
  `CompositeEffect` (extracts first `ModifyStatsEffect`, drops other effects silently)

**Solution:**
- Add `build()` validation: `SpellBuilder` throws if effect is null.
- `ModalBuilder` throws if fewer than 2 modes defined.
- Remove `buildFromEffect()` auto-conversion — require explicit `ability = ...` for complex
  static abilities instead of guessing from effects.

**Effort:** Low.
**Risk:** Low (fails fast with clear error messages during card definition, not at runtime).

---

## 7. Declarative Phase/Step Sequencing

**Problem:** `Phase.next()` and `Step.next()` use hard-coded `when` expressions (22+ arms).
Adding a new step requires modifying multiple `when` blocks.

**Solution:** Define sequencing as a list:
```kotlin
enum class Step(val phase: Phase, val hasPriority: Boolean) {
    UNTAP(Phase.BEGINNING, false),
    UPKEEP(Phase.BEGINNING, true),
    // ...
    ;
    fun next(): Step = entries.getOrElse(ordinal + 1) { entries.first() }
}
```
Use `ordinal` for sequencing instead of manual mapping.

**Effort:** Low.
**Risk:** Low (enum ordinal matches declaration order).

---

## 8. DynamicAmount Ergonomics

**Problem:** Simple integer amounts require verbose wrapping:
```kotlin
DynamicAmount.Fixed(3)  // instead of just 3
```
This appears hundreds of times in card definitions and effect constructors.

**Solution:** Add an implicit conversion or factory:
```kotlin
// Extension property on Int
val Int.fixed: DynamicAmount get() = DynamicAmount.Fixed(this)

// Or: accept Int in DSL facades and wrap internally
fun Effects.DealDamage(amount: Int, ...) = DealDamageEffect(DynamicAmount.Fixed(amount), ...)
```

The DSL facades (`Effects.kt`) already do this in many places. Audit for remaining raw
`DynamicAmount.Fixed()` calls in card definitions and convert to facade methods.

**Effort:** Low.
**Risk:** Low.

---

## 9. Reduce TextReplaceable Boilerplate

**Problem:** ~50 types implement `TextReplaceable` interface, and ~30 of them just return
`this` (identity no-op):
```kotlin
override fun replaceText(replacements: Map<String, String>): SomeEffect = this
```

**Solution:** Provide a default implementation on the interface:
```kotlin
interface TextReplaceable<T> {
    fun replaceText(replacements: Map<String, String>): T = @Suppress("UNCHECKED_CAST") (this as T)
}
```
Subtypes that actually perform text replacement override it. Others get the default for free.

**Alternative:** If the interface already has a default, audit to remove redundant overrides.

**Effort:** Low.
**Risk:** Low.

---

## 10. Zone Visibility Type Safety

**Problem:** `Zone` has independent boolean properties (`isPublic`, `isHidden`, `isShared`)
that could theoretically be inconsistent (a zone both public and hidden).

**Solution:** Replace with an enum:
```kotlin
enum class ZoneVisibility { PUBLIC, HIDDEN, SHARED }

enum class Zone(val visibility: ZoneVisibility) {
    LIBRARY(ZoneVisibility.HIDDEN),
    HAND(ZoneVisibility.HIDDEN),
    BATTLEFIELD(ZoneVisibility.PUBLIC),
    // ...
    ;
    val isPublic get() = visibility == ZoneVisibility.PUBLIC
    val isHidden get() = visibility == ZoneVisibility.HIDDEN
}
```

**Effort:** Low.
**Risk:** Low.

---

## Non-Goals (Explicitly Out of Scope)

These were considered during analysis but are **not recommended** at this time:

- **Subtype externalization** — Moving 240+ creature types to a resource file adds
  indirection without clear benefit. The current enum approach provides compile-time safety.
- **KDoc on factory methods** — Adds maintenance burden without proportional value since
  the DSL facades are the primary API surface.
- **ColorIdentity completeness** — Parsing oracle text for mana symbols is complex and
  color identity is only used for Commander format, not currently supported.
- **Intermediate groupings in effects/** — The current flat structure with 20 categorized
  files is sufficient. Sub-subdirectories would add navigation overhead.

---

## Suggested Execution Order

```
Phase 1: Quick wins (items 5, 7, 10)
  - Type-safe allAbilities, declarative Step sequencing, Zone visibility
  - Independent changes, each shippable separately

Phase 2: File splits (items 1, 3, 4)
  - Split StaticAbility, GameEvent, ReplacementEffect, DynamicAmount
  - Split CardDefinition, EffectPatterns
  - Purely structural, no behavior change

Phase 3: Serialization hardening (item 2)
  - Exhaustive when in CardValidator (Option C)
  - Evaluate annotation-driven registries (Option A)

Phase 4: DX polish (items 6, 8, 9)
  - DSL validation, DynamicAmount ergonomics, TextReplaceable cleanup
```

Each phase is independently shippable. Phases 1 and 2 can be parallelized.
