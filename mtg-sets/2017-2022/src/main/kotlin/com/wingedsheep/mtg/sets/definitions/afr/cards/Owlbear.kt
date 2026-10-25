package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Owlbear
 * {3}{G}{G}
 * Creature — Bird Bear
 * 4/4
 * Trample
 * Keen Senses — When this creature enters, draw a card.
 *
 * "Keen Senses" is a flavour ability word with no rules meaning, so the line is a plain
 * enters trigger over [Effects.DrawCards].
 */
val Owlbear = card("Owlbear") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Bird Bear"
    power = 4
    toughness = 4
    oracleText = "Trample\nKeen Senses — When this creature enters, draw a card."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(1)
        description = "Keen Senses — When this creature enters, draw a card."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "198"
        artist = "Ilse Gort"
        flavorText = "Many people believe a misguided wizard created the first owlbear, but others insist that these creatures have always dwelled in the Feywild."
        imageUri = "https://cards.scryfall.io/normal/front/3/0/30e8a00f-8131-470d-8072-4c23b812281a.jpg?1783926456"
    }
}
