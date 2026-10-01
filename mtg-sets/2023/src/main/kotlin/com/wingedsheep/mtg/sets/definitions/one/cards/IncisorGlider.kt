package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Incisor Glider
 * {1}{W}
 * Artifact Creature — Phyrexian Construct
 * 1/3
 * Flying
 * Corrupted — Whenever this creature attacks, if an opponent has three or more poison counters,
 * creatures you control get +1/+1 until end of turn.
 *
 * The corrupted clause is an intervening "if": checked when the attack trigger would fire and
 * again on resolution.
 */
val IncisorGlider = card("Incisor Glider") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Artifact Creature — Phyrexian Construct"
    oracleText = "Flying\nCorrupted — Whenever this creature attacks, if an opponent has three or more poison counters, creatures you control get +1/+1 until end of turn."
    power = 1
    toughness = 3

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        interveningIf = Conditions.Corrupted
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.ModifyStats(1, 1, EffectTarget.IterationEntity),
        )
        description = "Corrupted — Whenever this creature attacks, if an opponent has three or more poison counters, creatures you control get +1/+1 until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "15"
        artist = "Joe Slucher"
        flavorText = "\"You think it's creepy now? You should see how it eats.\"\n—Melira"
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b3cf4c4f-bda6-454c-991d-429c0dca0e85.jpg?1783918082"
    }
}
