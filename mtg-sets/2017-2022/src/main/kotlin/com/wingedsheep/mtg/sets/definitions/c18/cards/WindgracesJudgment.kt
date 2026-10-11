package com.wingedsheep.mtg.sets.definitions.c18.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Windgrace's Judgment — Commander 2018 #49
 * {3}{B}{G} · Instant
 *
 * "For any number of opponents, destroy target nonland permanent that player controls" is the
 * corpus's one-per-opponent distribution (Kaya, Spirits' Justice / Sheoldred's chapter I): up to one
 * nonland permanent per opponent, no two sharing a controller. Commander 2018 is the earliest
 * printing; NCC and BLC carry `Printing` rows.
 */
val WindgracesJudgment = card("Windgrace's Judgment") {
    manaCost = "{3}{B}{G}"
    typeLine = "Instant"
    oracleText = "For any number of opponents, destroy target nonland permanent that player controls."

    spell {
        targets(
            TargetFilter.NonlandPermanentOpponentControls,
            optional = true,
            dynamicMaxCount = DynamicAmounts.playerCount(Player.EachOpponent),
            differentControllers = true,
        )
        effect = Effects.ForEachTarget(Effects.Destroy(EffectTarget.ContextTarget(0)))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "49"
        artist = "Grzegorz Rutkowski"
        flavorText = "Not all who remember Urza do so with fondness."
        imageUri = "https://cards.scryfall.io/normal/front/d/8/d8b8f56c-091f-4932-9216-af58f7ea4bd7.jpg?1783934329"
    }
}
