package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Chandra's Triumph — War of the Spark #121 (canonical printing)
 * {1}{R}
 * Instant
 * Chandra's Triumph deals 3 damage to target creature or planeswalker an opponent controls.
 * Chandra's Triumph deals 5 damage instead if you control a Chandra planeswalker.
 *
 * "Instead" changes the amount, not the event: one damage event whose amount is a
 * [DynamicAmounts.conditional] (the Electrostatic Bolt shape). The Chandra check is read on
 * resolution, so a Chandra that leaves in response drops the spell back to 3.
 */
val ChandrasTriumph = card("Chandra's Triumph") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Chandra's Triumph deals 3 damage to target creature or planeswalker an opponent controls. " +
        "Chandra's Triumph deals 5 damage instead if you control a Chandra planeswalker."

    spell {
        val victim = target(TargetFilter(GameObjectFilter.CreatureOrPlaneswalker.opponentControls()))
        effect = Effects.DealDamage(
            amount = DynamicAmounts.conditional(
                condition = Conditions.YouControl(GameObjectFilter.Planeswalker.withSubtype("Chandra")),
                ifTrue = 5,
                ifFalse = 3
            ),
            target = victim
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "121"
        artist = "Kieran Yanner"
        flavorText = "Her triumph came not from achieving her ideal self, but from rejecting the idea of perfection."
        imageUri = "https://cards.scryfall.io/normal/front/7/0/70f43c17-7022-4052-86b8-90b74382b7bf.jpg?1783933431"
    }
}
