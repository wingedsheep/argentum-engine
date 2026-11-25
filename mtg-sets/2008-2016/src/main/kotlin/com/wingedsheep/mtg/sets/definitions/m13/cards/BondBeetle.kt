package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Bond Beetle
 * {G}
 * Creature — Insect
 * 0/1
 * When this creature enters, put a +1/+1 counter on target creature.
 */
val BondBeetle = card("Bond Beetle") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Insect"
    oracleText = "When this creature enters, put a +1/+1 counter on target creature."
    power = 0
    toughness = 1

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "161"
        artist = "John Avon"
        flavorText = "A beetle on your shoulder\n—Elvish expression meaning \"a blessing of luck\""
        imageUri = "https://cards.scryfall.io/normal/front/f/3/f341ed2c-353b-49a3-b200-94ae43cb8e24.jpg"
    }
}
