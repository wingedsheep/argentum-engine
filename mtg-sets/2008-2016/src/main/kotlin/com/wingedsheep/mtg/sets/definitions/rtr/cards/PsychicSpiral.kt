package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Psychic Spiral
 * {4}{U}
 * Instant
 * Shuffle all cards from your graveyard into your library. Target player mills that many cards.
 *
 * Canonical printing: Return to Ravnica, the card's only printing.
 *
 * "That many" is the number of cards shuffled in, so the graveyard size is frozen with
 * [Effects.StoreNumber] *before* the shuffle empties it. Psychic Spiral is still on the stack while
 * it resolves, so it is neither shuffled in nor counted (rulings). The shuffle happens even with an
 * empty graveyard — the move executor still shuffles the library for an empty shuffled move.
 */
val PsychicSpiral = card("Psychic Spiral") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Shuffle all cards from your graveyard into your library. Target player mills that many cards."

    spell {
        val player = target(Targets.Player)
        effect = Effects.StoreNumber("psychicSpiralCount", DynamicAmounts.cardsInYourGraveyard()) then
            Patterns.Library.shuffleGraveyardIntoLibrary(EffectTarget.Controller) then
            Patterns.Library.mill(DynamicAmounts.storedNumber("psychicSpiralCount"), player)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "47"
        artist = "Ryan Pancoast"
        flavorText = "An elocutor forces criminals to confess to the laws they merely intended to break."
        imageUri = "https://cards.scryfall.io/normal/front/5/9/59b09c16-f611-4c90-a990-e22bf46bd0e2.jpg?1783940367"
        ruling(
            "2012-10-01",
            "Psychic Spiral won’t be shuffled into your library, and it won’t be counted when determining " +
                "how many cards the target player puts into their graveyard. It will be put into your " +
                "graveyard after you follow its instructions.",
        )
        ruling(
            "2012-10-01",
            "If you cast Psychic Spiral when there are no cards in your graveyard, you’ll still shuffle your library.",
        )
    }
}
