package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.costs.CostAtom

/**
 * Add Renew (Tarkir: Dragonstorm, Sultai clan keyword).
 *
 * "Renew — [cost], Exile this card from your graveyard: [effect]. Activate only as a sorcery."
 *
 * Renew is a graveyard-activated ability. This helper composes it from existing primitives —
 * no new engine subsystem is involved:
 *  - the cost is the given mana [cost] plus [AbilityCost.ExileSelf] (the card exiles itself
 *    from the graveyard as part of the cost),
 *  - `activateFromZone = Zone.GRAVEYARD` so the engine's ZoneActivatedAbilityEnumerator surfaces it
 *    while the card sits in the graveyard, and
 *  - `timing = TimingRule.SorcerySpeed` to enforce "Activate only as a sorcery".
 *
 * The [init] lambda configures the effect (and any targets) exactly like `activatedAbility`;
 * its `cost`, `timing`, and `activateFromZone` fields are ignored — those are fixed by Renew.
 *
 * ```kotlin
 * renew("{2}{G}") {
 *     effect = Effects.PutCounters(Counters.PLUS_ONE_PLUS_ONE, 1, target("creature", Targets.Creature))
 * }
 * ```
 */
fun CardBuilder.renew(cost: String, init: ActivatedAbilityBuilder.() -> Unit) {
    val builder = ActivatedAbilityBuilder().apply(init)
    val renewEffect = requireNotNull(builder.effect) { "renew requires an effect" }
    activatedAbilities.add(
        ActivatedAbility(
            cost = AbilityCost.Composite(listOf(AbilityCost.Atom(CostAtom.Mana(ManaCost.parse(cost))), AbilityCost.ExileSelf)),
            effect = renewEffect,
            targetRequirements = builder.targetRequirements,
            timing = TimingRule.SorcerySpeed,
            restrictions = builder.restrictions,
            activateFromZone = Zone.GRAVEYARD,
            descriptionOverride = "Renew — $cost, Exile this card from your graveyard: " +
                "${renewEffect.description} Activate only as a sorcery."
        )
    )
    keywordSet.add(Keyword.RENEW)
}
