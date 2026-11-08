package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Breaching Hippocamp
 * {3}{U}
 * Creature — Horse Fish
 * 3/2
 * Flash
 * When this creature enters, untap another target creature you control.
 */
val BreachingHippocamp = card("Breaching Hippocamp") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Horse Fish"
    oracleText = "Flash (You may cast this spell any time you could cast an instant.)\n" +
        "When this creature enters, untap another target creature you control."
    power = 3
    toughness = 2

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.OtherCreatureYouControl)
        effect = Effects.Untap(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "43"
        artist = "Christopher Burdett"
        flavorText = "Don't try to ride this steed unless you've got gills too."
        imageUri = "https://cards.scryfall.io/normal/front/8/8/8841c01e-015e-4541-942a-f8859dd03fea.jpg?1783939802"
    }
}
