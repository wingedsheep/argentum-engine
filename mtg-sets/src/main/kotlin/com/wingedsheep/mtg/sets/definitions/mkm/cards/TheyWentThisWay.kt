package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * They Went This Way — Murders at Karlov Manor #178
 * {2}{G} · Sorcery
 *
 * Search your library for a basic land card, put it onto the battlefield tapped, then shuffle.
 * Investigate.
 *
 * A green ramp spell with a Clue stapled on. Both sentences live in one spell resolution, so
 * the two halves are a plain [Effects.then] chain rather than separate abilities.
 *
 * "Search your library for a basic land card" is not "up to one" — but searching never *forces*
 * a find (CR 701.23b), which is what `searchLibrary`'s `ChooseUpTo` selection models. Declining
 * to find still shuffles and still investigates: the Clue rides on the same resolution and has
 * no dependency on the search succeeding.
 */
val TheyWentThisWay = card("They Went This Way") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Search your library for a basic land card, put it onto the battlefield tapped, " +
        "then shuffle. Investigate. (Create a Clue token. It's an artifact with " +
        "\"{2}, Sacrifice this token: Draw a card.\")"

    spell {
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true
        ) then Effects.Investigate()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "178"
        artist = "Andreas Zafiratos"
        flavorText = "\"Running doesn't prove guilt, but innocent people don't typically flee " +
            "into the North Ridge Forest.\"\n—Alst of the Foundway Associates"
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f4a31d4a-34bc-46b4-b20f-a5460191b35d.jpg?1783912860"

        ruling(
            "2024-02-02",
            "Clue is an artifact type. Even though it appears on some cards with other permanent " +
                "types, it's never a creature type, a land type, or anything but an artifact type."
        )
        ruling(
            "2024-02-02",
            "If an effect refers to a Clue, it means any Clue artifact, not just a Clue artifact " +
                "token."
        )
    }
}
