## Phase 5 — Update DSL Facades

**Files changed**: 1

<details>
<summary><code>mtg-sdk/.../dsl/Effects.kt</code></summary>

```kotlin
// Before:
fun DealDamage(amount: Int, target: EffectTarget = EffectTarget.AnyTarget): Effect =
    DealDamageEffect(amount, target)

fun GainLife(amount: Int, target: EffectTarget = EffectTarget.Controller): Effect =
    GainLifeEffect(amount, target)

fun LoseLife(amount: Int, target: EffectTarget = EffectTarget.Opponent): Effect =
    LoseLifeEffect(amount, target)

fun Discard(count: Int, target: EffectTarget = EffectTarget.Opponent): Effect =
    DiscardCardsEffect(count, target)

fun Sacrifice(filter: GameObjectFilter, count: Int = 1, target: EffectTarget = EffectTarget.Opponent): Effect =
    ForceSacrificeEffect(filter, count, target)

// After:
fun DealDamage(amount: Int, target: EffectTarget): Effect =  // REMOVED default — force explicit
    DealDamageEffect(amount, target)

fun GainLife(amount: Int, target: EffectTarget = EffectTarget.Controller): Effect =
    GainLifeEffect(amount, target)  // Controller default is fine

fun LoseLife(amount: Int, target: EffectTarget = EffectTarget.PlayerRef(Player.TargetOpponent)): Effect =
    LoseLifeEffect(amount, target)

fun Discard(count: Int, target: EffectTarget = EffectTarget.PlayerRef(Player.TargetOpponent)): Effect =
    DiscardCardsEffect(count, target)

fun Sacrifice(
    filter: GameObjectFilter,
    count: Int = 1,
    target: EffectTarget = EffectTarget.PlayerRef(Player.TargetOpponent)
): Effect = ForceSacrificeEffect(filter, count, target)
```

Note: `DealDamage` loses its default. Every damage effect should explicitly declare its target. This prevents the ambiguity of "who does this damage hit?"

</details>

After removing the `DealDamage` default, verify all card files that call `Effects.DealDamage(amount)` without a target argument — they will need an explicit target added. Scan with:

```bash
grep -rn "DealDamage(" mtg-sets/ --include="*.kt" | grep -v "target"
```
