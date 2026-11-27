package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Thought Scour — Dark Ascension #52 (canonical printing)
 * {U} · Instant
 *
 * Target player mills two cards.
 * Draw a card.
 *
 * Dampen Thought's targeted mill followed by Mental Note's draw. The draw is part of the same
 * spell, so an illegal target fizzles the whole thing and you don't draw.
 */
val ThoughtScour = card("Thought Scour") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Target player mills two cards.\nDraw a card."

    spell {
        val player = target(Targets.Player)
        effect = Patterns.Library.mill(2, player) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "52"
        artist = "David Rapoza"
        flavorText = "\"As you inject the *viscus vitae* into the brain stem, don't let the spastic " +
            "moaning bother you. It will soon become music to your ears.\"\n—Stitcher Geralf"
        imageUri = "https://cards.scryfall.io/normal/front/8/8/88bf1ebb-9d85-4b9b-a614-c7f965c0893d.jpg?1783940835"
        ruling(
            "2011-01-22",
            "If the target player is an illegal target when Thought Scour tries to resolve, it won't " +
                "resolve and none of its effects will happen. You won't draw a card.",
        )
        ruling(
            "2011-01-22",
            "Follow the instructions in the order listed on the card: if you target yourself, you'll " +
                "put the top two cards of your library into your graveyard and then draw a card.",
        )
    }
}
