package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Merfolk Pupil
 * {1}{U}
 * Creature — Merfolk Wizard
 * 1/1
 * When this creature enters, draw a card, then discard a card.
 * {1}{U}, Exile this card from your graveyard: Draw a card, then discard a card.
 */
val MerfolkPupil = card("Merfolk Pupil") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Wizard"
    oracleText = "When this creature enters, draw a card, then discard a card.\n" +
        "{1}{U}, Exile this card from your graveyard: Draw a card, then discard a card."
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Hand.loot()
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{U}"), Costs.ExileSelf)
        effect = Patterns.Hand.loot()
        activateFromZone = Zone.GRAVEYARD
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "15"
        artist = "Caroline Gariba"
        flavorText = "Maintaining a personal bubble is even more important when you have gills."
        imageUri = "https://cards.scryfall.io/normal/front/4/2/42aacf99-39d1-4299-8557-6cc10ce9e35f.jpg"
    }
}
