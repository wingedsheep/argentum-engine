package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.OwnerDistribution

/**
 * Luminate Primordial — Gatecrash #20
 * {5}{W}{W} · Creature — Avatar · 4/7
 *
 * Vigilance
 * When this creature enters, for each opponent, exile up to one target creature that player
 * controls and that player gains life equal to its power.
 *
 * "For each opponent, up to one target creature that player controls" is one target declaration
 * distributed over the opponents by projected controller ([OwnerDistribution.UP_TO_ONE] with
 * `distributeByController`, Molten Primordial's shape). Each surviving target is then handled on
 * its own: its controller — "that player", since the target had to be controlled by them — gains
 * life equal to its power, read before it is exiled (Swords to Plowshares' order).
 */
val LuminatePrimordial = card("Luminate Primordial") {
    manaCost = "{5}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Avatar"
    power = 4
    toughness = 7
    oracleText = "Vigilance\nWhen this creature enters, for each opponent, exile up to one target creature " +
        "that player controls and that player gains life equal to its power."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        targets(
            TargetFilter(GameObjectFilter.Creature.opponentControls()),
            ownerDistribution = OwnerDistribution.UP_TO_ONE,
            distributeByController = true,
        )
        effect = Effects.ForEachTarget(
            Effects.GainLife(DynamicAmounts.powerOf(EffectTarget.ContextTarget(0)), EffectTarget.TargetController),
            Effects.Exile(EffectTarget.ContextTarget(0)),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "20"
        artist = "Stephan Martiniere"
        imageUri = "https://cards.scryfall.io/normal/front/b/0/b0747b12-c75a-4fdf-a881-f2383a23ccdd.jpg?1783940142"
    }
}
