package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SacrificeSelfEffect

/**
 * Blistering Firecat
 * {1}{R}{R}{R}
 * Creature — Elemental Cat
 * 7/1
 * Trample, haste
 * At the beginning of the end step, sacrifice Blistering Firecat.
 * Morph {R}{R}
 */
val BlisteringFirecat = card("Blistering Firecat") {
    manaCost = "{1}{R}{R}{R}"
    typeLine = "Creature — Elemental Cat"
    power = 7
    toughness = 1

    keywords(Keyword.TRAMPLE, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.EachEndStep
        effect = SacrificeSelfEffect
    }

    morph = "{R}{R}"

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "189"
        artist = "Dario Calmese"
        flavorText = "\"The next one who summons a cat is going to get it.\"\n—Arcanis the Omnipotent"
        imageUri = "https://cards.scryfall.io/large/front/e/0/e0ddcf4a-1943-49dd-a02c-75804ce4bc3e.jpg?1562948535"
    }
}
