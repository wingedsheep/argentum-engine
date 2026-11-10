package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator

/**
 * Burn from Within (SOI #148)
 * {X}{R}
 * Sorcery
 * Burn from Within deals X damage to any target. If a creature is dealt damage this way, it loses
 * indestructible until end of turn. If that creature would die this turn, exile it instead.
 *
 * Provoke the Trolls' shape: the damage instruction records the damage it really dealt
 * (`damageDealtVariable`, after prevention), and both riders need that to be at least 1 *and* the
 * target to be a creature — a player, a noncreature planeswalker, X = 0 or prevented damage gets
 * neither rider (2016-04-08 ruling). The indestructible removal lands in the same resolution as the
 * damage, before state-based actions check, so an indestructible creature dealt lethal damage dies
 * (Soul Sear's shape) — and the exile-instead mark, also set during resolution, sends it to exile.
 */
val BurnFromWithin = card("Burn from Within") {
    manaCost = "{X}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Burn from Within deals X damage to any target. If a creature is dealt damage this way, " +
        "it loses indestructible until end of turn. If that creature would die this turn, exile it instead."

    spell {
        val t = target(Targets.Any)
        effect = Effects.Pipeline {
            val dealt = runStoringNumber {
                Effects.DealDamage(DynamicAmounts.xValue(), t, damageDealtVariable = it)
            }
            run(
                Effects.If(
                    condition = Conditions.All(
                        Conditions.CompareAmounts(dealt.amount, ComparisonOperator.GTE, 1),
                        Conditions.TargetMatchesFilter(GameObjectFilter.Creature, t)
                    ),
                    then = Effects.RemoveKeyword(Keyword.INDESTRUCTIBLE, t, Duration.EndOfTurn) then
                        Effects.MarkExileOnDeath(t, Duration.EndOfTurn)
                )
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "148"
        artist = "James Ryman"
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f8bdc165-4c6f-47e6-8bda-877c0be3613b.jpg?1783937758"
        ruling(
            "2016-04-08",
            "The damaged creature will be exiled if it would die for any reason that turn, not just if it " +
                "dies due to damage from Burn from Within."
        )
        ruling(
            "2016-04-08",
            "If Burn from Within doesn't deal damage to the target creature (perhaps because that damage was " +
                "prevented or X is 0), neither additional effect will apply. It won't lose indestructible, and " +
                "it won't be exiled instead of dying that turn."
        )
        ruling(
            "2016-04-08",
            "You can target a creature that doesn't have indestructible with Burn from Within. It will still " +
                "be exiled if it would die this turn."
        )
    }
}
