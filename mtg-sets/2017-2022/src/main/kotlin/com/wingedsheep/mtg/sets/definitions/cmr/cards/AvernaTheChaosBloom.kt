package com.wingedsheep.mtg.sets.definitions.cmr.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CascadeMayPutOntoBattlefield

/**
 * Averna, the Chaos Bloom
 * {G}{U}{R}
 * Legendary Creature — Elemental Shaman
 * 4/2
 * As you cascade, you may put a land card from among the exiled cards onto the battlefield tapped.
 * (Do this after the last card is exiled but before deciding whether to cast a nonland card.)
 */
val AvernaTheChaosBloom = card("Averna, the Chaos Bloom") {
    manaCost = "{G}{U}{R}"
    colorIdentity = "GUR"
    typeLine = "Legendary Creature — Elemental Shaman"
    power = 4
    toughness = 2
    oracleText = "As you cascade, you may put a land card from among the exiled cards onto the battlefield tapped. " +
        "(Do this after the last card is exiled but before deciding whether to cast a nonland card.)"

    staticAbility {
        ability = CascadeMayPutOntoBattlefield()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "269"
        artist = "Lucas Graciano"
        flavorText = "\"Nature is a dance. Master it, and the world can keep no secrets from you.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/4/e46b3ef3-3ba9-4da7-a675-86468a8da4c7.jpg?1783928776"
        ruling("2020-11-10", "If you don't cast the nonland card you reveal, or if you don't reveal any nonland cards with lesser mana value, you can still put a land card onto the battlefield before you finish cascading.")
        ruling("2020-11-10", "Averna's ability isn't the same as playing a land. You may do this even if you've already played a land during your turn and even if it isn't your turn at all.")
    }
}
