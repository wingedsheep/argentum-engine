package com.wingedsheep.mtg.sets.definitions.ltc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Relic of Sauron
 * {4}
 * Artifact
 * {T}: Add two mana in any combination of {U}, {B}, and/or {R}.
 * {3}, {T}: Draw two cards, then discard a card.
 */
val RelicOfSauron = card("Relic of Sauron") {
    manaCost = "{4}"
    colorIdentity = "UBR"
    typeLine = "Artifact"
    oracleText = "{T}: Add two mana in any combination of {U}, {B}, and/or {R}.\n" +
        "{3}, {T}: Draw two cards, then discard a card."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddManaInAnyCombination(
            amount = 2,
            allowedColors = setOf(Color.BLUE, Color.BLACK, Color.RED)
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap)
        effect = Patterns.Hand.loot(draw = 2, discard = 1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "79"
        artist = "Anton Solovianchyk"
        flavorText = "While the power of Sauron grew, light and living things forsook his borders."
        imageUri = "https://cards.scryfall.io/normal/front/1/7/175b3d28-5c74-4972-9b5c-5e39762c78f4.jpg?1783916010"
    }
}
