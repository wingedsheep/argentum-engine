package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Fissure Wizard — Zendikar Rising #140
 * {1}{R} · Creature — Goblin Wizard · 2/1
 *
 * When this creature enters, you may discard a card. If you do, draw a card.
 */
val FissureWizard = card("Fissure Wizard") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Wizard"
    oracleText = "When this creature enters, you may discard a card. If you do, draw a card."
    power = 2
    toughness = 1

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.May(
            Effects.IfYouDo(Patterns.Hand.discardCards(1), Effects.DrawCards(1))
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "140"
        artist = "Lius Lasahido"
        flavorText = "He might call up a burst of lava or a raging hellion from Akoum's volcanic depths. Or he might just fall in."
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9fa10a18-5059-46ab-9983-0ea78b507331.jpg"
    }
}
