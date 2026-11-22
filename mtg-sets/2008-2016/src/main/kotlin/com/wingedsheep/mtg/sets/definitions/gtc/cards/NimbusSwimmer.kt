package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters

/**
 * Nimbus Swimmer
 * {X}{G}{U}
 * Creature — Leviathan
 * 0/0
 * Flying
 * This creature enters with X +1/+1 counters on it.
 */
val NimbusSwimmer = card("Nimbus Swimmer") {
    manaCost = "{X}{G}{U}"
    colorIdentity = "UG"
    typeLine = "Creature — Leviathan"
    oracleText = "Flying\nThis creature enters with X +1/+1 counters on it."
    power = 0
    toughness = 0

    keywords(Keyword.FLYING)

    replacementEffect(EntersWithDynamicCounters(count = DynamicAmounts.xValue()))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "181"
        artist = "Howard Lyon"
        flavorText = "The Simic soon discovered that the sky offered as few constraints on size as the sea."
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d691cd3b-afe5-4f28-95a9-125475515126.jpg"
    }
}
