package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction

/**
 * Angus Mackenzie — Legends #217
 * {G}{W}{U} · Legendary Creature — Human Cleric · 2/2
 *
 * {G}{W}{U}, {T}: Prevent all combat damage that would be dealt this turn. Activate only before
 * the combat damage step.
 *
 * A Fog on a legend: the same turn-scoped [Effects.PreventAllCombatDamage] shield Fog installs.
 * "Before the combat damage step" is every step up to and including declare blockers — the
 * first-strike combat damage step is itself a combat damage step (as Berserk reads it), so the
 * window closes at [Step.FIRST_STRIKE_COMBAT_DAMAGE] and stays closed for the rest of the turn.
 */
val AngusMackenzie = card("Angus Mackenzie") {
    manaCost = "{G}{W}{U}"
    colorIdentity = "GUW"
    typeLine = "Legendary Creature — Human Cleric"
    power = 2
    toughness = 2
    oracleText = "{G}{W}{U}, {T}: Prevent all combat damage that would be dealt this turn. " +
        "Activate only before the combat damage step."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{G}{W}{U}"), Costs.Tap)
        effect = Effects.PreventAllCombatDamage()
        restrictions = listOf(ActivationRestriction.BeforeStep(Step.FIRST_STRIKE_COMBAT_DAMAGE))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "217"
        artist = "Bryon Wackwitz"
        flavorText = "\"Battles no longer served a purpose in Karakas.\" —Angus Mackenzie, *Diary*"
        imageUri = "https://cards.scryfall.io/normal/front/5/7/57264bd9-94f6-4d4d-baff-2b2900585635.jpg?1783948042"
    }
}
