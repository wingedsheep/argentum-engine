package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CastSpellTypesFromTopOfLibrary
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.LookAtTopOfLibrary

/**
 * Errant and Giada — March of the Machine #224
 * {1}{W}{U} · Legendary Creature — Human Angel 2/3
 *
 * The private look ([LookAtTopOfLibrary]) plus a cast-only top-of-library permission filtered to
 * spells with flash *or* flying — no land play and no public reveal, exactly Vizier of the
 * Menagerie's shape with a keyword union for the filter.
 */
val ErrantAndGiada = card("Errant and Giada") {
    manaCost = "{1}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Creature — Human Angel"
    power = 2
    toughness = 3
    oracleText = "Flash\n" +
        "Flying\n" +
        "You may look at the top card of your library any time.\n" +
        "You may cast spells with flash or flying from the top of your library."

    keywords(Keyword.FLASH, Keyword.FLYING)

    staticAbility {
        ability = LookAtTopOfLibrary
    }

    staticAbility {
        ability = CastSpellTypesFromTopOfLibrary(
            GameObjectFilter.Any.withKeyword(Keyword.FLASH) or GameObjectFilter.Any.withKeyword(Keyword.FLYING)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "224"
        artist = "Cristi Balanescu"
        imageUri = "https://cards.scryfall.io/normal/front/5/4/54aa2d03-7713-44d4-8fca-45c6f77b174b.jpg?1783916955"
        ruling("2023-04-14", "Errant and Giada lets you look at the top card of your library whenever you want (with one restriction—see below), even if you don't have priority. This action doesn't use the stack. Knowing what that card is becomes part of the information you have access to, just like you can look at the cards in your hand.")
        ruling("2023-04-14", "If the top card of your library changes while you're casting a spell, playing a land, or activating an ability, you can't look at the new top card until you finish doing so. This means that if you cast the top card of your library, you can't look at the next one until you're done paying for that spell.")
    }
}
