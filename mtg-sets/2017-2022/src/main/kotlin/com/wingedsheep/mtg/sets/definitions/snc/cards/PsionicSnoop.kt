package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Psionic Snoop
 * {2}{U}
 * Creature — Human Rogue
 * 0 / 3
 * Flash
 * When this creature enters, it connives.
 */
val PsionicSnoop = card("Psionic Snoop") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Rogue"
    oracleText = "Flash\n" +
        "When this creature enters, it connives. (Draw a card, then discard a card. If you discarded a nonland card, put a +1/+1 counter on this creature.)"
    power = 0
    toughness = 3

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Connive()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "53"
        artist = "Marcela Medeiros"
        flavorText = "She knows your next move before you do."
        imageUri = "https://cards.scryfall.io/normal/front/b/1/b1e04834-7335-4b25-bedf-b7739c5c3aad.jpg"
    }
}
