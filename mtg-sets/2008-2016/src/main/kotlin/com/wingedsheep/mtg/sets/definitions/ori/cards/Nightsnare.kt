package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser

/**
 * Nightsnare
 * {3}{B}
 * Sorcery
 * Target opponent reveals their hand. You may choose a nonland card from it. If you do, that player
 * discards that card. If you don't, that player discards two cards.
 *
 * Reckoner Shakedown's either/or shape: the caster picks up to one nonland card from the revealed
 * hand, and the emptiness of that pick drives the branch. Declining — or finding no nonland card to
 * take — falls through to a plain "discards two cards", where the opponent (not the caster) chooses
 * which two, per the 2015-06-22 ruling.
 */
val Nightsnare = card("Nightsnare") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target opponent reveals their hand. You may choose a nonland card from it. " +
        "If you do, that player discards that card. If you don't, that player discards two cards."

    spell {
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            run(Effects.RevealHand(opponent))
            val hand = gather(CardSource.FromZone(Zone.HAND, opponent.asPlayer))
            val chosen = chooseUpTo(
                1,
                from = hand,
                chooser = Chooser.Controller,
                filter = GameObjectFilter.Nonland,
                prompt = "You may choose a nonland card for that player to discard",
                alwaysPrompt = true,
                showAllCards = true
            )
            ifNotEmpty(chosen) {
                discard(chosen, opponent.asPlayer)
            } orElse {
                run(Patterns.Hand.discardCards(2, opponent))
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "111"
        artist = "Min Yum"
        flavorText = "Ravnica would be safer if mere coin was all the Dimir could steal."
        imageUri = "https://cards.scryfall.io/normal/front/3/9/391a1023-69e7-425c-ac54-987ba366f8f4.jpg?1783938338"
        ruling(
            "2015-06-22",
            "If you don't choose a nonland card for the player to discard, that player chooses which " +
                "two cards they will discard."
        )
    }
}
