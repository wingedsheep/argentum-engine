package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Yidaro, Wandering Monster
 * {5}{R}{R}
 * Legendary Creature — Dinosaur Turtle
 * 8/8
 *
 * Trample, haste
 * Cycling {1}{R}
 * When you cycle this card, shuffle it into your library from your graveyard. If you've cycled a
 * card named Yidaro, Wandering Monster four or more times this game, put it onto the battlefield
 * from your graveyard instead. (Do this before you draw.)
 *
 * The count is `cardsCycledThisGame(name)` — any copy named Yidaro counts, and the cycle that
 * triggered this ability is already counted. Both moves are guarded on the card still being in the
 * graveyard.
 */
val YidaroWanderingMonster = card("Yidaro, Wandering Monster") {
    manaCost = "{5}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Dinosaur Turtle"
    power = 8
    toughness = 8
    oracleText = "Trample, haste\n" +
        "Cycling {1}{R} ({1}{R}, Discard this card: Draw a card.)\n" +
        "When you cycle this card, shuffle it into your library from your graveyard. If you've " +
        "cycled a card named Yidaro, Wandering Monster four or more times this game, put it onto " +
        "the battlefield from your graveyard instead. (Do this before you draw.)"

    keywords(Keyword.TRAMPLE, Keyword.HASTE)
    keywordAbility(KeywordAbility.cycling("{1}{R}"))

    triggeredAbility {
        trigger = Triggers.self.isCycled()
        effect = Effects.If(
            Conditions.CompareAmounts(
                DynamicAmounts.cardsCycledThisGame("Yidaro, Wandering Monster"),
                ComparisonOperator.GTE,
                4,
            ),
            then = Effects.PutOntoBattlefieldFromGraveyard(EffectTarget.Self),
            otherwise = Effects.ShuffleIntoLibrary(EffectTarget.Self, fromZone = Zone.GRAVEYARD),
        )
        description = "When you cycle this card, shuffle it into your library from your graveyard. If " +
            "you've cycled a card named Yidaro, Wandering Monster four or more times this game, put it " +
            "onto the battlefield from your graveyard instead."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "141"
        artist = "Jesper Ejsing"
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b540c7c6-5b9e-4606-ac84-e583a62a3647.jpg?1783931041"
    }
}
