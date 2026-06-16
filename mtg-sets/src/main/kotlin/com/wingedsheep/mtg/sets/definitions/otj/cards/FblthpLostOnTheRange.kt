package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.LookAtTopOfLibrary
import com.wingedsheep.sdk.scripting.PlotFromTopOfLibrary
import com.wingedsheep.sdk.scripting.effects.WardCost

/**
 * Fblthp, Lost on the Range
 * {1}{U}{U}
 * Legendary Creature — Homunculus
 * 1/1
 *
 * Ward {2}
 * You may look at the top card of your library any time.
 * The top card of your library has plot. The plot cost is equal to its mana cost.
 * You may plot nonland cards from the top of your library.
 *
 * Ward {2} is the standard [KeywordAbility.Ward]. The visibility clause is [LookAtTopOfLibrary].
 * The two plot clauses combine into the [PlotFromTopOfLibrary] static ability (filtered to
 * nonland, since plotting a land is meaningless): the plot legal-action enumerator offers the top
 * card of the library as a plot action at a cost equal to its mana cost, and [PlotCardHandler]
 * moves it from library → exile and plots it (CR 718 — sorcery-speed special action; can't be
 * cast the turn it's plotted).
 */
val FblthpLostOnTheRange = card("Fblthp, Lost on the Range") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Homunculus"
    power = 1
    toughness = 1
    oracleText = "Ward {2}\n" +
        "You may look at the top card of your library any time.\n" +
        "The top card of your library has plot. The plot cost is equal to its mana cost.\n" +
        "You may plot nonland cards from the top of your library."

    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{2}")))

    staticAbility {
        ability = LookAtTopOfLibrary
    }
    staticAbility {
        ability = PlotFromTopOfLibrary(filter = GameObjectFilter.Nonland)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "48"
        artist = "Brian Valeza"
        imageUri = "https://cards.scryfall.io/normal/front/0/1/01d3e6ea-4791-4948-af22-c1bd04c34c1e.jpg?1712355420"

        ruling("2024-04-12", "If the top card of your library normally has plot, you may plot that card using its own plot cost or the plot cost given to it by Fblthp.")
        ruling("2024-04-12", "If you plot the top card of your library, you must exile that card and pay its plot cost before you may look at the new top card of your library.")
        ruling("2024-04-12", "You can't cast a plotted card on the same turn it became plotted.")
    }
}
