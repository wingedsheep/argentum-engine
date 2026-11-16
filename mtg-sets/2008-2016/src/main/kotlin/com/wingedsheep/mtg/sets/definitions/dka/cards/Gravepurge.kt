package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Gravepurge
 * {2}{B}
 * Instant
 * Put any number of target creature cards from your graveyard on top of your library.
 * Draw a card.
 *
 * Forever Young's script at instant speed: one `unlimited = true` target slot (zero targets is
 * legal, leaving a plain cantrip), the chosen cards moved to the top of your library in an order
 * you choose, then the draw — so the card you put on top is the card you draw.
 */
val Gravepurge = card("Gravepurge") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Put any number of target creature cards from your graveyard on top of your library.\n" +
        "Draw a card."

    spell {
        targets(TargetFilter.CreatureInYourGraveyard, unlimited = true)
        effect = Effects.Pipeline {
            val chosen = gather(CardSource.ChosenTargets)
            toLibraryTop(chosen)
            run(Effects.DrawCards(1))
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "65"
        artist = "Zoltan Boros"
        flavorText = "\"The Blessed Sleep? Ha! Wake up my lovelies, there's work to be done!\"\n" +
            "—Gisa, ghoulcaller of Gavony"
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3c0c266b-9ef5-4de2-a358-65739de41491.jpg?1783940828"
        ruling("2011-01-22", "You choose the order of the cards you put on top of your library.")
        ruling(
            "2011-01-22",
            "You can cast Gravepurge with zero targets. If you do, you'll draw a card as the spell resolves."
        )
        ruling(
            "2011-01-22",
            "If you cast Gravepurge with at least one target and all of those targets become illegal, " +
                "the spell won't resolve and you won't get to draw a card."
        )
    }
}
