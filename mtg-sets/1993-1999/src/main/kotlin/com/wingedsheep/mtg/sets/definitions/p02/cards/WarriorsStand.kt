package com.wingedsheep.mtg.sets.definitions.p02.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.YouWereAttackedThisStep
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Warrior's Stand
 * {1}{W}
 * Instant
 * Cast this spell only during the declare attackers step and only if you've been attacked this step.
 * Creatures you control get +2/+2 until end of turn.
 */
val WarriorsStand = card("Warrior's Stand") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText =
        "Cast this spell only during the declare attackers step and only if you've been attacked this step.\n" +
        "Creatures you control get +2/+2 until end of turn."

    spell {
        castOnlyDuring(Step.DECLARE_ATTACKERS)
        castOnlyIf(YouWereAttackedThisStep)
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.ModifyStats(2, 2, EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "29"
        artist = "Keith Parkinson"
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2a24f00c-7e2e-4609-b956-ca3d5fb365b2.jpg"
    }
}
