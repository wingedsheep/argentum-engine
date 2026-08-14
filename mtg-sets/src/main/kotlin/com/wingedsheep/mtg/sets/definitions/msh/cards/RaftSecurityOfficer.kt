package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Raft Security Officer — Marvel Super Heroes #33
 * {1}{W} · Creature — Human Soldier · 1/3
 *
 * {2}, {T}: Tap target creature. This ability costs {1} less to activate if it targets a
 * creature with power 3 or less.
 *
 * Modeling notes:
 *  - The discount is [com.wingedsheep.sdk.scripting.ActivatedAbility.genericCostReduction]
 *    (Starport Security / Hylda's Crown of Winter's shape), but *target-dependent*: the gate is
 *    `Conditions.TargetMatchesFilter`, which reads the chosen target out of the effect context.
 *    `ActivateAbilityHandler.applyGenericCostReduction` builds that context from the targets the
 *    player actually picked, so the exact per-target cost is what gets paid; the legal-action
 *    enumerator previews the *cheapest* reachable cost across legal targets, which is only ever
 *    lower, so it never under-taps. Per the standard cost-reduction ruling this is locked in as
 *    the ability is activated, before costs are paid — a later power change doesn't refund or
 *    surcharge.
 *  - Power is read from projected state by the predicate evaluator, so a creature pumped down to
 *    3 or less by a continuous effect counts, and one pumped above 3 doesn't.
 */
val RaftSecurityOfficer = card("Raft Security Officer") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 1
    toughness = 3
    oracleText = "{2}, {T}: Tap target creature. This ability costs {1} less to activate if it " +
        "targets a creature with power 3 or less."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        val restrained = target("target creature", TargetCreature())
        effect = Effects.Tap(restrained)
        genericCostReduction = DynamicAmount.Conditional(
            condition = Conditions.TargetMatchesFilter(GameObjectFilter.Creature.powerAtMost(3)),
            ifTrue = DynamicAmount.Fixed(1),
            ifFalse = DynamicAmount.Fixed(0),
        )
        description = "{2}, {T}: Tap target creature. This ability costs {1} less to activate if " +
            "it targets a creature with power 3 or less."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "33"
        artist = "Bartek Fedyczak"
        flavorText = "\"We've upgraded the power dampeners since your last escape. No more breakouts on my watch.\"\n—[Name withheld on S.H.I.E.L.D. orders], last words"
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f9b9f9d6-b50c-4b29-80be-284ba773c70b.jpg?1783902967"
    }
}
