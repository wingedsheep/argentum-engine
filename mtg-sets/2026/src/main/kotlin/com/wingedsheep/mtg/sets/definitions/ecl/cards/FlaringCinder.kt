package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Flaring Cinder
 * {1}{U/R}{U/R}
 * Creature — Elemental Sorcerer
 * 3/2
 * When this creature enters and whenever you cast a spell with mana value 4 or greater,
 * you may discard a card. If you do, draw a card.
 */
val FlaringCinder = card("Flaring Cinder") {
    manaCost = "{1}{U/R}{U/R}"
    colorIdentity = "UR"
    typeLine = "Creature — Elemental Sorcerer"
    power = 3
    toughness = 2
    oracleText = "When this creature enters and whenever you cast a spell with mana value 4 or greater, " +
        "you may discard a card. If you do, draw a card."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.May(
            effect = Effects.IfYouDo(Patterns.Hand.discardCards(1), Effects.DrawCards(1)),
            descriptionOverride = "You may discard a card. If you do, draw a card."
        )
    }

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Any.manaValueAtLeast(4))
        effect = Effects.May(
            effect = Effects.IfYouDo(Patterns.Hand.discardCards(1), Effects.DrawCards(1)),
            descriptionOverride = "You may discard a card. If you do, draw a card."
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "225"
        artist = "Kai Carpenter"
        flavorText = "\"Am I losing myself or realizing my true nature?\""
        imageUri = "https://cards.scryfall.io/normal/front/0/6/0691a1a9-18e7-44b7-9a34-764b1ab45a76.jpg?1767862636"
    }
}
