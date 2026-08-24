package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Ring of Renewal
 * {5}
 * Artifact
 * {5}, {T}: Discard a card at random, then draw two cards.
 *
 * The discard is part of the *effect*, not the cost, so a player with an empty hand still draws.
 */
val RingOfRenewal = card("Ring of Renewal") {
    manaCost = "{5}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{5}, {T}: Discard a card at random, then draw two cards."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{5}"), Costs.Tap)
        effect = Effects.Composite(
            Patterns.Hand.discardRandom(1),
            Effects.DrawCards(2)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "89"
        artist = "Douglas Shuler"
        flavorText = "To the uninitiated, the Ring of Renewal is merely an oddity. For those fluent in the wielding of magic, however, it is a source of great knowledge."
        imageUri = "https://cards.scryfall.io/normal/front/a/5/a532d38a-809b-4132-8690-be15fe23afab.jpg?1783947880"
    }
}
