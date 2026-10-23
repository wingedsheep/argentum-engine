package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Illuminate History — Strixhaven: School of Mages #108
 * {2}{R}{R} · Sorcery — Lesson
 *
 * Discard any number of cards, then draw that many cards. Then if there are seven or more cards
 * in your graveyard, create a 3/2 red and white Spirit creature token.
 *
 * The Into the Night rail: [Patterns.Hand.discardAnyNumber] stores the discarded set and the draw
 * reads its size (zero discards is legal and draws nothing). The graveyard count is checked after
 * the draw, while the spell is still on the stack, so it never counts itself. Token art resolves
 * through STX's synced token printings, as on Spirit Summoning.
 */
val IlluminateHistory = card("Illuminate History") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery — Lesson"
    oracleText = "Discard any number of cards, then draw that many cards. Then if there are seven or " +
        "more cards in your graveyard, create a 3/2 red and white Spirit creature token."

    spell {
        effect = Effects.Pipeline {
            val discarded = runStoringCollection { Patterns.Hand.discardAnyNumber(storeAs = it) }
            run(Effects.DrawCards(discarded.count))
            run(
                Effects.If(
                    condition = Conditions.CardsInGraveyardAtLeast(7),
                    then = Effects.CreateToken(
                        power = 3,
                        toughness = 2,
                        colors = setOf(Color.RED, Color.WHITE),
                        creatureTypes = setOf("Spirit"),
                    ),
                )
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "108"
        artist = "Jokubas Uogintas"
        flavorText = "\"Look at the detail in this stonework! Professor, our assumptions about early " +
            "Kathorran tool use may not be correct.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/8/98739789-80b5-4224-a2e4-09e00654aa9d.jpg?1783927352"
        ruling(
            "2021-04-16",
            "You may choose not to discard any cards as Illuminate History resolves. In that case, you will " +
                "not draw any cards, but you might still create a Spirit.",
        )
        ruling(
            "2021-04-16",
            "Illuminate History will stay on the stack until it is finished resolving, so it will not count " +
                "itself as one of the cards in your graveyard.",
        )
    }
}
