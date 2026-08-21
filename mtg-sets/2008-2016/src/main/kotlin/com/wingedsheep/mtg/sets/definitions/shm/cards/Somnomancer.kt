package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Somnomancer
 * {1}{W/U}
 * Creature — Kithkin Wizard
 * 2 / 1
 *
 * When this creature enters, you may tap target creature.
 *
 * - The target is mandatory at announcement even though the tap is optional: the trigger carries a
 *   `targetRequirement`, so a creature must be chosen when the ability goes on the stack, and the
 *   "you may" is only the resolution-time yes/no (`optional = true` lowers to a `Gate.MayDecide`).
 * - Any creature is legal, including one you control — the printed line has no controller clause.
 */
val Somnomancer = card("Somnomancer") {
    manaCost = "{1}{W/U}"
    typeLine = "Creature — Kithkin Wizard"
    power = 2
    toughness = 1
    oracleText = "When this creature enters, you may tap target creature."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        optional = true
        val creature = target("target", TargetCreature(filter = TargetFilter.Creature))
        effect = Effects.Tap(creature)
        description = "When this creature enters, you may tap target creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "149"
        artist = "Lars Grant-West"
        flavorText = "\"Are you tired? You look tired.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/a/eaf51122-fd7c-40b5-b759-65e21c28e3d6.jpg?1783942735"
    }
}
