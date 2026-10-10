package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Elvish Reclaimer
 * {G}
 * Creature — Elf Warrior
 * 1/2
 * This creature gets +2/+2 as long as there are three or more land cards in your graveyard.
 * {2}, {T}, Sacrifice a land: Search your library for a land card, put it onto the battlefield
 * tapped, then shuffle.
 *
 * The buff is the Reclusive Taxidermist shape (a [ConditionalStaticAbility] over [ModifyStats]
 * on the source) gated on land cards in your graveyard; the activated ability is the Warped
 * Landscape search with a sacrifice-a-land cost instead of sacrifice-self.
 */
val ElvishReclaimer = card("Elvish Reclaimer") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Warrior"
    power = 1
    toughness = 2
    oracleText = "This creature gets +2/+2 as long as there are three or more land cards in your graveyard.\n" +
        "{2}, {T}, Sacrifice a land: Search your library for a land card, put it onto the battlefield tapped, then shuffle."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(powerBonus = 2, toughnessBonus = 2, filter = GroupFilter.source()),
            condition = Conditions.CardsInGraveyardMatchingAtLeast(3, GameObjectFilter.Land)
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap, Costs.Sacrifice(GameObjectFilter.Land))
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Land,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "169"
        artist = "Victor Adame Minguez"
        imageUri = "https://cards.scryfall.io/normal/front/3/9/39c431d7-d94b-46c4-bb89-f3db56214ab4.jpg?1783932966"
    }
}
