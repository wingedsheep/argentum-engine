package com.wingedsheep.mtg.sets.definitions.arn.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Singing Tree
 * {3}{G}
 * Creature — Plant
 * 0/3
 * {T}: Target attacking creature has base power 0 until end of turn.
 */
val SingingTree = card("Singing Tree") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Plant"
    power = 0
    toughness = 3
    oracleText = "{T}: Target attacking creature has base power 0 until end of turn."

    activatedAbility {
        cost = Costs.Tap
        val creature = target(
            "target attacking creature",
            TargetObject(filter = TargetFilter(GameObjectFilter.Creature.attacking()))
        )
        effect = Effects.SetBasePower(creature, DynamicAmount.Fixed(0), Duration.EndOfTurn)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "54"
        artist = "Rob Alexander"
        imageUri = "https://cards.scryfall.io/normal/front/3/0/3003bf1e-8085-45d8-882b-c449109e7631.jpg?1562903854"
    }
}
