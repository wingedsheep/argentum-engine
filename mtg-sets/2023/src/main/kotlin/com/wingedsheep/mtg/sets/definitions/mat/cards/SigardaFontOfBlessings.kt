package com.wingedsheep.mtg.sets.definitions.mat.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CastSpellTypesFromTopOfLibrary
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.LookAtTopOfLibrary
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Sigarda, Font of Blessings — March of the Machine: The Aftermath #47
 * {2}{G}{W} · Legendary Creature — Angel · 4/4
 *
 * Flying
 * Other permanents you control have hexproof.
 * You may look at the top card of your library any time.
 * You may cast Angel spells and Human spells from the top of your library.
 *
 * The top-of-library permission is a cast permission only (normal timing applies), filtered to
 * spells with the Angel or Human subtype — which includes kindred spells of those types.
 */
val SigardaFontOfBlessings = card("Sigarda, Font of Blessings") {
    manaCost = "{2}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Creature — Angel"
    power = 4
    toughness = 4
    oracleText = "Flying\n" +
        "Other permanents you control have hexproof.\n" +
        "You may look at the top card of your library any time.\n" +
        "You may cast Angel spells and Human spells from the top of your library."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = GrantKeyword(
            Keyword.HEXPROOF,
            GroupFilter(GameObjectFilter.Permanent.youControl(), excludeSelf = true),
        )
    }

    staticAbility {
        ability = LookAtTopOfLibrary()
    }

    staticAbility {
        ability = CastSpellTypesFromTopOfLibrary(
            GameObjectFilter.Any.withSubtype("Angel") or GameObjectFilter.Any.withSubtype("Human"),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "47"
        artist = "Justyna Dura"
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cba71725-d6df-44d7-a93b-934573759711.jpg?1783916509"
        ruling(
            "2023-05-12",
            "Sigarda lets you look at the top card of your library whenever you want (with one " +
                "restriction—see below), even if you don't have priority. This action doesn't use the " +
                "stack. Knowing what that card is becomes part of the information you have access to, " +
                "just like you can look at the cards in your hand.",
        )
        ruling(
            "2023-05-12",
            "If the top card of your library changes while you're casting a spell, playing a land, or " +
                "activating an ability, you can't look at the new top card until you finish doing so. " +
                "This means that if you cast the top card of your library, you can't look at the next " +
                "one until you're done paying for that spell.",
        )
    }
}
