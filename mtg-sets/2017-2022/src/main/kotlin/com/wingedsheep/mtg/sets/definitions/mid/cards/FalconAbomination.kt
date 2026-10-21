package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Falcon Abomination
 * {2}{U}
 * Creature — Zombie Bird
 * 2/2
 * Flying
 * When this creature enters, create a 2/2 black Zombie creature token with decayed.
 *
 * The token carries the Decayed keyword, which the engine realizes as "can't block" plus the
 * attack-triggered end-of-combat sacrifice (CR 702.147a).
 */
val FalconAbomination = card("Falcon Abomination") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Zombie Bird"
    power = 2
    toughness = 2
    oracleText = "Flying\nWhen this creature enters, create a 2/2 black Zombie creature token with decayed. " +
        "(It can't block. When it attacks, sacrifice it at end of combat.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Zombie"),
            keywords = setOf(Keyword.DECAYED),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "52"
        artist = "Brent Hollowell"
        flavorText = "The foulest of fowl."
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f0668413-859d-4ec4-a4d8-17ec7f5b1b8b.jpg?1783925641"
    }
}
