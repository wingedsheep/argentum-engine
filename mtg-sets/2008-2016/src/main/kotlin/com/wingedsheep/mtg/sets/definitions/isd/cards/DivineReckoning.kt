package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Divine Reckoning
 * {2}{W}{W}
 * Sorcery
 * Each player chooses a creature they control. Destroy the rest.
 * Flashback {5}{W}{W}
 *
 * The Cataclysmic Gearhulk shape with a single category: `chooseOnePerCategory` asks each
 * controller in APNAP order (per the ruling) to keep one creature of their own, then every
 * unchosen creature is destroyed at once.
 */
val DivineReckoning = card("Divine Reckoning") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Each player chooses a creature they control. Destroy the rest.\n" +
        "Flashback {5}{W}{W} (You may cast this card from your graveyard for its flashback cost. Then exile it.)"

    spell {
        effect = Effects.Pipeline {
            val creatures = gather(Filters.Creature)
            val kept = chooseOnePerCategory(creatures, listOf(Filters.Creature))
            destroy(exclude(creatures, kept))
        }
    }

    keywordAbility(KeywordAbility.flashback("{5}{W}{W}"))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "10"
        artist = "Greg Staples"
        flavorText = "Survival of the purest."
        imageUri = "https://cards.scryfall.io/normal/front/4/4/446ea3a4-206a-4097-87c1-c04bb7812972.jpg?1783940996"
        ruling(
            "2011-09-22",
            "Starting with the player whose turn it is, each player chooses a creature in turn order. " +
                "Players will know the choice of each player who chose before them.",
        )
    }
}
