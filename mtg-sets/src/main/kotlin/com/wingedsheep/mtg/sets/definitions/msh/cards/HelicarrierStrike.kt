package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.teamwork
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Helicarrier Strike — Marvel Super Heroes #15
 * {W} · Instant
 *
 * Teamwork 2 (As an additional cost to cast this spell, you may tap any number of creatures you
 * control with total power 2 or more.)
 * Helicarrier Strike deals 2 damage to target attacking or blocking creature. If this spell was
 * cast using teamwork, it deals 4 damage to that creature instead.
 *
 * The plain spell-rider shape of teamwork (CR 702.194b): the declaration is read off the spell
 * while it is still on the stack, through [Conditions.TeamworkWasPaid].
 *
 * The "4 instead" is one [DynamicAmount.Conditional] rather than 2 damage plus a second 2 — the
 * printed card deals a *single* damage event, and splitting it would let a "prevent the next 2
 * damage" shield or a "whenever a source deals damage to it" trigger see two events instead of one.
 */
val HelicarrierStrike = card("Helicarrier Strike") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Teamwork 2 (As an additional cost to cast this spell, you may tap any number of " +
        "creatures you control with total power 2 or more.)\n" +
        "Helicarrier Strike deals 2 damage to target attacking or blocking creature. If this " +
        "spell was cast using teamwork, it deals 4 damage to that creature instead."

    teamwork(2)

    spell {
        val creature = target(
            "target attacking or blocking creature",
            TargetObject(filter = TargetFilter.AttackingOrBlockingCreature),
        )
        effect = Effects.DealDamage(
            DynamicAmount.Conditional(
                condition = Conditions.TeamworkWasPaid,
                ifTrue = DynamicAmount.Fixed(4),
                ifFalse = DynamicAmount.Fixed(2),
            ),
            creature,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "15"
        artist = "Maxim Ruabtsev"
        flavorText = "\"Light 'em up, people!\""
        imageUri = "https://cards.scryfall.io/normal/front/6/e/6e518842-ce44-4af2-8f38-89869828294a.jpg?1783902975"
    }
}
