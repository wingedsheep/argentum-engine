package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Approach of the Second Sun — Amonkhet #4 (canonical printing)
 * {6}{W} · Sorcery
 *
 * If this spell was cast from your hand and you've cast another spell named Approach of the Second
 * Sun this game, you win the game. Otherwise, put Approach of the Second Sun into its owner's
 * library seventh from the top and you gain 7 life.
 *
 * "Another spell named …" reads the game-long by-name cast count while this spell resolves, so it
 * counts itself: two or more means another was cast, from anywhere, resolved or countered (rulings).
 * A copy fails [Conditions.WasCastFromHand] and isn't counted as cast. Seventh from the top is
 * `positionFromTop = 6`; a library of fewer than six cards puts it on the bottom (ruling). The
 * spell leaves the stack as part of its own resolution, so it isn't put into the graveyard after.
 */
val ApproachOfTheSecondSun = card("Approach of the Second Sun") {
    manaCost = "{6}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "If this spell was cast from your hand and you've cast another spell named Approach of the " +
        "Second Sun this game, you win the game. Otherwise, put Approach of the Second Sun into its owner's " +
        "library seventh from the top and you gain 7 life."

    spell {
        effect = Effects.If(
            condition = Conditions.All(
                Conditions.WasCastFromHand,
                Conditions.CompareAmounts(
                    DynamicAmounts.spellsCastThisGame(cardName = "Approach of the Second Sun"),
                    ComparisonOperator.GTE,
                    2,
                ),
            ),
            then = Effects.WinGame(),
            otherwise = Effects.Move(EffectTarget.Self, Zone.LIBRARY, fromZone = Zone.STACK, positionFromTop = 6) then
                Effects.GainLife(7),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "4"
        artist = "Noah Bradley"
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fdf59a6e-7708-45a1-884d-d12e9f7b9ed9.jpg?1783936543"
        ruling("2017-07-14", "Approach of the Second Sun has no effect until it's resolving. If the second one you cast is countered, you won't win the game.")
        ruling("2017-04-18", "The second Approach of the Second Sun that you cast must be cast from your hand, but first may have been cast from anywhere.")
        ruling("2017-04-18", "A copy of a spell isn't cast, so it won't count as the first nor as the second Approach of the Second Sun.")
        ruling("2017-04-18", "A card that changes zones is considered a new object, so casting the same Approach of the Second Sun card on a later turn is “another spell” named Approach of the Second Sun.")
        ruling("2017-04-18", "If you have fewer than six cards in your library, you'll put Approach of the Second Sun on the bottom of your library. Otherwise, you'll lift up the top six cards without looking at them and place Approach of the Second Sun just under them.")
        ruling("2017-04-18", "As your second Approach of the Second Sun resolves, it checks only whether the first one was cast, not whether the first one resolved. If your first Approach of the Second Sun was countered, you'll still win the game as your second one resolves.")
    }
}
