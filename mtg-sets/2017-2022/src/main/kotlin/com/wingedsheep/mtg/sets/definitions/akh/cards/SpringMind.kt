package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Spring // Mind — Amonkhet #219
 *
 * Spring {2}{G} — Sorcery: Search your library for a basic land card, put it onto the battlefield
 *   tapped, then shuffle.
 * Mind {4}{U}{U} — Instant: Aftermath (Cast this spell only from your graveyard. Then exile it.)
 *   Draw two cards.
 */
val SpringMind = card("Spring // Mind") {
    layout = CardLayout.SPLIT
    colorIdentity = "GU"

    face("Spring") {
        manaCost = "{2}{G}"
        typeLine = "Sorcery"
        oracleText = "Search your library for a basic land card, put it onto the battlefield tapped, then shuffle."

        spell {
            effect = Patterns.Library.searchLibrary(
                filter = GameObjectFilter.BasicLand,
                destination = SearchDestination.BATTLEFIELD,
                entersTapped = true
            )
        }
    }

    face("Mind") {
        manaCost = "{4}{U}{U}"
        typeLine = "Instant"
        oracleText = "Aftermath (Cast this spell only from your graveyard. Then exile it.)\n" +
            "Draw two cards."
        keywords(Keyword.AFTERMATH)

        spell {
            effect = Effects.DrawCards(2)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "219"
        artist = "Josu Hernaiz"
        imageUri = "https://cards.scryfall.io/normal/front/6/4/6431d464-1f2b-42c4-ad38-67b7d0984080.jpg?1783936454"
        ruling("2017-07-14", "Once you've started to cast a spell with aftermath from your graveyard, the card is immediately moved to the stack. Opponents can't try to stop the ability by exiling the card with an effect such as that of Crook of Condemnation.")
        ruling("2017-04-18", "If another effect allows you to cast a split card with aftermath from any zone other than a graveyard, you can't cast the half with aftermath.")
        ruling("2017-04-18", "If another effect allows you to cast a split card with aftermath from a graveyard, you may cast either half. If you cast the half that has aftermath, you'll exile the card if it would leave the stack.")
        ruling("2017-04-18", "A spell with aftermath cast from a graveyard will always be exiled afterward, whether it resolves, it's countered, or it leaves the stack in some other way.")
        ruling("2017-04-18", "If you cast the first half of a split card with aftermath during your turn, you'll have priority immediately after it resolves. You can cast the half with aftermath from your graveyard before any player can take any other action if it's legal for you to do so.")
    }
}
