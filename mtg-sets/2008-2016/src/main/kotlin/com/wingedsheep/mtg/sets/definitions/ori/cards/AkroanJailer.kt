package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Akroan Jailer
 * {W}
 * Creature — Human Soldier
 * 1/1
 * {2}{W}, {T}: Tap target creature.
 */
val AkroanJailer = card("Akroan Jailer") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 1
    toughness = 1
    oracleText = "{2}{W}, {T}: Tap target creature."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{W}"), Costs.Tap)
        val creature = target("target creature", Targets.Creature)
        effect = Effects.Tap(creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "1"
        artist = "Anastasia Ovchinnikova"
        flavorText = "He ensures escape attempts are just that—attempts."
        imageUri = "https://cards.scryfall.io/normal/front/8/d/8d96bc2b-2e31-4654-b192-c3f023d9fde6.jpg?1783938366"
    }
}
