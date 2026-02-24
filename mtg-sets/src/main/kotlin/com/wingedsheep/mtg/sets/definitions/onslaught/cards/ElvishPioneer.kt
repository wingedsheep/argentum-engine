package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Elvish Pioneer
 * {G}
 * Creature — Elf Druid
 * 1/1
 * When Elvish Pioneer enters the battlefield, you may put a basic land card
 * from your hand onto the battlefield tapped.
 */
val ElvishPioneer = card("Elvish Pioneer") {
    manaCost = "{G}"
    typeLine = "Creature — Elf Druid"
    power = 1
    toughness = 1
    oracleText = "When Elvish Pioneer enters the battlefield, you may put a basic land card from your hand onto the battlefield tapped."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = EffectPatterns.putFromHand(
            filter = GameObjectFilter.BasicLand,
            entersTapped = true
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "257"
        artist = "Christopher Rush"
        flavorText = "\"Ravaged or reborn, the land always provides.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7e71fc2d-643b-4fad-89a8-624d330895d6.jpg?1562924784"
    }
}
