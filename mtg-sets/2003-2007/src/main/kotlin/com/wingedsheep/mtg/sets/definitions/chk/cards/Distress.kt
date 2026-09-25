package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser

/**
 * Distress
 * {B}{B}
 * Sorcery
 * Target player reveals their hand. You choose a nonland card from it. That player discards that card.
 *
 * The Duress pipeline (`usg/cards/Duress.kt`) over *any* player and a nonland filter. Targeting
 * yourself is legal; you still reveal your whole hand (2014-02-01 ruling).
 */
val Distress = card("Distress") {
    manaCost = "{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target player reveals their hand. You choose a nonland card from it. That player discards that card."

    spell {
        val player = target(Targets.Player)
        effect = Effects.Pipeline {
            run(Effects.RevealHand(player))
            val hand = gather(CardSource.FromZone(Zone.HAND, player.asPlayer))
            val toDiscard = chooseExactly(
                1,
                from = hand,
                chooser = Chooser.Controller,
                filter = GameObjectFilter.Nonland,
                prompt = "Choose a nonland card to discard",
                alwaysPrompt = true,
                showAllCards = true
            )
            discard(toDiscard, player.asPlayer)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "111"
        artist = "Michael Sutfin"
        flavorText = "\"Today I asked Master Dosan what the ogre mages did with the humans they sacrificed. He gave me a hard look and said to think no more on the matter.\"\n—Meditation journal of young budoka"
        imageUri = "https://cards.scryfall.io/normal/front/8/1/8130a902-3a03-4473-a64f-84cf3590f4c6.jpg?1783944315"
        ruling("2014-02-01", "If you target yourself with this spell, you must reveal your entire hand to the other players just as any other player would.")
    }
}
