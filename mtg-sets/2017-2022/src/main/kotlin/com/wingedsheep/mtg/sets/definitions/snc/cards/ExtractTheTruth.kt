package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser

/**
 * Extract the Truth
 * {1}{B}
 * Sorcery
 * Choose one —
 * • Target opponent reveals their hand. You may choose a creature, enchantment, or planeswalker card
 *   from it. That player discards that card.
 * • Target opponent sacrifices an enchantment of their choice.
 */
val ExtractTheTruth = card("Extract the Truth") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Choose one —\n" +
        "• Target opponent reveals their hand. You may choose a creature, enchantment, or planeswalker card from it. That player discards that card.\n" +
        "• Target opponent sacrifices an enchantment of their choice."

    spell {
        modal(chooseCount = 1) {
            mode("Target opponent reveals their hand; you may choose a creature, enchantment, or planeswalker card for them to discard") {
                val opponent = target(Targets.Opponent)
                effect = Effects.Pipeline {
                    run(Effects.RevealHand(opponent))
                    val hand = gather(CardSource.FromZone(Zone.HAND, opponent.asPlayer))
                    val chosen = chooseUpTo(
                        1,
                        from = hand,
                        chooser = Chooser.Controller,
                        filter = GameObjectFilter.Creature or GameObjectFilter.Enchantment or GameObjectFilter.Planeswalker,
                        prompt = "You may choose a creature, enchantment, or planeswalker card to discard",
                        alwaysPrompt = true,
                        showAllCards = true
                    )
                    discard(chosen, opponent.asPlayer)
                }
            }
            mode("Target opponent sacrifices an enchantment of their choice") {
                val opponent = target(Targets.Opponent)
                effect = Effects.Sacrifice(GameObjectFilter.Enchantment, target = opponent)
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "78"
        artist = "Josu Hernaiz"
        imageUri = "https://cards.scryfall.io/normal/front/2/b/2bdb9df1-4475-45e7-bc9c-40937c8c1722.jpg?1783923131"
    }
}
