package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Wight of the Reliquary
 * {B}{G}
 * Creature — Zombie Knight
 * 2/2
 * Vigilance
 * This creature gets +1/+1 for each creature card in your graveyard.
 * {T}, Sacrifice another creature: Search your library for a land card, put it onto the
 * battlefield tapped, then shuffle.
 *
 * The sacrificed creature hits the graveyard as a cost, so it already counts toward the
 * self-buff by the time the search resolves.
 */
val WightOfTheReliquary = card("Wight of the Reliquary") {
    manaCost = "{B}{G}"
    colorIdentity = "BG"
    typeLine = "Creature — Zombie Knight"
    power = 2
    toughness = 2
    oracleText = "Vigilance\n" +
        "This creature gets +1/+1 for each creature card in your graveyard.\n" +
        "{T}, Sacrifice another creature: Search your library for a land card, put it onto the battlefield tapped, then shuffle."

    keywords(Keyword.VIGILANCE)

    staticAbility {
        ability = GrantDynamicStats(
            filter = GroupFilter.source(),
            powerBonus = DynamicAmounts.creatureCardsInYourGraveyard(),
            toughnessBonus = DynamicAmounts.creatureCardsInYourGraveyard()
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.SacrificeAnother(GameObjectFilter.Creature))
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Land,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true
        )
        description = "{T}, Sacrifice another creature: Search your library for a land card, put it onto the battlefield tapped, then shuffle."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "207"
        artist = "Scott Murphy"
        flavorText = "Her new demonic overlords prized her knowledge of Bant's landscape."
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a15570c0-4210-4959-8584-987ec85f8283.jpg?1783911244"
    }
}
