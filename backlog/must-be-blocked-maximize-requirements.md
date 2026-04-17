# Bug: Blocking validator doesn't implement CR 509.1c (maximize requirements)

## Summary

When multiple "must be blocked" effects overlap — for example, **Taunting Elf**
("All creatures able to block ~ do so") and **Vinebred Brawler** ("~ must be
blocked if able") both attacking — the engine's blocking validator enforces
two *local* rules that can be jointly unsatisfiable, even though MTG's
Comprehensive Rules admit legal block assignments.

The user-visible effect: the defender cannot declare **any** legal set of
blockers. Every option returns "must block" / "must be blocked if able".

## Reproduction

See `VinebredBrawlerScenarioTest` in `game-server/src/test/kotlin/.../scenarios/`,
context **"Vinebred Brawler + Taunting Elf — overlapping blocking requirements"**.
The three tests prefixed `TODO engine bug:` pin the current (wrong) behavior:

- 1 able blocker — both "block TE" and "block VB" are rejected.
- 3 able blockers, all-on-TE — rejected as "Vinebred Brawler must be blocked if able".
- 3 able blockers, 2-on-TE + 1-on-VB — rejected as "X must block Taunting Elf".

Per CR 509.1c all three block assignments should be **legal** (they each
satisfy the maximum number of requirements achievable).

## The MTG rule (CR 509.1c)

> The defending player checks each of the chosen creatures to see whether
> they're affected by any requirements. If the number of requirements being
> obeyed is fewer than the maximum possible number of requirements that
> could be obeyed without violating any restrictions, the declaration of
> blockers is illegal.

Each effect produces "requirements":

- **Taunting Elf** (`MustBeBlockedEffect(allCreatures = true)`) — one
  requirement **per able blocker**: "this blocker must block Taunting Elf".
- **Vinebred Brawler** (`MustBeBlockedEffect(allCreatures = false)`) — exactly
  **one** requirement: "at least one creature must block Vinebred Brawler".

With 3 able blockers, the maximum satisfiable is 3:

| Assignment          | TE reqs met | VB req met | Total | Legal? |
|---------------------|-------------|------------|-------|--------|
| 3 on TE, 0 on VB    | 3           | 0          | 3     | ✅     |
| 2 on TE, 1 on VB    | 2           | 1          | 3     | ✅     |
| 1 on TE, 2 on VB    | 1           | 1          | 2     | ❌     |
| 0 on TE, 3 on VB    | 0           | 1          | 1     | ❌     |

With 1 able blocker, the maximum is 1 (either requirement, but not both —
one creature can only block one attacker):

| Assignment | TE reqs met | VB req met | Total | Legal? |
|------------|-------------|------------|-------|--------|
| 1 on TE    | 1           | 0          | 1     | ✅     |
| 1 on VB    | 0           | 1          | 1     | ✅     |

## Root cause

File: `rules-engine/src/main/kotlin/com/wingedsheep/engine/mechanics/combat/BlockPhaseManager.kt`,
function `validateMustBeBlockedRequirements` (roughly lines 438–505).

The function enforces two local rules that don't account for each other:

**Rule (a) — "Must be blocked by all" (Taunting Elf)** at lines ~454–486:

```kotlin
for (blockerId in potentialBlockers) {
    val canBlockThese = mustBeBlockedByAllAttackers.filter { attackerId ->
        canCreatureBlockAttacker(state, blockerId, attackerId, blockingPlayer, projected)
    }
    if (canBlockThese.isEmpty()) continue

    val actuallyBlocking = blockerToAttackers[blockerId] ?: emptySet()
    val blockingMustBeBlocked = actuallyBlocking.intersect(mustBeBlockedByAllAttackers.toSet())
    if (blockingMustBeBlocked.isEmpty()) {
        return "$blockerName must block ..." // ← too strict
    }
}
```

This says: *every* potential blocker that can block Taunting Elf MUST block
Taunting Elf (or another must-be-blocked-by-all attacker). That's stricter
than CR 509.1c — the MTG rule allows a blocker to break a Taunting-Elf
requirement if doing so satisfies a stronger requirement elsewhere (like
Vinebred Brawler's lone requirement).

**Rule (b) — "Must be blocked if able" (Vinebred Brawler)** at lines ~488–502:

```kotlin
for (attackerId in mustBeBlockedIfAbleAttackers) {
    val blockersAssigned = attackerToBlockers[attackerId] ?: emptySet()
    if (blockersAssigned.isNotEmpty()) continue
    val canBeBlocked = potentialBlockers.any { blockerId ->
        canCreatureBlockAttacker(state, blockerId, attackerId, blockingPlayer, projected)
    }
    if (!canBeBlocked) continue
    return "$attackerName must be blocked if able" // ← too strict
}
```

This says: Vinebred Brawler MUST have a blocker whenever any potential
blocker could block it — without considering whether all potential blockers
are already pinned to Taunting-Elf requirements.

When both rules are active on a board with any able blocker, at least one
rule always fires on every assignment.

## Suggested fix

Replace the two independent loops with a single validation pass that
implements CR 509.1c directly. A reference shape:

1. **Enumerate requirements.** For the current declared blockers, count the
   requirements that would be obeyed under this assignment:
   - For each must-be-blocked-by-all attacker `A`, one requirement per able
     blocker: "this blocker blocks `A`". Obeyed iff that blocker's assigned
     attacker set contains `A`.
   - For each must-be-blocked-if-able attacker `A`, one requirement: "`A` has
     ≥1 blocker". Obeyed iff its blocker set is non-empty.
   - (Existing provoke, menace, CantBlockUnless logic continues to act as
     *restrictions*, i.e. hard constraints.)

2. **Compute the maximum.** Determine the maximum number of requirements
   that any legal assignment could obey, given the restrictions and the
   pool of potential blockers. This is a small combinatorial search — with
   the typical counts in MTG (a handful of attackers, a handful of blockers),
   an exhaustive or branch-and-bound search is fine.

3. **Reject if sub-maximal.** If the player's declared assignment obeys
   fewer requirements than the maximum, return an error; otherwise accept.
   The error should name a specific missed requirement for usability.

4. **Tie-handling is automatic.** Several distinct assignments may all hit
   the maximum — the rule accepts *any* of them, which naturally gives the
   defender the MTG-correct freedom of choice.

An acceptable first pass may skip step 2's optimization work and instead
brute-force: enumerate legal assignments (respecting restrictions only),
compute each one's satisfied-requirement count, and accept the player's
choice iff its count equals the max.

### Entry points

- `BlockPhaseManager.validateMustBeBlockedRequirements` — the logic above.
- `findMustBeBlockedAttackers` / `findMustBeBlockedIfAbleAttackers` — already
  enumerate the two flavors of must-block attackers. Reuse.
- `canCreatureBlockAttacker` — evasion/cost/protection check for the
  restriction side. Reuse unchanged.
- Consider extracting a `RequirementModel` data class so provoke and future
  requirement-style effects (e.g. "this creature must be blocked by a
  creature with flying") fit the same framework.

## Tests

When the fix lands, flip these three tests in
`VinebredBrawlerScenarioTest` (the "TODO engine bug" cases) to assert
`error shouldBe null` instead of the current "documents current behavior"
shape:

1. `TODO engine bug: 1 blocker + both attacking — no legal block exists (should allow either)`
   → Both `blockTaunting` and `blockBrawler` should succeed.
2. `TODO engine bug: 3 blockers — all-on-Taunting-Elf should be legal per CR 509.1c tie`
   → `allOnTaunting.error shouldBe null`.
3. `TODO engine bug: 3 blockers — 2 on Taunting Elf + 1 on Vinebred Brawler should be legal`
   → `split.error shouldBe null`.

The fourth test in that context (`three blockers — 2 on Vinebred Brawler + 1
on Taunting Elf is illegal`) must keep passing — that assignment satisfies
only 2 requirements when 3 are achievable, so the max-requirements rule
still rejects it.

All pre-existing `TauntingElfScenarioTest` cases must keep passing — they
exercise Taunting Elf in isolation, where the local "every able blocker
must block TE" rule coincides with the CR 509.1c result.

## Out of scope for this fix

- Provoke, menace, "must be blocked by a creature with X" — these already
  have their own validators. Consider folding them into the same framework
  later, but the immediate fix only needs to handle the `MustBeBlockedByAll`
  + `MustBeBlockedIfAble` interaction.
- Attacker-side requirements (CR 508 — "must attack if able"). Same *shape*
  of bug may exist there, but it's a separate file
  (`AttackPhaseManager`/legal-actions enumerators) and not covered here.
