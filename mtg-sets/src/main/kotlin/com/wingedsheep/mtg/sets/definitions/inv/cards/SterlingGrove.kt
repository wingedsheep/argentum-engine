package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Sterling Grove
 * {G}{W}
 * Enchantment
 * Other enchantments you control have shroud.
 * {1}, Sacrifice this enchantment: Search your library for an enchantment card, reveal it,
 *   then shuffle and put that card on top.
 *
 * The shroud lord uses a battlefield-scoped [GrantKeyword] excluding the source itself, so Sterling
 * Grove does not protect itself. The search ability shuffles first, then places the found card on
 * top (CR ordering preserved by [SearchDestination.TOP_OF_LIBRARY] with `shuffleAfter = true`).
 */
val SterlingGrove = card("Sterling Grove") {
    manaCost = "{G}{W}"
    colorIdentity = "GW"
    typeLine = "Enchantment"
    oracleText = "Other enchantments you control have shroud. (They can't be the targets of spells or abilities.)\n" +
        "{1}, Sacrifice this enchantment: Search your library for an enchantment card, reveal it, " +
        "then shuffle and put that card on top."

    staticAbility {
        ability = GrantKeyword(
            Keyword.SHROUD,
            GroupFilter.AllEnchantments.youControl().other()
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.SacrificeSelf)
        effect = LibraryPatterns.searchLibrary(
            filter = GameObjectFilter.Enchantment,
            count = 1,
            destination = SearchDestination.TOP_OF_LIBRARY,
            reveal = true,
            shuffleAfter = true
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "278"
        artist = "Jeff Miracola"
        imageUri = "https://cards.scryfall.io/normal/front/4/0/40b26aa3-8169-4978-9554-bd2fc8e18e3b.jpg?1562907957"
    }
}
