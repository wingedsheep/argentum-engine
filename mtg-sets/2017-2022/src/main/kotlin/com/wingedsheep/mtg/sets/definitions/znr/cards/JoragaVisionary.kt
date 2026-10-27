package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Joraga Visionary — Zendikar Rising #188
 * {3}{G} · Creature — Elf Wizard · 3/2
 *
 * When this creature enters, draw a card.
 */
val JoragaVisionary = card("Joraga Visionary") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Wizard"
    oracleText = "When this creature enters, draw a card."
    power = 3
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "188"
        artist = "Andrey Kuzinskiy"
        flavorText = "\"If you only follow where the paths lead, you merely go where others have gone before you.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a1ea2c17-382a-45e4-ac34-88bf8ba47169.jpg"
    }
}
