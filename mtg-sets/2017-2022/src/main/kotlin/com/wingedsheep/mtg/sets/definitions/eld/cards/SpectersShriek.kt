package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Specter's Shriek
 * {B}
 * Sorcery
 * Target opponent reveals their hand. You may choose a nonland card from it. If you do, that player
 * exiles that card. If a nonblack card is exiled this way, exile a card from your hand.
 *
 * The Memory Theft reveal-and-pick pipeline with an optional pick, exiling instead of discarding.
 * The exile is tracked so "exiled this way" reads the card that actually moved; the self-exile then
 * runs only when that card is nonblack, and you pick which card of your own hand goes.
 */
val SpectersShriek = card("Specter's Shriek") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target opponent reveals their hand. You may choose a nonland card from it. If you do, " +
        "that player exiles that card. If a nonblack card is exiled this way, exile a card from your hand."

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
                prompt = "You may choose a nonland card for that player to exile",
                alwaysPrompt = true,
                showAllCards = true
            )
            val exiled = moveTracked(chosen, CardDestination.ToZone(Zone.EXILE, opponent.asPlayer))

            run(Effects.If(
                condition = whenMatches(exiled, GameObjectFilter.Any.notColor(Color.BLACK)),
                then = Effects.Pipeline {
                    val yourHand = gather(CardSource.FromZone(Zone.HAND, Player.You))
                    val yours = chooseExactly(
                        1,
                        from = yourHand,
                        chooser = Chooser.Controller,
                        prompt = "Exile a card from your hand"
                    )
                    move(yours, CardDestination.ToZone(Zone.EXILE, Player.You))
                }
            ))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "106"
        artist = "Svetlin Velinov"
        flavorText = "\"Imagine having your skeleton extracted through your ear, one bone at a time.\"\n—Corliss the Wanderer"
        imageUri = "https://cards.scryfall.io/normal/front/e/d/edae2c29-1418-477c-9efe-e53fa6c7fe93.jpg?1783932632"
        ruling(
            "2019-10-04",
            "If you have no cards in hand, you won't have to exile anything if you have the player exile a " +
                "nonblack card from their hand."
        )
    }
}
