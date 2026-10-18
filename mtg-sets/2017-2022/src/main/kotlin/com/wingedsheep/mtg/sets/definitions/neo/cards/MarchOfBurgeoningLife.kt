package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * March of Burgeoning Life — Kamigawa: Neon Dynasty #201 (canonical printing)
 * {X}{G} · Instant
 *
 * As an additional cost to cast this spell, you may exile any number of green cards from your hand.
 * This spell costs {2} less to cast for each card exiled this way.
 * Choose target creature with mana value less than X. Search your library for a creature card with
 * the same name as that creature, put it onto the battlefield tapped, then shuffle.
 *
 * The exiled cards are a discount, not the X: each takes {2} off the total cost including the
 * announced X, and the target bound reads the announced X. The search filter names the target
 * (`sharingNameWith(target)`), compared against the target's projected name at resolution; the
 * mana-value bound sits on the target only, as printed.
 */
val MarchOfBurgeoningLife = card("March of Burgeoning Life") {
    manaCost = "{X}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "As an additional cost to cast this spell, you may exile any number of green cards " +
        "from your hand. This spell costs {2} less to cast for each card exiled this way.\n" +
        "Choose target creature with mana value less than X. Search your library for a creature card " +
        "with the same name as that creature, put it onto the battlefield tapped, then shuffle."

    additionalCost(Costs.additional.ExileAnyNumberFromHandForCostReduction(
        GameObjectFilter.Any.withColor(Color.GREEN), reductionPerUnit = 2
    ))

    spell {
        val creature = target(TargetFilter(GameObjectFilter.Creature.manaValueLessThanX()))
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Creature.sharingNameWith(creature),
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "201"
        artist = "Wylie Beckert"
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0e223d99-408a-413d-abb1-cb2f11ae521d.jpg?1783923843"
    }
}
