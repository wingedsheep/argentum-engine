package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanOnlyBlockCreaturesWith
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.EventPattern.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Soulcipher Board // Cipherbound Spirit (Innistrad: Crimson Vow)
 * {1}{U}
 * Artifact // Creature — Spirit
 *
 * Front — Soulcipher Board
 *   This artifact enters with three omen counters on it.
 *   {1}{U}, {T}: Look at the top two cards of your library. Put one of them into your graveyard.
 *   Whenever a creature card is put into your graveyard from anywhere, remove an omen counter from
 *   this artifact. Then if it has no omen counters on it, transform it.
 *
 * Back — Cipherbound Spirit (3/2)
 *   Flying
 *   This creature can block only creatures with flying.
 *   {3}{U}: Draw two cards, then discard a card.
 *
 * Built from existing primitives. The countdown is the new passive [Counters.OMEN] counter placed
 * by a self-only [EntersWithCounters]. The tap ability is
 * [Patterns.Library.lookAtTopAndKeep] with the *kept* card going to the graveyard and the
 * remainder back on top of the library — "look at the top two, put one of them into your graveyard"
 * leaves the other where it was.
 *
 * The countdown trigger is a per-card `ZoneChangeEvent(to = GRAVEYARD)` over creature cards you
 * own, with `TriggerBinding.ANY` — deliberately **not** the batching
 * `Triggers.CardsPutIntoYourGraveyard`: two creature cards hitting the graveyard at once remove two
 * counters, not one. "From anywhere" is expressed by leaving `from` unset. The follow-up
 * "Then if it has no omen counters on it" is an intervening check at resolution, so it is a
 * [ConditionalEffect] over the *current* counter count rather than a second trigger.
 */

private val SoulcipherBoardFront = card("Soulcipher Board") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Artifact"
    oracleText = "This artifact enters with three omen counters on it.\n" +
        "{1}{U}, {T}: Look at the top two cards of your library. Put one of them into your graveyard.\n" +
        "Whenever a creature card is put into your graveyard from anywhere, remove an omen counter " +
        "from this artifact. Then if it has no omen counters on it, transform it."

    replacementEffect(
        EntersWithCounters(
            counterType = CounterTypeFilter.Named(Counters.OMEN),
            count = 3,
            selfOnly = true,
        )
    )

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{U}"), Costs.Tap)
        effect = Patterns.Library.lookAtTopAndKeep(
            count = 2,
            keepCount = 1,
            keepDestination = CardDestination.ToZone(Zone.GRAVEYARD),
            restDestination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Top),
        )
        description = "Look at the top two cards of your library. Put one of them into your graveyard."
    }

    triggeredAbility {
        trigger = TriggerSpec(
            event = ZoneChangeEvent(
                filter = GameObjectFilter.Creature.ownedByYou(),
                to = Zone.GRAVEYARD,
            ),
            binding = TriggerBinding.ANY,
        )
        effect = Effects.Composite(
            Effects.RemoveCounters(Counters.OMEN, 1, EffectTarget.Self),
            ConditionalEffect(
                condition = Compare(
                    DynamicAmounts.countersOnSelf(CounterTypeFilter.Named(Counters.OMEN)),
                    ComparisonOperator.EQ,
                    DynamicAmount.Fixed(0),
                ),
                effect = TransformEffect(EffectTarget.Self),
            ),
        )
        description = "Whenever a creature card is put into your graveyard from anywhere, remove " +
            "an omen counter from this artifact. Then if it has no omen counters on it, transform it."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "79"
        artist = "Caio Monteiro"
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3c0fae23-1278-499f-9df7-4a29691726b1.jpg?1783924889"
    }
}

private val CipherboundSpirit = card("Cipherbound Spirit") {
    manaCost = ""
    colorIdentity = "U"
    colorIndicator = "U" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Spirit"
    power = 3
    toughness = 2
    oracleText = "Flying\n" +
        "This creature can block only creatures with flying.\n" +
        "{3}{U}: Draw two cards, then discard a card."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = CanOnlyBlockCreaturesWith(
            blockerFilter = GameObjectFilter.Creature.withKeyword(Keyword.FLYING)
        )
    }

    activatedAbility {
        cost = Costs.Mana("{3}{U}")
        effect = Effects.Composite(
            Effects.DrawCards(2),
            Patterns.Hand.discardCards(1, EffectTarget.Controller),
        )
        description = "Draw two cards, then discard a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "79"
        artist = "Caio Monteiro"
        imageUri = "https://cards.scryfall.io/normal/back/3/c/3c0fae23-1278-499f-9df7-4a29691726b1.jpg?1783924889"
    }
}

val SoulcipherBoard: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = SoulcipherBoardFront,
    backFace = CipherboundSpirit,
)
