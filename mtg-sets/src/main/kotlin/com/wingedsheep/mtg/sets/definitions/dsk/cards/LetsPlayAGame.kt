package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Let's Play a Game
 * {3}{B}
 * Sorcery
 *
 * Delirium — Choose one. If there are four or more card types among cards in your graveyard,
 * choose one or more instead.
 * • Creatures your opponents control get -1/-1 until end of turn.
 * • Each opponent discards two cards.
 * • Each opponent loses 3 life and you gain 3 life.
 *
 * Delirium gates the modal *choose count*, not a separate effect. The mandatory floor stays
 * `minChooseCount = 1` ("choose one"); the cap is a cast-time [DynamicAmount.Conditional] that
 * yields the full mode count (3 — "one or more") when delirium is active as the spell is cast,
 * otherwise 1. Evaluated against the caster's graveyard at cast time by
 * [com.wingedsheep.engine.handlers.actions.spell.CastSpellHandler] — same machinery as Flame of
 * Anor's "you may choose two instead."
 */
val LetsPlayAGame = card("Let's Play a Game") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Delirium — Choose one. If there are four or more card types among cards in your " +
        "graveyard, choose one or more instead.\n" +
        "• Creatures your opponents control get -1/-1 until end of turn.\n" +
        "• Each opponent discards two cards.\n" +
        "• Each opponent loses 3 life and you gain 3 life."

    spell {
        modal(
            chooseCount = 1,
            minChooseCount = 1,
            dynamicChooseCount = DynamicAmount.Conditional(
                condition = Conditions.Delirium(),
                ifTrue = DynamicAmount.Fixed(3),
                ifFalse = DynamicAmount.Fixed(1)
            )
        ) {
            mode(
                "Creatures your opponents control get -1/-1 until end of turn.",
                Patterns.Group.modifyStatsForAll(-1, -1, Filters.Group.creaturesOpponentsControl, Duration.EndOfTurn)
            )
            mode(
                "Each opponent discards two cards.",
                Effects.EachOpponentDiscards(2)
            )
            mode(
                "Each opponent loses 3 life and you gain 3 life.",
                Effects.Composite(
                    Effects.LoseLife(3, EffectTarget.PlayerRef(Player.EachOpponent)),
                    Effects.GainLife(3)
                )
            )
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "105"
        artist = "Riccardo Federici"
        imageUri = "https://cards.scryfall.io/normal/front/6/4/645911b4-7728-4380-9097-e4139b986423.jpg?1726286240"
    }
}
