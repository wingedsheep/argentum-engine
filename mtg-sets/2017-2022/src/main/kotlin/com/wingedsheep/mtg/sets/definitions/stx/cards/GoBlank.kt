package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Go Blank — Strixhaven: School of Mages #72
 * {2}{B} · Sorcery
 *
 * Target player discards two cards. Then exile that player's graveyard.
 *
 * The target discards two of their choice ([Patterns.Hand.discardCards]), then the Bojuka Bog
 * pipeline gathers that player's whole graveyard — the just-discarded cards included — and exiles it.
 */
val GoBlank = card("Go Blank") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target player discards two cards. Then exile that player's graveyard."

    spell {
        val player = target(Targets.Player)
        effect = Patterns.Hand.discardCards(2, player) then Effects.Pipeline {
            val graveyard = gather(CardSource.FromZone(Zone.GRAVEYARD, player.asPlayer))
            exile(graveyard, player.asPlayer)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "72"
        artist = "Wylie Beckert"
        flavorText = "The only feeling worse than not knowing the answer is the certainty that you once did."
        imageUri = "https://cards.scryfall.io/normal/front/8/4/846e8657-7435-44c6-a997-b8b156d0cd2c.jpg?1783927368"
        ruling(
            "2021-04-16",
            "If the target player has one card in hand as Go Blank resolves, they'll discard it and the cards " +
                "in their graveyard will be exiled. If their hand is empty, you'll just exile the cards in " +
                "their graveyard.",
        )
    }
}
