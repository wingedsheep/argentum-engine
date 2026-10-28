package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Command Decision
 * {1}{W}
 * Sorcery
 *
 * Choose one —
 * • Destroy target creature with power 4 or greater.
 * • Draw a card.
 */
val CommandDecision = card("Command Decision") {
    manaCost = "{1}{W}"
    typeLine = "Sorcery"
    colorIdentity = "W"
    oracleText = "Choose one —\n• Destroy target creature with power 4 or greater.\n• Draw a card."
    spell {
        effect = Effects.Modal(listOf(
            mode("Destroy target creature with power 4 or greater") {
                val creature = target(TargetFilter.Creature.powerAtLeast(4))
                effect = Effects.Destroy(creature)
            },
            mode("Draw a card") { effect = Effects.DrawCards(1) }
        ))
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "8"
        artist = "Raymond Bonilla"
        flavorText = "\"If we're going to be damned, let's be damned for what we really are.\"\n—Captain Jean-Luc Picard"
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9f1f789b-5c08-4a4d-b3a3-4d51ad9f1a4d.jpg?1785981318"
    }
}
