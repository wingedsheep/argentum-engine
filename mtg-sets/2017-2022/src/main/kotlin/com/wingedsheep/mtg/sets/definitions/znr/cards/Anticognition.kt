package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Anticognition — Zendikar Rising #45
 * {1}{U} · Instant
 *
 * Counter target creature or planeswalker spell unless its controller pays {2}. If an opponent
 * has eight or more cards in their graveyard, instead counter that spell, then scry 2.
 *
 * Stubborn Denial's `If(hard counter, otherwise soft counter)` shape, gated on Expedition
 * Lookout's per-opponent graveyard test: any single opponent at eight or more (not the targeted
 * spell's controller, and not a sum across opponents), checked at resolution.
 */
val Anticognition = card("Anticognition") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target creature or planeswalker spell unless its controller pays {2}. If an " +
        "opponent has eight or more cards in their graveyard, instead counter that spell, then scry 2."

    spell {
        target(TargetFilter(GameObjectFilter.Creature or GameObjectFilter.Planeswalker, zone = Zone.STACK))
        effect = Effects.If(
            condition = Conditions.CompareAmounts(
                DynamicAmounts.greatestAmongPlayers(
                    DynamicAmounts.count(Player.You, Zone.GRAVEYARD),
                    Player.EachOpponent
                ),
                ComparisonOperator.GTE,
                8
            ),
            then = Effects.CounterSpell() then Effects.Scry(2),
            otherwise = Effects.CounterUnlessPays("{2}")
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "45"
        artist = "Igor Kieryluk"
        flavorText = "\"I think not.\"\n—Flekon the Mindcensor"
        imageUri = "https://cards.scryfall.io/normal/front/d/b/db99b872-77c7-4471-9c44-a36d4ff5d33f.jpg?1783929404"
        ruling(
            "2020-09-25",
            "Anticognition will counter the spell and let you scry 2 if any opponent has eight or more " +
                "cards in their graveyard, not just that spell's controller."
        )
        ruling(
            "2020-09-25",
            "Anticognition can target a spell that can't be countered. You'll still scry 2 if an " +
                "opponent has eight or more cards in their graveyard."
        )
    }
}
