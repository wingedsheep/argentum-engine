package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Dawnhart Rejuvenator (Innistrad: Midnight Hunt #180)
 * {3}{G}
 * Creature — Human Warlock
 * 2/4
 * When this creature enters, you gain 3 life.
 * {T}: Add one mana of any color.
 */
val DawnhartRejuvenator = card("Dawnhart Rejuvenator") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Warlock"
    oracleText = "When this creature enters, you gain 3 life.\n" +
        "{T}: Add one mana of any color."
    power = 2
    toughness = 4

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GainLife(3)
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana()
        manaAbility = true
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "180"
        artist = "Darren Tan"
        flavorText = "\"Wherever there is life, there is power for one who knows the old ways.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/f/cf4b5ad9-3de6-4d53-93a0-3b8d2e484687.jpg"
    }
}
