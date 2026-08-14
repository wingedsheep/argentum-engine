package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Unstable Experiment
 * {1}{U}
 * Instant
 *
 * Target player draws a card, then up to one target creature you control connives. (Draw a card,
 * then discard a card. If you discarded a nonland card, put a +1/+1 counter on that creature.)
 *
 * Two independent targets, chosen at cast time (CR 601.2c): a player who draws, and — via "up to
 * one target" — an optional creature you control that connives. The conniving creature is the
 * source of the connive keyword action (CR 701.50), so when no creature is chosen (or the chosen
 * creature has left the battlefield by resolution) nothing connives: you neither draw nor discard.
 * That's why the connive is gated on the second target slot actually holding a creature via
 * [Conditions.TargetMatchesFilter] on `targetIndex = 1`, rather than run unconditionally — the
 * +1/+1 counter itself already lands on the chosen creature through the shared connive pipeline.
 */
val UnstableExperiment = card("Unstable Experiment") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Target player draws a card, then up to one target creature you control connives. " +
        "(Draw a card, then discard a card. If you discarded a nonland card, put a +1/+1 counter " +
        "on that creature.)"

    spell {
        val drawer = target("target player", Targets.Player)
        val creature = target(
            "up to one target creature you control",
            TargetCreature(optional = true, filter = TargetFilter.CreatureYouControl)
        )
        effect = Effects.DrawCards(1, drawer) then ConditionalEffect(
            condition = Conditions.TargetMatchesFilter(GameObjectFilter.Creature, targetIndex = 1),
            effect = Effects.Connive(creature)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "47"
        artist = "David Palumbo"
        flavorText = "\"The solution is turning green! But . . . why? What does it mean?\"\n—Norman Osborn"
        imageUri = "https://cards.scryfall.io/normal/front/b/e/be9d5985-0a39-4ee3-80de-30d17d08f404.jpg?1783905348"
    }
}
