package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Elvish Clancaller
 * {G}{G}
 * Creature — Elf Druid
 * 1/1
 *
 * Other Elves you control get +1/+1.
 * {4}{G}{G}, {T}: Search your library for a card named Elvish Clancaller, put it onto the
 * battlefield, then shuffle.
 */
val ElvishClancaller = card("Elvish Clancaller") {
    manaCost = "{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Druid"
    power = 1
    toughness = 1
    oracleText = "Other Elves you control get +1/+1.\n" +
        "{4}{G}{G}, {T}: Search your library for a card named Elvish Clancaller, put it onto the battlefield, then shuffle."

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(
                GameObjectFilter.Permanent.withSubtype(Subtype.ELF).youControl(),
                excludeSelf = true
            )
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}{G}{G}"), Costs.Tap)
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Any.named("Elvish Clancaller"),
            count = 1,
            destination = SearchDestination.BATTLEFIELD,
            shuffleAfter = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "179"
        artist = "Matt Stewart"
        flavorText = "No single root feeds the forest."
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4da3b0bc-84da-4b85-992b-d0700c97157c.jpg?1783934537"
    }
}
