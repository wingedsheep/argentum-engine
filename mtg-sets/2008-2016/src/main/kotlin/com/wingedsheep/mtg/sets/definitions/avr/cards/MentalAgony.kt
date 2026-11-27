package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Mental Agony — Avacyn Restored #114 (canonical printing)
 * {3}{B}
 * Sorcery
 * Target player discards two cards and loses 2 life.
 *
 * The life loss doesn't depend on the discard (ruling): a player with fewer than two cards in hand
 * still loses 2 life.
 */
val MentalAgony = card("Mental Agony") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target player discards two cards and loses 2 life."

    spell {
        val t = target(Targets.Player)
        effect = Effects.Discard(2, t) then Effects.LoseLife(2, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "114"
        artist = "Greg Staples"
        flavorText = "\"His mind is feeble. Feed him to the bats.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/f/4f8a1d51-aa7f-41fd-b97d-56bc48221615.jpg?1783940695"
        ruling(
            "2012-05-01",
            "If the targeted player has fewer than two cards in their hand, that player will still lose 2 " +
                "life (and discard one card, if applicable).",
        )
    }
}
