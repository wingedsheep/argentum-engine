package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.effects.FeasibilityCheck
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Argentum Masticore
 * {5}
 * Artifact Creature — Phyrexian Masticore
 * 5/5
 * First strike, protection from multicolored
 * At the beginning of your upkeep, sacrifice this creature unless you discard a card. When you
 * discard a card this way, destroy target nonland permanent an opponent controls with mana value
 * less than or equal to the mana value of the discarded card.
 *
 * The upkeep trigger is "you may discard a card; if you don't, sacrifice" — a `May` whose
 * `otherwise` sacrifices, with a hand-size feasibility so an empty hand sacrifices without asking.
 * The discard is the action of a mandatory reflexive trigger (CR 603.12): its target is chosen when
 * it goes on the stack, capped by the discarded card's mana value read off the carried pipeline.
 * Per the rulings, discarding keeps the Masticore even when there's no legal target.
 */
val ArgentumMasticore = card("Argentum Masticore") {
    manaCost = "{5}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Phyrexian Masticore"
    oracleText = "First strike, protection from multicolored\n" +
        "At the beginning of your upkeep, sacrifice this creature unless you discard a card. " +
        "When you discard a card this way, destroy target nonland permanent an opponent controls " +
        "with mana value less than or equal to the mana value of the discarded card."
    power = 5
    toughness = 5

    keywords(Keyword.FIRST_STRIKE)
    keywordAbility(KeywordAbility.Protection(ProtectionScope.Multicolored))

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.May(
            effect = Effects.ReflexiveTrigger(action = Effects.Discard(1), optional = false) {
                val permanent = target(
                    TargetFilter.NonlandPermanentOpponentControls.manaValueAtMostDynamic(
                        DynamicAmounts.manaValueOf("discarded")
                    )
                )
                effect = Effects.Destroy(permanent)
            },
            otherwise = SacrificeSelfEffect,
            prompt = "Discard a card? If you don't, sacrifice Argentum Masticore.",
            feasibility = FeasibilityCheck.HasCardsInZone(Zone.HAND)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "222"
        artist = "Zack Stella"
        imageUri = "https://cards.scryfall.io/normal/front/9/7/9746e3ab-c0a6-46c1-a418-275b419962e4.jpg?1783917994"
        ruling(
            "2023-02-04",
            "You don't choose a target for Argentum Masticore's upkeep triggered ability at the time it triggers. Rather, a second \"reflexive\" ability triggers when you discard a card this way. You choose a target for that ability as it goes on the stack. Each player may respond to this triggered ability as normal."
        )
        ruling(
            "2023-02-04",
            "Notably, this means that you may discard a card to keep Argentum Masticore on the battlefield even if there isn't a legal target for that second triggered ability. In that case, no players may respond to the second triggered ability and it does not resolve."
        )
    }
}
