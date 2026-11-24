package com.wingedsheep.mtg.sets.definitions.ddj.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Korozda Guildmage
 * {B}{G}
 * Creature — Elf Shaman
 * 2/2
 * {1}{B}{G}: Target creature gets +1/+1 and gains intimidate until end of turn.
 * {2}{B}{G}, Sacrifice a nontoken creature: Create X 1/1 green Saproling creature tokens, where X
 * is the sacrificed creature's toughness.
 *
 * Canonical printing: Duel Decks: Izzet vs. Golgari, the card's earliest real printing.
 *
 * X reads the sacrificed creature's last-known toughness through
 * [DynamicAmounts.sacrificedToughness], as Geralf, Visionary Stitcher does.
 */
val KorozdaGuildmage = card("Korozda Guildmage") {
    manaCost = "{B}{G}"
    colorIdentity = "BG"
    typeLine = "Creature — Elf Shaman"
    power = 2
    toughness = 2
    oracleText = "{1}{B}{G}: Target creature gets +1/+1 and gains intimidate until end of turn. " +
        "(It can't be blocked except by artifact creatures and/or creatures that share a color with it.)\n" +
        "{2}{B}{G}, Sacrifice a nontoken creature: Create X 1/1 green Saproling creature tokens, " +
        "where X is the sacrificed creature's toughness."

    activatedAbility {
        cost = Costs.Mana("{1}{B}{G}")
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(1, 1, creature) then
            Effects.GrantKeyword(Keyword.INTIMIDATE, creature)
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}{B}{G}"),
            Costs.Sacrifice(GameObjectFilter.Creature.nontoken()),
        )
        effect = Effects.CreateToken(
            count = DynamicAmounts.sacrificedToughness(),
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Saproling"),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "52"
        artist = "Ryan Pancoast"
        imageUri = "https://cards.scryfall.io/normal/front/d/c/dc863140-221c-4f57-b0a1-933fadadd427.jpg?1783940402"
        ruling(
            "2012-10-01",
            "Use the sacrificed creature's toughness when it was last on the battlefield to determine the value of X.",
        )
    }
}
