package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Vraska's Stoneglare — Guilds of Ravnica #272 (canonical printing; planeswalker deck)
 * {4}{B}{G} · Sorcery
 *
 * Destroy target creature. You gain life equal to its toughness. You may search your library
 * and/or graveyard for a card named Vraska, Regal Gorgon, reveal it, and put it into your hand.
 * If you search your library this way, shuffle.
 *
 * The life is read off the creature before it's destroyed (the Vendetta shape): nothing happens
 * between the two steps, so that's the toughness it last had on the battlefield, and an
 * indestructible creature that survives still yields its current toughness (ruling). The search
 * half is the Ral's Dispersal planeswalker-deck tutor; with its only target illegal the spell
 * doesn't resolve, so there's no life gain or search (ruling) — the engine's fizzle rule.
 */
val VraskasStoneglare = card("Vraska's Stoneglare") {
    manaCost = "{4}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Sorcery"
    oracleText = "Destroy target creature. You gain life equal to its toughness. You may search your " +
        "library and/or graveyard for a card named Vraska, Regal Gorgon, reveal it, and put it into your " +
        "hand. If you search your library this way, shuffle."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.GainLife(DynamicAmounts.toughnessOf(creature)) then
            Effects.Destroy(creature) then
            Patterns.Library.searchLibraryAndOrGraveyard(
                filter = GameObjectFilter.Any.named("Vraska, Regal Gorgon"),
                destination = SearchDestination.HAND,
                reveal = true,
                optional = true
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "272"
        artist = "Yongjae Choi"
        imageUri = "https://cards.scryfall.io/normal/front/2/7/27fc4db6-a5f5-4254-ae64-c8eaf2c98030.jpg?1783934093"
        ruling("2018-10-05", "If the target creature becomes an illegal target for Vraska’s Stoneglare, the spell doesn’t resolve. You don’t gain life or search for Vraska, Regal Gorgon.")
        ruling("2018-10-05", "The amount of life gained is equal to the destroyed creature’s toughness as it last existed on the battlefield. If it wasn’t destroyed (most likely because it has indestructible), you gain life equal to its toughness as it currently exists on the battlefield.")
    }
}
