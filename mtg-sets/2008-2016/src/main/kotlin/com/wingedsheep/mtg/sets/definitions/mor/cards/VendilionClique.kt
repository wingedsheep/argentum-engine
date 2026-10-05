package com.wingedsheep.mtg.sets.definitions.mor.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser

/**
 * Vendilion Clique
 * {1}{U}{U}
 * Legendary Creature — Faerie Wizard
 * 3/1
 * Flash
 * Flying
 * When Vendilion Clique enters, look at target player's hand. You may choose a nonland card from it.
 * If you do, that player reveals the chosen card, puts it on the bottom of their library, then draws a card.
 */
val VendilionClique = card("Vendilion Clique") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Faerie Wizard"
    oracleText = "Flash\nFlying\nWhen Vendilion Clique enters, look at target player's hand. You may choose a nonland card from it. If you do, that player reveals the chosen card, puts it on the bottom of their library, then draws a card."
    power = 3
    toughness = 1

    keywords(Keyword.FLASH, Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val player = target(Targets.Player)
        effect = Effects.Pipeline {
            run(Effects.LookAtHand(player))
            val hand = gather(CardSource.FromZone(Zone.HAND, player.asPlayer))
            val chosen = chooseUpTo(
                1,
                from = hand,
                chooser = Chooser.Controller,
                filter = GameObjectFilter.Nonland,
                prompt = "You may choose a nonland card to put on the bottom of its owner's library",
                selectedLabel = "Bottom of library",
                showAllCards = true,
                alwaysPrompt = true
            )
            ifNotEmpty(chosen) {
                reveal(chosen, fromZone = Zone.HAND)
                toLibraryBottom(chosen, player.asPlayer)
                run(Effects.DrawCards(1, player))
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "55"
        artist = "Michael Sutfin"
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f53d8540-fb6d-4d4c-b467-ebfbfa53c880.jpg?1783942794"
    }
}
