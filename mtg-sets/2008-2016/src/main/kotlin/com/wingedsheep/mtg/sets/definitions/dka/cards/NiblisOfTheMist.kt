package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Niblis of the Mist
 * {2}{W}
 * Creature — Spirit
 * 2/1
 * Flying
 * When this creature enters, you may tap target creature.
 */
val NiblisOfTheMist = card("Niblis of the Mist") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Spirit"
    power = 2
    toughness = 1
    oracleText = "Flying\nWhen this creature enters, you may tap target creature."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature)
        effect = Effects.May(Effects.Tap(creature))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "15"
        artist = "Igor Kieryluk"
        flavorText = "\"Not even a roaring fire could thaw the chill it put in my heart.\"\n—Vonn, geist-trapper"
        imageUri = "https://cards.scryfall.io/normal/front/0/8/08aea6e3-c8a8-4964-b95d-4c639da55de1.jpg"
    }
}
