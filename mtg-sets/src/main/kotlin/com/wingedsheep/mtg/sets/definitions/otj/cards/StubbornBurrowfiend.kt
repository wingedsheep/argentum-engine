package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Stubborn Burrowfiend
 * {1}{G}
 * Creature — Badger Beast Mount
 * 2/2
 *
 * Whenever this creature becomes saddled for the first time each turn, mill two cards, then this
 * creature gets +X/+X until end of turn, where X is the number of creature cards in your graveyard.
 * Saddle 2 (Tap any number of other creatures you control with total power 2 or more: This Mount
 * becomes saddled until end of turn. Saddle only as a sorcery.)
 *
 * "Becomes saddled for the first time each turn" is `Triggers.becomesSaddled(firstTimeEachTurn =
 * true)` (CR 702.171b) — a SELF-bound trigger fired off `BecameSaddledEvent`, gated on the event's
 * `firstThisTurn` flag so re-saddling the same turn doesn't re-fire. The "then" sequences the mill
 * before the buff (CR 608.2k): X (`creatureCardsInYourGraveyard`) is evaluated as the
 * `ModifyStatsEffect` resolves, after the two milled cards have landed in the graveyard, and is
 * locked for the until-end-of-turn duration (CR 611.2c).
 */
val StubbornBurrowfiend = card("Stubborn Burrowfiend") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Badger Beast Mount"
    power = 2
    toughness = 2
    oracleText = "Whenever this creature becomes saddled for the first time each turn, mill two " +
        "cards, then this creature gets +X/+X until end of turn, where X is the number of " +
        "creature cards in your graveyard.\n" +
        "Saddle 2 (Tap any number of other creatures you control with total power 2 or more: This " +
        "Mount becomes saddled until end of turn. Saddle only as a sorcery.)"

    keywordAbility(KeywordAbility.saddle(2))

    triggeredAbility {
        trigger = Triggers.becomesSaddled(firstTimeEachTurn = true)
        effect = Effects.Composite(
            Patterns.Library.mill(2),
            Effects.ModifyStats(
                power = DynamicAmounts.creatureCardsInYourGraveyard(),
                toughness = DynamicAmounts.creatureCardsInYourGraveyard(),
                target = EffectTarget.Self
            )
        )
        description = "Whenever this creature becomes saddled for the first time each turn, mill " +
            "two cards, then this creature gets +X/+X until end of turn, where X is the number of " +
            "creature cards in your graveyard."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "184"
        artist = "Ângelo Bortolini"
        imageUri = "https://cards.scryfall.io/normal/front/6/d/6d963eb4-d20b-4d3f-bf5d-c75f7bcb9670.jpg?1712356009"
    }
}
