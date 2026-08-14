package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * The Spot's Portal
 * {2}{B}
 * Instant
 * Put target creature on the bottom of its owner's library. You lose 2 life unless you control a Villain.
 *
 * The tuck is unconditional; the life loss is gated on the negation of "you control a Villain" so it
 * only happens when you control no Villain. The condition is evaluated at resolution (CR 608.2), on
 * projected battlefield state, after the creature has been moved.
 */
val TheSpotsPortal = card("The Spot's Portal") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Put target creature on the bottom of its owner's library. " +
        "You lose 2 life unless you control a Villain."

    spell {
        val t = target("target", TargetCreature(filter = TargetFilter.Creature))
        effect = Effects.Move(t, Zone.LIBRARY, ZonePlacement.Bottom).then(
            ConditionalEffect(
                condition = Conditions.Not(
                    Conditions.YouControl(GameObjectFilter.Creature.withSubtype("Villain"))
                ),
                effect = Effects.LoseLife(2, EffectTarget.Controller),
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "68"
        artist = "Carlos Dattoli"
        flavorText = "\"You've been working too hard, Spider-Man. I think you deserve a little " +
            "vacation somewhere you won't be so...in the way.\"\n—The Spot, Johnathon Ohnn"
        imageUri = "https://cards.scryfall.io/normal/front/6/7/67a8bf52-7562-4cdd-b970-106717a0aad6.jpg?1783905340"
    }
}
