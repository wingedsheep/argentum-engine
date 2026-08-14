package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Rustmouth Ogre — Mirrodin #103
 * {4}{R}{R} · Creature — Ogre · 5/4
 *
 * Whenever this creature deals combat damage to a player, you may destroy target artifact that
 * player controls.
 *
 * Modelling notes:
 * - "that player controls" is the *damaged* player, not just any opponent — resolved through
 *   `controlledByTriggeringPlayer()`, which reads projected control so a stolen artifact follows
 *   its current controller. Same shape as Dreadmaw's Ire's granted rider.
 * - The target is locked in when the trigger goes on the stack; the "you may" is the resolution-time
 *   choice, hence [MayEffect] wrapping the destroy rather than an optional trigger.
 */
val RustmouthOgre = card("Rustmouth Ogre") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Ogre"
    power = 5
    toughness = 4
    oracleText = "Whenever this creature deals combat damage to a player, you may destroy target " +
        "artifact that player controls."

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        val t = target(
            "target",
            TargetPermanent(filter = TargetFilter(GameObjectFilter.Artifact.controlledByTriggeringPlayer()))
        )
        effect = MayEffect(Effects.Destroy(t))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "103"
        artist = "Brian Snõddy"
        flavorText = "It has an iron stomach. Literally."
        imageUri = "https://cards.scryfall.io/normal/front/3/f/3f8bd0c9-6b3a-489e-bf4d-b2062d530b55.jpg?1783944538"
    }
}
