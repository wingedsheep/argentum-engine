package com.wingedsheep.mtg.sets.definitions.atq.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Priest of Yawgmoth
 * {1}{B}
 * Creature — Phyrexian Human Cleric
 * 1/2
 * {T}, Sacrifice an artifact: Add an amount of {B} equal to the sacrificed artifact's mana value.
 *
 * Composes from existing primitives — no engine work. The sacrifice cost binds the sacrificed
 * artifact to [EntityReference.Sacrificed] (its last-known information is captured at cost payment,
 * the same template as the rules' Bosh, Iron Golem example), and the mana ability adds {B} times
 * that artifact's mana value via
 * [DynamicAmount.EntityProperty] reading [EntityNumericProperty.ManaValue]. Mana value is a printed
 * characteristic, so it reads correctly even after the artifact has been sacrificed to the
 * graveyard — including {0} (adds no mana) and high-MV artifacts. Same shape as Metamorphosis (ARN).
 */
val PriestOfYawgmoth = card("Priest of Yawgmoth") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Phyrexian Human Cleric"
    power = 1
    toughness = 2
    oracleText = "{T}, Sacrifice an artifact: Add an amount of {B} equal to the sacrificed artifact's mana value."

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.Sacrifice(GameObjectFilter.Artifact))
        effect = Effects.AddMana(
            Color.BLACK,
            amount = DynamicAmount.EntityProperty(
                EntityReference.Sacrificed(0),
                EntityNumericProperty.ManaValue
            )
        )
        description = "{T}, Sacrifice an artifact: Add an amount of {B} equal to the sacrificed artifact's mana value."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "19"
        artist = "Mark Tedin"
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c9fd4054-42fc-4f95-a6f7-369a5da43dd5.jpg?1562937643"
    }
}
