package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Unleash Shell
 * {3}{R}{R}
 * Instant
 * Unleash Shell deals 5 damage to target creature or planeswalker and 2 damage to that
 * permanent's controller.
 *
 * Judgment Bolt's shape: a fixed 5 to the target, then 2 to its controller via
 * [EffectTarget.TargetController] (which falls back to last-known control if the permanent
 * has already left the battlefield).
 */
val UnleashShell = card("Unleash Shell") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Unleash Shell deals 5 damage to target creature or planeswalker and 2 damage to " +
        "that permanent's controller."

    spell {
        val t = target(Targets.CreatureOrPlaneswalker)
        effect = Effects.DealDamage(5, t) then
            Effects.DealDamage(2, EffectTarget.TargetController)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "155"
        artist = "Campbell White"
        flavorText = "The new cannon's destructive power more than made up for its unpredictable blast radius."
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1be13abf-b717-4122-ab2b-7e597a251d52.jpg?1783920060"
    }
}
