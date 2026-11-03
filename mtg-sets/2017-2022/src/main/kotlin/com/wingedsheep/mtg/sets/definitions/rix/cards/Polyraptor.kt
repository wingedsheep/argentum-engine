package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Polyraptor
 * {6}{G}{G}
 * Creature — Dinosaur
 * 5/5
 *
 * Enrage — Whenever this creature is dealt damage, create a token that's a copy of this creature.
 */
val Polyraptor = card("Polyraptor") {
    manaCost = "{6}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Dinosaur"
    power = 5
    toughness = 5
    oracleText = "Enrage — Whenever this creature is dealt damage, create a token that's a copy of this creature."

    triggeredAbility {
        trigger = Triggers.self.isDealtDamage()
        effect = Effects.CreateTokenCopyOfSelf()
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "144"
        artist = "Mark Behm"
        flavorText = "An innumerable pack is concealed in a single reflection."
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f8965a3a-93fe-4021-a665-b6013bdc86f7.jpg?1783935282"
    }
}
