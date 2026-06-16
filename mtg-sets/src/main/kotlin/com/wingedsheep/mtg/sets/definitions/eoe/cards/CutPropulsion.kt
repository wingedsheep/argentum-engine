package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

val CutPropulsion = card("Cut Propulsion") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Target creature deals damage to itself equal to its power. If that creature has flying, it deals twice that much damage to itself instead."

    spell {
        val creature = target("creature", Targets.Creature)
        val power = DynamicAmount.EntityProperty(EntityReference.Target(0), EntityNumericProperty.Power)
        effect = Effects.DealDamage(
            amount = DynamicAmount.Conditional(
                condition = Conditions.TargetMatchesFilter(
                    filter = GameObjectFilter.Creature.withKeyword(Keyword.FLYING),
                    targetIndex = 0
                ),
                ifTrue = DynamicAmount.Multiply(power, 2),
                ifFalse = power
            ),
            target = creature,
            damageSource = creature
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "130"
        artist = "Andrea Piparo"
        flavorText = "When fighting Kav, always expect each action to have an unequal and opposite overreaction."
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d96f1c41-3d11-48a8-b962-db46a2d054de.jpg?1752947077"
    }
}
