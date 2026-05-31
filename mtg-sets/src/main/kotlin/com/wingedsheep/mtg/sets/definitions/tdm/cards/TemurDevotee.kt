package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.values.ManaColorSet

/**
 * Temur Devotee
 * {1}{U}
 * Creature — Human Druid
 * 3/3
 *
 * Defender
 * {1}: Add {G}, {U}, or {R}. Activate only once each turn.
 */
val TemurDevotee = card("Temur Devotee") {
    manaCost = "{1}{U}"
    colorIdentity = "GUR"
    typeLine = "Creature — Human Druid"
    power = 3
    toughness = 3
    oracleText = "Defender\n{1}: Add {G}, {U}, or {R}. Activate only once each turn."

    keywords(Keyword.DEFENDER)

    activatedAbility {
        cost = Costs.Mana("{1}")
        manaAbility = true
        restrictions = listOf(ActivationRestriction.OncePerTurn)
        effect = Effects.AddManaOfChoice(
            ManaColorSet.Specific(setOf(Color.GREEN, Color.BLUE, Color.RED))
        )
        description = "{1}: Add {G}, {U}, or {R}. Activate only once each turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "61"
        artist = "Marina Ortega Lorente"
        flavorText = "Whisperers are conduits to the spirits of the land. They provide healing, " +
            "guidance, and support to the Temur people."
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a2ef698e-5466-43bd-985d-020f2e5d8205.jpg?1743204205"
    }
}
