package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Mindeye Drake
 * {4}{U}
 * Creature — Drake
 * 2/5
 * Flying
 * When this creature dies, target player mills five cards.
 */
val MindeyeDrake = card("Mindeye Drake") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Drake"
    oracleText = "Flying\nWhen this creature dies, target player mills five cards."
    power = 2
    toughness = 5

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.dies()
        val player = target(Targets.Player)
        effect = Patterns.Library.mill(5, player)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "43"
        artist = "Lars Grant-West"
        flavorText = "They can map both the cityscape and the minds of its architects."
        imageUri = "https://cards.scryfall.io/normal/front/9/4/947f44b0-91be-4115-b499-57893f0f69a9.jpg"
    }
}
