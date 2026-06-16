package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Dunland Crebain
 * {2}{B}
 * Creature — Bird Horror
 * 1/1
 *
 * Flying
 * When this creature enters, amass Orcs 2. (Put two +1/+1 counters on an Army you control. It's
 * also an Orc. If you don't control an Army, create a 0/0 black Orc Army creature token first.)
 */
val DunlandCrebain = card("Dunland Crebain") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Bird Horror"
    power = 1
    toughness = 1
    oracleText = "Flying\n" +
        "When this creature enters, amass Orcs 2. (Put two +1/+1 counters on an Army you control. " +
        "It's also an Orc. If you don't control an Army, create a 0/0 black Orc Army creature token first.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Amass(2, "Orc")
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "82"
        flavorText = "\"What's that, Strider? It don't look like a cloud.\"\n—Sam"
        artist = "Alexander Ostrowski"
        imageUri = "https://cards.scryfall.io/normal/front/6/9/695c05ab-e46e-46c7-bd2e-ef0b2307e449.jpg?1686968429"
    }
}
