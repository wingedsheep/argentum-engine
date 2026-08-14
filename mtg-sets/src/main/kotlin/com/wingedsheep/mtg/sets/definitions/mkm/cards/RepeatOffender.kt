package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Repeat Offender — Murders at Karlov Manor #101
 * {1}{B} · Creature — Human Assassin · 2/1
 *
 * {2}{B}: If this creature is suspected, put a +1/+1 counter on it. Otherwise, suspect it.
 *
 * One ability with a branch, not two abilities: the first activation suspects it (menace, can't
 * block — CR 701.60a), and every activation after that grows it. The branch is a *state test*, so
 * `ConditionalEffect` (which lowers to `GatedEffect` + `Gate.WhenCondition`) is the right shape —
 * no prompt, no pause, both branches resolve synchronously in the executor.
 *
 * The condition is checked on **resolution**, not activation, which is the interaction worth
 * knowing: holding priority and activating twice in response to itself resolves the second copy
 * first, and since the creature is not yet suspected at that point, *both* copies take the
 * "suspect it" branch — CR 701.60d makes the second one a no-op rather than a second counter.
 * `Conditions.SourceIsSuspected` reads the projected suspected designation rather than probing for
 * menace, so a creature that gained menace some other way doesn't trip the counter branch.
 *
 * "It" names the source in both branches, so both are `EffectTarget.Self` — there is no target and
 * the ability can't be fizzled by removing something else.
 */
val RepeatOffender = card("Repeat Offender") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Assassin"
    oracleText = "{2}{B}: If this creature is suspected, put a +1/+1 counter on it. Otherwise, " +
        "suspect it. (A suspected creature has menace and can't block.)"
    power = 2
    toughness = 1

    activatedAbility {
        cost = Costs.Mana("{2}{B}")
        effect = ConditionalEffect(
            condition = Conditions.SourceIsSuspected,
            effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
            elseEffect = Effects.Suspect(EffectTarget.Self)
        )
        description = "If this creature is suspected, put a +1/+1 counter on it. Otherwise, suspect it."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "101"
        artist = "Joshua Cairos"
        flavorText = "\"Blasted sketch artist did too good a job. Looks like it's time for me to " +
            "lie low for a little while.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0c2ca1e7-e0de-4d29-a81b-62185ccd295f.jpg?1783912892"

        ruling(
            "2024-02-02",
            "When an effect suspects a creature, it becomes suspected. It gains menace and \"This " +
                "creature can't block\" for as long as it's suspected. It stays suspected until it " +
                "leaves the battlefield or another effect causes it to no longer be suspected."
        )
        ruling("2024-02-02", "If a creature is already suspected, suspecting it again won't have any effect.")
        ruling(
            "2024-02-02",
            "Being suspected isn't a copiable value. If a permanent becomes a copy of a suspected " +
                "creature, it won't be suspected."
        )
    }
}
