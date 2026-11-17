package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Jeskai Sage
 * {1}{U}
 * Creature — Human Monk
 * 1/1
 *
 * Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)
 * When this creature dies, draw a card.
 */
val JeskaiSage = card("Jeskai Sage") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Monk"
    oracleText = "Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)\n" +
        "When this creature dies, draw a card."
    power = 1
    toughness = 1

    prowess()

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "38"
        artist = "Craig J Spearing"
        flavorText = "\"The one who conquers the mind is greater than the one who conquers the world.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/d/6dff4734-7b2a-4089-a78a-46199818f39d.jpg?1783938705"
    }
}
