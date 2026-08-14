package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.startYourEngines
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Point the Way — Aetherdrift #175
 * {G} · Enchantment
 *
 * Start your engines!
 * {3}{G}, Sacrifice this enchantment: Search your library for up to X basic land cards, where X is
 * your speed. Put those cards onto the battlefield tapped, then shuffle.
 *
 * The activated ability is *not* max-speed gated — it is available at any speed and just finds
 * fewer lands, so X is a [DynamicAmounts.speed] read at resolution rather than a gate.
 *
 * Sacrificing the enchantment is a cost, so it is gone by the time X is read. That costs nothing:
 * start your engines! only raises a player with *no* speed to 1 as a state-based action, and
 * "losing control of permanents with start your engines! doesn't affect your speed"
 * (Scryfall ruling 2025-02-07). A player who has reached max speed still finds four lands.
 *
 * "Up to X" is honest — [Patterns.Library.searchLibrary] selects with `ChooseUpTo`, so finding
 * fewer than X (or none) is legal, and X = 0 leaves a search that finds nothing but still fires
 * "whenever a player searches their library" triggers (CR 701.23b).
 */
val PointTheWay = card("Point the Way") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "Start your engines! (If you have no speed, it starts at 1. It increases once on " +
        "each of your turns when an opponent loses life. Max speed is 4.)\n" +
        "{3}{G}, Sacrifice this enchantment: Search your library for up to X basic land cards, " +
        "where X is your speed. Put those cards onto the battlefield tapped, then shuffle."

    startYourEngines()

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}{G}"), Costs.SacrificeSelf)
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand,
            count = DynamicAmounts.speed(),
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true
        )
        description = "{3}{G}, Sacrifice this enchantment: Search your library for up to X basic " +
            "land cards, where X is your speed. Put those cards onto the battlefield tapped, then " +
            "shuffle."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "175"
        artist = "Izzy"
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8fd4c73d-0e9a-4ffe-8062-f2f4d0e601fe.jpg?1783907867"
    }
}
