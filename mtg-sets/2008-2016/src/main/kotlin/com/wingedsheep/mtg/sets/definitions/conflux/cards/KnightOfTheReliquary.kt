package com.wingedsheep.mtg.sets.definitions.conflux.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Knight of the Reliquary — Conflux #113 (earliest printing)
 * {1}{G}{W} · Creature — Human Knight · 2/2
 *
 * This creature gets +1/+1 for each land card in your graveyard.
 * {T}, Sacrifice a Forest or Plains: Search your library for a land card, put it onto the
 * battlefield, then shuffle.
 *
 * The Liliana's Elite shape: a [GrantDynamicStats] on [GroupFilter.source] counting land cards in
 * your graveyard — a layer-7c +N/+N on the printed 2/2 (not a characteristic-defining P/T), so it
 * only applies on the battlefield, per the ruling. The fetch is a tap + sacrifice-a-permanent-with-
 * subtype-Forest-or-Plains cost into [Patterns.Library.searchLibrary] to the battlefield; the
 * sacrificed land lands in the graveyard as a cost, so it already counts by resolution.
 */
val KnightOfTheReliquary = card("Knight of the Reliquary") {
    manaCost = "{1}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Creature — Human Knight"
    power = 2
    toughness = 2
    oracleText = "This creature gets +1/+1 for each land card in your graveyard.\n" +
        "{T}, Sacrifice a Forest or Plains: Search your library for a land card, put it onto the battlefield, then shuffle."

    staticAbility {
        ability = GrantDynamicStats(
            filter = GroupFilter.source(),
            powerBonus = DynamicAmounts.count(Player.You, Zone.GRAVEYARD, GameObjectFilter.Land),
            toughnessBonus = DynamicAmounts.count(Player.You, Zone.GRAVEYARD, GameObjectFilter.Land)
        )
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.Sacrifice(GameObjectFilter.Permanent.withAnySubtype("Forest", "Plains"))
        )
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Land,
            destination = SearchDestination.BATTLEFIELD
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "113"
        artist = "Michael Komarck"
        flavorText = "\"Knowledge of Bant's landscape and ruins is a weapon that the invaders can't comprehend.\"\n—Elspeth"
        imageUri = "https://cards.scryfall.io/normal/front/a/d/ad8b8518-c09e-4cb7-95b2-08e4e370d89c.jpg?1783942468"
        ruling(
            "2021-03-19",
            "Knight of the Reliquary's first ability applies only while it's on the battlefield. In all other zones, it's a 2/2 creature."
        )
    }
}
