package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CastSpellTypesFromTopOfLibrary
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.LookAtTopOfLibrary

/**
 * Realmwalker
 * {2}{G}
 * Creature — Shapeshifter
 * 2/3
 *
 * Changeling (This card is every creature type.)
 * As this creature enters, choose a creature type.
 * You may look at the top card of your library any time.
 * You may cast creature spells of the chosen type from the top of your library.
 *
 * Vizier of the Menagerie's two statics, with the cast permission's filter narrowed by
 * `withChosenSubtype()` — read against the Realmwalker that grants it. With no choice made the
 * filter matches nothing, so the peek still works but nothing is castable (ruling).
 */
val Realmwalker = card("Realmwalker") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Shapeshifter"
    power = 2
    toughness = 3
    oracleText = "Changeling (This card is every creature type.)\n" +
        "As this creature enters, choose a creature type.\n" +
        "You may look at the top card of your library any time.\n" +
        "You may cast creature spells of the chosen type from the top of your library."

    keywords(Keyword.CHANGELING)

    replacementEffect(EntersWithChoice(ChoiceType.CREATURE_TYPE))

    staticAbility {
        ability = LookAtTopOfLibrary()
    }

    staticAbility {
        ability = CastSpellTypesFromTopOfLibrary(GameObjectFilter.Creature.withChosenSubtype())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "188"
        artist = "Zack Stella"
        imageUri = "https://cards.scryfall.io/normal/front/c/8/c8212667-7e18-42a5-9f36-4f8a6ad12f83.jpg?1783928207"
        ruling(
            "2021-02-05",
            "The top card of your library isn't in your hand, so you can't foretell it, discard it, or " +
                "activate any of its activated abilities.",
        )
        ruling(
            "2021-02-05",
            "Realmwalker doesn't change when you can cast creature spells. Normally, this means during " +
                "your main phase when the stack is empty, although flash may change this.",
        )
        ruling(
            "2021-02-05",
            "If Realmwalker is somehow on the battlefield with no choice having been made for it, you may " +
                "still look at the top card of your library any time. You won't be able to cast any " +
                "creature spells from the top of your library, even if they have no creature types.",
        )
        ruling(
            "2021-02-05",
            "You can look at the top card of your library whenever you want (with one restriction; see " +
                "below), even if you don't have priority. This action doesn't use the stack. Knowing what " +
                "that card is becomes part of the information you have access to, just like you can look " +
                "at the cards in your hand.",
        )
        ruling(
            "2021-02-05",
            "You must choose an existing creature type, such as Elk or Advisor. You can't choose card " +
                "types (e.g., artifact) or supertypes (e.g., snow).",
        )
        ruling(
            "2021-02-05",
            "You'll still pay all costs for the spell, including additional costs. You may also pay " +
                "alternative costs if any are available.",
        )
        ruling(
            "2021-02-05",
            "If the top card of your library changes while you're casting a spell, playing a land, or " +
                "activating an ability, you can't look at the new top card until you finish doing so. " +
                "This means that if you cast a spell from the top of your library, you can't look at the " +
                "next one until you're done paying for that spell.",
        )
    }
}
