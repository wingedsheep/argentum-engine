package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Murder of Crows
 * {3}{U}{U}
 * Creature — Bird
 * 4/4
 * Flying
 * Whenever another creature dies, you may draw a card. If you do, discard a card.
 */
val MurderOfCrows = card("Murder of Crows") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Bird"
    power = 4
    toughness = 4
    oracleText = "Flying\nWhenever another creature dies, you may draw a card. If you do, discard a card."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature).dies()
        effect = Effects.May(Patterns.Hand.loot())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "70"
        artist = "Drew Baker"
        flavorText = "Even more than carrion, they crave the last words of the dying."
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f914f7e4-06fc-4943-8597-b7f834938c00.jpg?1783940968"
        ruling("2018-03-16", "If another creature dies at the same time as Murder of Crows, its last ability triggers.")
        ruling(
            "2018-03-16",
            "You can't do anything in between drawing a card and discarding a card, including casting or " +
                "cycling the card you drew."
        )
    }
}
