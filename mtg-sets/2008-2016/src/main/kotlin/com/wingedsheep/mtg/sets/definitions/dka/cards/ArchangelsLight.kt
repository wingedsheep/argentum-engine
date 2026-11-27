package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Archangel's Light
 * {7}{W}
 * Sorcery
 *
 * You gain 2 life for each card in your graveyard, then shuffle your graveyard into your library.
 */
val ArchangelsLight = card("Archangel's Light") {
    manaCost = "{7}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "You gain 2 life for each card in your graveyard, then shuffle your graveyard into your library."

    spell {
        effect = Effects.GainLife(DynamicAmounts.cardsInYourGraveyard() * 2) then
            Patterns.Library.shuffleGraveyardIntoLibrary(EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "1"
        artist = "Volkan Baǵa"
        flavorText = "\"This is the light of Avacyn. Even in her absence she offers us hope.\"\n—Radulf, priest of Avacyn"
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f99837b3-b487-43bb-846b-7a0e8afb6eef.jpg?1783940857"
        ruling(
            "2011-01-22",
            "Archangel's Light isn't put into the graveyard until after its instructions are followed. " +
                "You won't gain 2 life for it, and it won't be shuffled into your library."
        )
        ruling(
            "2011-01-22",
            "If you have no cards in your graveyard, you won't gain any life, but you'll still shuffle your library."
        )
    }
}
