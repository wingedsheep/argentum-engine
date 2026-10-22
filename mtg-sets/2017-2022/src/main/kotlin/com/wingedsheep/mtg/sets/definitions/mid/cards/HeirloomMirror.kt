package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Heirloom Mirror // Inherited Fiend (Innistrad: Midnight Hunt)
 * {1}{B} Artifact // Creature — Demon 4/4 (transform)
 *
 * Front — Heirloom Mirror
 *   {1}, {T}, Pay 1 life, Discard a card: Draw a card, mill a card, then put a ritual counter on
 *   this artifact. Then if it has three or more ritual counters on it, remove them and transform
 *   it. Activate only as a sorcery.
 *
 * Back — Inherited Fiend
 *   Flying
 *   {2}{B}: Exile target creature card from a graveyard. Put a +1/+1 counter on this creature.
 *
 * The countdown is Hostile Hostel's shape over the passive [CounterType.RITUAL] counter: "remove
 * them" removes every ritual counter, not just three. Nothing untaps the Mirror, so the Fiend
 * stays tapped after it transforms (the card's ruling). The Fiend's ability hangs off its single
 * target, so an illegal target on resolution counters the whole ability and no counter is placed.
 */

private const val MIRROR_TEXT = "{1}, {T}, Pay 1 life, Discard a card: Draw a card, mill a card, then put a " +
    "ritual counter on this artifact. Then if it has three or more ritual counters on it, remove them and " +
    "transform it. Activate only as a sorcery."

private val HeirloomMirrorFront = card("Heirloom Mirror") {
    manaCost = "{1}{B}"
    typeLine = "Artifact"
    oracleText = MIRROR_TEXT

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}"),
            Costs.Tap,
            Costs.PayLife(1),
            Costs.DiscardCard,
        )
        effect = Effects.DrawCards(1) then
            Patterns.Library.mill(1) then
            Effects.AddCounters(CounterType.RITUAL, 1, EffectTarget.Self) then
            Effects.If(
                condition = Conditions.SourceCounterCountAtLeast(CounterType.RITUAL, 3),
                then = Effects.RemoveAllCountersOfType(CounterType.RITUAL, EffectTarget.Self) then
                    Effects.Transform(EffectTarget.Self),
            )
        timing = TimingRule.SorcerySpeed
        description = MIRROR_TEXT.removeSuffix(" Activate only as a sorcery.")
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "105"
        artist = "Josh Hass"
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e6dd05f0-a3c0-4bd6-a1d1-a74540623093.jpg?1783925622"
        ruling("2021-09-24", "Heirloom Mirror will still be tapped after it transforms into Inherited Fiend unless some other effect has untapped it.")
    }
}

private const val FIEND_TEXT = "{2}{B}: Exile target creature card from a graveyard. Put a +1/+1 counter on this creature."

private val InheritedFiend = card("Inherited Fiend") {
    manaCost = ""
    colorIdentity = "B"
    colorIndicator = "B" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Demon"
    power = 4
    toughness = 4
    oracleText = "Flying\n$FIEND_TEXT"

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Mana("{2}{B}")
        val creatureCard = target(TargetFilter.CreatureInGraveyard)
        effect = Effects.Exile(creatureCard) then
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = FIEND_TEXT
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "105"
        artist = "Josh Hass"
        imageUri = "https://cards.scryfall.io/normal/back/e/6/e6dd05f0-a3c0-4bd6-a1d1-a74540623093.jpg?1783925622"
    }
}

val HeirloomMirror: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = HeirloomMirrorFront,
    backFace = InheritedFiend,
)
