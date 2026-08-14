package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CastSpellTypesFromTopOfLibrary
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.LookAtTopOfLibrary
import com.wingedsheep.sdk.scripting.SpendAnyManaTypeForSpells

/**
 * Vizier of the Menagerie
 * {3}{G}
 * Creature — Snake Cleric
 * 3/4
 *
 * You may look at the top card of your library any time.
 * You may cast creature spells from the top of your library.
 * You can spend mana of any type to cast creature spells.
 *
 * Three independent statics, each already an engine concept:
 *
 * - [LookAtTopOfLibrary] — the private peek (revealed to the controller only, unlike Future
 *   Sight's [com.wingedsheep.sdk.scripting.PlayFromTopOfLibrary], which reveals to everyone).
 * - [CastSpellTypesFromTopOfLibrary] filtered to creatures — a *cast* permission only, so ordinary
 *   timing still applies: sorcery-speed unless the card itself has flash, and the card is not in
 *   your hand, so it can't be cycled, discarded, or have its activated abilities used (rulings).
 * - [SpendAnyManaTypeForSpells] filtered to creatures — deliberately **not** scoped to the
 *   top-of-library permission. Per the ruling, "You may spend mana as though it were mana of any
 *   type to cast any creature spell, not just creature spells that you cast from the top of your
 *   library", so this is a zone-agnostic static that also relaxes creature spells cast from hand.
 *   Only the *mana* portion is relaxed — additional and alternative costs are unaffected, matching
 *   "You'll still pay all costs for that spell, including additional costs."
 */
val VizierOfTheMenagerie = card("Vizier of the Menagerie") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Snake Cleric"
    power = 3
    toughness = 4
    oracleText = "You may look at the top card of your library any time.\n" +
        "You may cast creature spells from the top of your library.\n" +
        "You can spend mana of any type to cast creature spells."

    staticAbility {
        ability = LookAtTopOfLibrary
    }

    staticAbility {
        ability = CastSpellTypesFromTopOfLibrary(GameObjectFilter.Creature)
    }

    staticAbility {
        ability = SpendAnyManaTypeForSpells(GameObjectFilter.Creature)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "192"
        artist = "Victor Adame Minguez"
        imageUri = "https://cards.scryfall.io/normal/front/c/a/ca204351-7a7e-4e4b-8c2b-f90fa0f9d724.jpg?1783936465"
        ruling(
            "2017-04-18",
            "Vizier of the Menagerie lets you look at the top card of your library whenever you " +
                "want (with one restriction—see below), even if you don't have priority. This action " +
                "doesn't use the stack. Knowing what that card is becomes part of the information you " +
                "have access to, just like you can look at the cards in your hand.",
        )
        ruling(
            "2017-04-18",
            "If the top card of your library changes while you're casting a spell, playing a land, " +
                "or activating an ability, you can't look at the new top card until you finish doing " +
                "so. This means that if you cast the top card of your library, you can't look at the " +
                "next one until you're done paying for that spell.",
        )
        ruling(
            "2017-04-18",
            "Normally, Vizier of the Menagerie allows you to cast the top card of your library if " +
                "it's a creature card, it's your main phase, and the stack is empty. If that creature " +
                "card has flash, you'll be able to cast it any time you could cast an instant, even " +
                "on an opponent's turn.",
        )
        ruling(
            "2017-04-18",
            "You may spend mana as though it were mana of any type to cast any creature spell, not " +
                "just creature spells that you cast from the top of your library.",
        )
        ruling(
            "2017-04-18",
            "You'll still pay all costs for that spell, including additional costs. You may also pay " +
                "alternative costs such as emerge or that of As Foretold.",
        )
        ruling(
            "2017-04-18",
            "The top card of your library isn't in your hand, so you can't cycle it, discard it, or " +
                "activate any of its activated abilities.",
        )
    }
}
