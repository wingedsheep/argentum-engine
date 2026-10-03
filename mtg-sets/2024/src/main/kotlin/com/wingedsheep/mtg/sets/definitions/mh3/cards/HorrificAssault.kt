package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Horrific Assault
 * {G}
 * Sorcery
 * Target creature you control deals damage equal to its power to target creature or planeswalker
 * you don't control. If you control an Eldrazi, you gain 3 life.
 *
 * "Eldrazi" is a bare tribal noun, so any Eldrazi *permanent* you control satisfies the condition.
 * Per the ruling, if either target is illegal the damage isn't dealt, but the life gain still
 * happens as long as one target is legal.
 */
val HorrificAssault = card("Horrific Assault") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Target creature you control deals damage equal to its power to target creature or " +
        "planeswalker you don't control. If you control an Eldrazi, you gain 3 life."

    spell {
        val myCreature = target(TargetFilter.CreatureYouControl)
        val theirTarget = target(TargetFilter(GameObjectFilter.CreatureOrPlaneswalker.opponentControls()))
        effect = Effects.DealDamage(
            amount = DynamicAmounts.powerOf(myCreature),
            target = theirTarget,
            damageSource = myCreature
        ) then Effects.If(
            condition = Conditions.YouControl(GameObjectFilter.Permanent.withSubtype("Eldrazi")),
            then = Effects.GainLife(3),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "157"
        artist = "Justine Jones"
        flavorText = "With shifting forms and no angel to pray to, Innistrad's werewolves were especially " +
            "susceptible to Emrakul's influence."
        imageUri = "https://cards.scryfall.io/normal/front/c/f/cfa6ed13-7bba-40c0-8e0e-4ffd3cea6241.jpg?1783911259"
        ruling(
            "2024-06-07",
            "If either target is an illegal target as Horrific Assault tries to resolve, the creature you " +
                "control won't deal damage. As long as either target is still legal and you control an " +
                "Eldrazi, you'll still gain 3 life."
        )
    }
}
