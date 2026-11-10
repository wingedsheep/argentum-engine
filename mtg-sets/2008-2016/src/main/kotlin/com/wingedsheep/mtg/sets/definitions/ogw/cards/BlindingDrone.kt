package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Blinding Drone
 * {1}{U}
 * Creature — Eldrazi Drone
 * 1/3
 * Devoid (This card has no color.)
 * {C}, {T}: Tap target creature. ({C} represents colorless mana.)
 */
val BlindingDrone = card("Blinding Drone") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Eldrazi Drone"
    power = 1
    toughness = 3
    oracleText = "Devoid (This card has no color.)\n" +
        "{C}, {T}: Tap target creature. ({C} represents colorless mana.)"

    keywords(Keyword.DEVOID)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{C}"), Costs.Tap)
        val creature = target(TargetFilter.Creature)
        effect = Effects.Tap(creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "41"
        artist = "Lake Hurwitz"
        flavorText = "\"Zendikar's true gods have risen. Whether in reverence or agony, all the world will kneel.\"\n—Ayli, high priest of the Eternal Pilgrims"
        imageUri = "https://cards.scryfall.io/normal/front/4/e/4eb60957-0811-40c3-a92b-cce3e6a94745.jpg"
    }
}
