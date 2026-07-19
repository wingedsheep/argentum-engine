# The Continuous Effect Dependency System

In a standard ECS model, systems simply iterate through components. However, Magic: The Gathering requires continuous
effects to be applied in a specific order. While **Layers** (Rule 613.1) handle the macro-ordering (e.g., "Copy effects
before P/T changes"), **Dependencies** (Rule 613.8) dictate the order *within* a single layer.

If we rely solely on timestamps (First In, First Out), the engine will break for interactions like *Blood Moon* vs.
*Urborg, Tomb of Yawgmoth*.

## 1. The Problem: Rule 613.8

An effect **A** is said to depend on effect **B** if:

1. They apply in the same layer.
2. Applying **B** would:

* Change the *text* or *existence* of effect **A**.
* Change the *set of objects* that effect **A** applies to.
* Change *how* effect **A** applies.


3. Effect **B** does not depend on **A** (no circular dependency).

**If A depends on B, B must be applied first, regardless of timestamps.**

### The Classic Example

* **Card A (Blood Moon):** "Nonbasic lands are Mountains." (Removes existing abilities of those lands).
* **Card B (Urborg, Tomb of Yawgmoth):** "Each land is a Swamp in addition to its other types." (Grants an
  ability/type).

Both are **Layer 4 (Type Changing)**.

* If we apply *Urborg* first: Lands become Swamps. Then *Blood Moon* applies, turning non-basics into Mountains and
  removing *Urborg's* ability (if Urborg is non-basic).
* **Correct Resolution:** *Urborg* depends on *Blood Moon* because *Blood Moon* can remove the ability that generates
  *Urborg's* effect. Thus, *Blood Moon* applies first. *Urborg* loses its ability and generates no effect.

---

## 2. The Solution: Iterative Trial Application

The "Best" solution prioritizes **Correctness** over raw speed. MTG logic is too dynamic to rely on static dependency
graphs or declarative tags. We must use **Trial Application** to simulate applying an effect to see if it changes the
outcome of another.

However, simulating the entire game state for every pair of effects is too slow. We use a **Candidate Filtering**
optimization.

### The Algorithm

Inside the `StateProjector`, when processing a specific Layer (e.g., Layer 4):

1. **Collection:** Gather all active effects for this layer.
2. **Dependency Sort:** Sort the effects using a custom comparator.

* If `A` depends on `B` -> `B` comes before `A`.
* If `B` depends on `A` -> `A` comes before `B`.
* If Circular or Independent -> Use Timestamp order.


3. **Application:** Apply effects in the sorted order.

### The `dependsOn(Effect A, Effect B)` Check

To determine if `A` depends on `B`, we execute the following logic:

1. **Fast-Fail Check (Optimization):**

* Does `B` modify a characteristic that `A` filters for?
* *Example:* If `A` affects "Goblins" and `B` changes "Color", there is likely no dependency. If `B` changes "Subtypes",
  there might be.


2. **Trial Application (The Core Logic):**

* Create a temporary `GameView` state (`S_base`).
* Calculate the outcome of `A` on `S_base` -> `Result_A_alone`.
* Apply `B` to `S_base` to create `S_modified`.
* Calculate the outcome of `A` on `S_modified` -> `Result_A_after_B`.


3. **Comparison:**

* If `Result_A_alone` != `Result_A_after_B`, then **A depends on B**.

---

## 3. Implementation Strategy

We introduce a `DependencyResolver` service that works alongside the `StateProjector`.

### A. The Dependency Resolver

```kotlin
object DependencyResolver {

    /**
     * Sorts effects based on dependencies and timestamps.
     */
    fun sortEffects(
        effects: List<ActiveContinuousEffect>,
        state: GameState,
        projector: StateProjector
    ): List<ActiveContinuousEffect> {
        if (effects.size < 2) return effects

        // Topological sort logic
        val sorted = ArrayList(effects)
        var changed = true

        while (changed) {
            changed = false
            // Bubble sort passes are sufficient for small N (MTG usually has < 10 effects per layer)
            // Ideally, build a real graph, but this is easier to implement and debug.
            for (i in 0 until sorted.size - 1) {
                val a = sorted[i]
                val b = sorted[i + 1]

                if (dependsOn(a, b, state, projector)) {
                    // A depends on B, so B must come FIRST.
                    // Currently A is first. Swap them.
                    val dependencyMet = false // A is after B? No, A is at i, B is at i+1
                    // Wait, if A depends on B, B must act before A.
                    // If we found (A, B) such that A depends on B, they are in Wrong Order.
                    // Swap to (B, A).
                    sorted[i] = b
                    sorted[i + 1] = a
                    changed = true
                }
            }
        }
        return sorted
    }

    /**
     * Returns true if Dependent depends on Dependency.
     */
    private fun dependsOn(
        dependent: ActiveContinuousEffect,
        dependency: ActiveContinuousEffect,
        state: GameState,
        projector: StateProjector
    ): Boolean {
        // 1. Optimization: Check Characteristic overlap
        if (!shareCharacteristicDomain(dependent, dependency)) return false

        // 2. Trial Application
        // Calculate what 'dependent' affects in vacuum
        val targetsAlone = resolveTargets(dependent, state, projector)

        // Apply 'dependency' to a temp view
        val stateWithDependency = projector.applyEffects(state, listOf(dependency))

        // Calculate what 'dependent' affects now
        val targetsAfter = resolveTargets(dependent, stateWithDependency, projector)

        // 3. Compare sets and existence
        return targetsAlone != targetsAfter
    }
}

```

### B. Optimizing with "Characteristic Domains"

To avoid running Trial Application for every pair (which is ), every `Modifier` should declare metadata about what it
touches.

```kotlin
enum class Characteristic {
    POWER_TOUGHNESS,
    COLOR,
    TYPES,
    ABILITIES,
    CONTROLLER,
    TEXT
}

data class ModificationMetadata(
    val affects: Set<Characteristic>, // What does this write?
    val filtersBy: Set<Characteristic> // What does this read?
)

// In DependencyResolver
private fun shareCharacteristicDomain(dep: Effect, ind: Effect): Boolean {
    // If Independent writes to TYPES, and Dependent reads TYPES, we must check deeper.
    // If Independent writes to COLOR, but Dependent filters by CARD_NAME, they don't interact.
    return ind.metadata.affects.intersect(dep.metadata.filtersBy).isNotEmpty()
}

```

---

## 4. Why This is the "Best" Approach

1. **Reliability:** It strictly adheres to the Comprehensive Rules. Hard-coding interactions ("Blood Moon beats Urborg")
   leads to spaghetti code when new cards (e.g., *Ashaya, Soul of the Wild*) are released.
2. **Performance Scalability:**

* **99% Case:** Most effects don't interact (Glorious Anthem vs. Levitation). The `shareCharacteristicDomain` check
  returns `false` instantly.
* **1% Case:** Complex dependency loops. The Trial Application runs only here. Since board states rarely have >10 global
  effects active at once, the cost is negligible (e.g., 100 fast checks).


3. **Debuggability:** You can log exactly *why* an effect was reordered: "Reordered [Urborg] after [Blood Moon] because
   target set changed from {Land1, Land2} to {}."

## 5. Architectural Integration

Modify the `StateProjector` loop:

```kotlin
class StateProjector(val state: GameState) {
    fun project(): GameView {
        var view = createBaseView(state)

        for (layer in Layer.values()) {
            val layerEffects = state.continuousEffects.filter { it.layer == layer }

            // THE NEW STEP:
            val sortedEffects = DependencyResolver.sortEffects(layerEffects, state, this)

            for (effect in sortedEffects) {
                view = apply(view, effect)
            }
        }
        return view
    }
}

```

This effectively solves the hardest logic problem in the engine while keeping the core `GameAction` logic pure and
simple.

## 6. Cross-Layer Grouping (Rule 613.6)

Dependencies (613.8) order effects *within* a layer. Rule **613.6** governs a different problem: a *single* continuous
effect that applies in **several** layers.

> 613.6. If an effect should be applied in different layers and/or sublayers, the parts of the effect each apply in
> their appropriate ones. If an effect starts to apply in one layer and/or sublayer, it will continue to be applied to
> the **same set of objects** in each other applicable layer and/or sublayer, **even if the ability generating the
> effect is removed during this process.**

The canonical example: *"All noncreature artifacts become 2/2 artifact creatures."* The type change (Layer 4) is applied
to the set of noncreature artifacts; the P/T set (Layer 7b) is applied to **those same permanents**, even though they
are no longer noncreature artifacts by then. Bello, Bard of the Brambles is the same shape (Layer 4 type/subtype, Layer
6 keywords, Layer 7b P/T), and additionally exercises the "even if the ability is removed" clause — if Bello loses all
abilities in Layer 6, the animation it started in Layer 4 still sets the 4/4 in Layer 7b.

### Implementation: effect groups

A single static ability that lowers to more than one `ContinuousEffectData` (an `AnimateLandGroup`, a
`TransformPermanent`, a `CompositeStaticAbility`, Blood Moon's type + ability pair, ...) is one continuous effect
spanning multiple layers. `StaticAbilityHandler.toGroupedEffectData` stamps every such ability's effects with a shared
`ContinuousEffectData.groupId` (unique per source permanent; single-layer abilities keep `groupId = null`). The
`StateProjector` then enforces 613.6 for each group, keyed by `(sourceId, groupId)`:

* **Same set of objects** — `lockAffected` records the affected-object set the *first* time a group is resolved. Because
  the projector processes effects in ascending layer order, that's the group's earliest applicable layer. Every later
  layer reuses the frozen set instead of re-resolving its filter (which could otherwise drift — a "noncreature artifact"
  filter matches nothing once Layer 4 has made the artifacts creatures).
* **Survives ability removal** — the Layer-7 "source lost all abilities" suppression (which correctly kills a standalone
  lord anthem removed by Humility) is *skipped* for a group whose earliest layer is before Layer 6 (ABILITY). Such a
  group "started to apply" before the removal, so per 613.6 its later-layer parts keep applying.

This is orthogonal to 613.8: grouping decides *which set* a multi-layer effect uses across layers; dependency sorting
decides the *order* of effects within a layer.
