package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Fiend Artisan — Ikoria: Lair of Behemoths #220
 * {B/G}{B/G} · Creature — Nightmare · 1/1
 *
 * This creature gets +1/+1 for each creature card in your graveyard.
 * {X}{B/G}, {T}, Sacrifice another creature: Search your library for a creature card with mana
 * value X or less, put it onto the battlefield, then shuffle. Activate only as a sorcery.
 *
 * The Wight of the Reliquary shape: a [GrantDynamicStats] on the source counting creature cards in
 * your graveyard, plus a tap-and-sacrifice-another activated search. The search filter reads the X
 * paid in the activation cost via [GameObjectFilter.manaValueAtMostX] (as Vivien's Arkbow does);
 * an {X} in a library card's own mana cost counts as 0 when comparing.
 */
val FiendArtisan = card("Fiend Artisan") {
    manaCost = "{B/G}{B/G}"
    colorIdentity = "BG"
    typeLine = "Creature — Nightmare"
    power = 1
    toughness = 1
    oracleText = "This creature gets +1/+1 for each creature card in your graveyard.\n" +
        "{X}{B/G}, {T}, Sacrifice another creature: Search your library for a creature card with mana value X or less, put it onto the battlefield, then shuffle. Activate only as a sorcery."

    staticAbility {
        ability = GrantDynamicStats(
            filter = GroupFilter.source(),
            powerBonus = DynamicAmounts.creatureCardsInYourGraveyard(),
            toughnessBonus = DynamicAmounts.creatureCardsInYourGraveyard()
        )
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{X}{B/G}"),
            Costs.Tap,
            Costs.SacrificeAnother(GameObjectFilter.Creature)
        )
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Creature.manaValueAtMostX(),
            destination = SearchDestination.BATTLEFIELD
        )
        timing = TimingRule.SorcerySpeed
        description = "{X}{B/G}, {T}, Sacrifice another creature: Search your library for a creature card with mana value X or less, put it onto the battlefield, then shuffle. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "220"
        artist = "Yigit Koroglu"
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6cd9d800-6d31-42e2-87d2-772db0ff95ed.jpg?1783931012"
        ruling("2020-04-17", "If a card in a player's library has {X} in its mana cost, X is considered to be 0.")
        ruling("2020-04-17", "Fiend Artisan's first ability applies only while it's on the battlefield.")
        ruling("2020-04-17", "Because damage remains marked on a creature until the damage is removed as the turn ends, nonlethal damage dealt to Fiend Artisan may become lethal if creature cards leave your graveyard during that turn.")
    }
}
