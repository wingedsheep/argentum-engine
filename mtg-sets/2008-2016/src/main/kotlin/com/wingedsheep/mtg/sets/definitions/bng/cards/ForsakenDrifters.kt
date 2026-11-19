package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Forsaken Drifters
 * {3}{B}
 * Creature — Zombie
 * 4/2
 *
 * When this creature dies, mill four cards.
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing.
 */
val ForsakenDrifters = card("Forsaken Drifters") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    power = 4
    toughness = 2
    oracleText = "When this creature dies, mill four cards."

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Patterns.Library.mill(4)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "72"
        artist = "Min Yum"
        flavorText = "It is a terrible thing when the path of the Returned leads them back to the Underworld."
        imageUri = "https://cards.scryfall.io/normal/front/0/5/05445d18-3590-4216-9392-9438d9f9395d.jpg?1783939556"
    }
}
