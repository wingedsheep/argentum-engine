package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.teamwork
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Repulsor Blast — Marvel Super Heroes #150
 * {3}{R} · Sorcery
 *
 * Teamwork 2 (As an additional cost to cast this spell, you may tap any number of creatures you
 * control with total power 2 or more.)
 * Repulsor Blast deals 5 damage to target creature. If this spell was cast using teamwork, it also
 * deals 2 damage to that creature's controller.
 *
 * The plain spell-rider shape of teamwork (CR 702.194b), gated on [Conditions.TeamworkWasPaid].
 * Unlike Helicarrier Strike's "instead", the rider here is genuinely a *second* damage event to a
 * different recipient, so it is a [ConditionalEffect] appended to the base damage. The recipient is
 * [EffectTarget.TargetController] — "that creature's controller" is not itself a target, so the
 * spell keeps exactly one target requirement whichever branch is taken.
 */
val RepulsorBlast = card("Repulsor Blast") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Teamwork 2 (As an additional cost to cast this spell, you may tap any number of " +
        "creatures you control with total power 2 or more.)\n" +
        "Repulsor Blast deals 5 damage to target creature. If this spell was cast using teamwork, " +
        "it also deals 2 damage to that creature's controller."

    teamwork(2)

    spell {
        val creature = target("target creature", TargetCreature(filter = TargetFilter.Creature))
        effect = Effects.DealDamage(5, creature).then(
            ConditionalEffect(
                condition = Conditions.TeamworkWasPaid,
                effect = Effects.DealDamage(2, EffectTarget.TargetController),
            ),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "150"
        artist = "Peter Scanlan"
        flavorText = "\"Mind if I cut in?\""
        imageUri = "https://cards.scryfall.io/normal/front/8/3/837265b0-fc15-4d96-9d6b-fd1c78534262.jpg?1783902925"
    }
}
