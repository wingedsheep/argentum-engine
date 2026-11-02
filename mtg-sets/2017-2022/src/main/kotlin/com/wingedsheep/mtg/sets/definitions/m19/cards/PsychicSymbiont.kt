package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Psychic Symbiont
 * {4}{U}{B}
 * Creature — Nightmare Horror
 * 3/3
 * Flying
 * When this creature enters, target opponent discards a card and you draw a card.
 */
val PsychicSymbiont = card("Psychic Symbiont") {
    manaCost = "{4}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Nightmare Horror"
    power = 3
    toughness = 3
    oracleText = "Flying\nWhen this creature enters, target opponent discards a card and you draw a card."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val opponent = target(Targets.Opponent)
        effect = Patterns.Hand.discardCards(1, opponent) then Effects.DrawCards(1)
        description = "When this creature enters, target opponent discards a card and you draw a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "221"
        artist = "Robbie Trevino"
        flavorText = "\"I turn around and see nothing, yet I know it is behind me. Its fingers rummage in my mind, crafting new ideas.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/1/3188e533-9a72-4a02-b169-a918f7c095ba.jpg?1783934519"
    }
}
