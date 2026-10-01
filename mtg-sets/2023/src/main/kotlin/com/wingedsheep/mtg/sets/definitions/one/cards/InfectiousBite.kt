package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Infectious Bite — Phyrexia: All Will Be One #172
 * {1}{G}
 * Instant
 * Target creature you control deals damage equal to its power to target creature you don't
 * control. Each opponent gets a poison counter.
 *
 * A bite: the damage source is the first target, its power read on resolution. The poison half
 * is per-opponent — `AddCounters` resolves one player — so it runs under
 * `ForEachPlayer(EachOpponent)`, which rebinds `Player.You` to the visited opponent.
 */
val InfectiousBite = card("Infectious Bite") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Target creature you control deals damage equal to its power to target creature you don't control. Each opponent gets a poison counter."

    spell {
        val biter = target(TargetFilter.Creature.youControl())
        val victim = target(TargetFilter.Creature.opponentControls())
        effect = Effects.DealDamage(DynamicAmounts.powerOf(biter), victim, damageSource = biter) then
            Effects.ForEachPlayer(
                Player.EachOpponent,
                Effects.AddCounters(CounterType.POISON, 1, EffectTarget.PlayerRef(Player.You)),
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "172"
        artist = "Campbell White"
        flavorText = "\"Let me tell you why you must never close your eyes when on watch. Not even for a second.\"\n—Jor Kadeen"
        imageUri = "https://cards.scryfall.io/normal/front/8/3/83dfb2a5-cd5c-46c6-9bb8-7c5d00f3e003.jpg?1783918015"
    }
}
