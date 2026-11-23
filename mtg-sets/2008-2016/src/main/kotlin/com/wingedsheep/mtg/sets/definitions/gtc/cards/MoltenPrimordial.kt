package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.OwnerDistribution

/**
 * Molten Primordial — Gatecrash #101
 * {5}{R}{R} · Creature — Avatar · 6/4
 *
 * Haste
 * When this creature enters, for each opponent, gain control of up to one target creature that
 * player controls until end of turn. Untap those creatures. They gain haste until end of turn.
 *
 * "For each opponent, up to one target creature that player controls" is one target declaration
 * distributed over the opponents by projected controller ([OwnerDistribution.UP_TO_ONE] with
 * `distributeByController`, Sylvan Primordial's shape), so no opponent yields two and zero targets
 * is legal. Each surviving target is then Threatened — Harness by Force's per-target body.
 */
val MoltenPrimordial = card("Molten Primordial") {
    manaCost = "{5}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Avatar"
    power = 6
    toughness = 4
    oracleText = "Haste\nWhen this creature enters, for each opponent, gain control of up to one target " +
        "creature that player controls until end of turn. Untap those creatures. They gain haste until " +
        "end of turn."

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        targets(
            TargetFilter(GameObjectFilter.Creature.opponentControls()),
            ownerDistribution = OwnerDistribution.UP_TO_ONE,
            distributeByController = true,
        )
        effect = Effects.ForEachTarget(
            Effects.GainControl(EffectTarget.ContextTarget(0), Duration.EndOfTurn),
            Effects.Untap(EffectTarget.ContextTarget(0)),
            Effects.GrantKeyword(Keyword.HASTE, EffectTarget.ContextTarget(0)),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "101"
        artist = "Stephan Martiniere"
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a8f5c7e2-f4da-4cee-a7d0-80b29bb73acd.jpg?1783940123"
    }
}
