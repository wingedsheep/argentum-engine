package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Herd Migration
 * {6}{G}
 * Sorcery
 * Domain — Create a 3/3 green Beast creature token for each basic land type among lands you control.
 * {1}{G}, Discard this card: Search your library for a basic land card, reveal it, put it into your
 * hand, then shuffle. You gain 3 life.
 *
 * The token count is [DynamicAmounts.domain], read on resolution. The second ability is not
 * landcycling (it gains 3 life too), so it is a hand-zone activated ability with [Costs.DiscardSelf],
 * the same shape as Spinewoods Armadillo.
 */
val HerdMigration = card("Herd Migration") {
    manaCost = "{6}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Domain — Create a 3/3 green Beast creature token for each basic land type among " +
        "lands you control.\n{1}{G}, Discard this card: Search your library for a basic land card, " +
        "reveal it, put it into your hand, then shuffle. You gain 3 life."

    spell {
        effect = Effects.CreateToken(
            count = DynamicAmounts.domain(),
            power = 3,
            toughness = 3,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Beast"),
            imageUri = "https://cards.scryfall.io/normal/front/5/9/591cb5b2-6394-4c1a-b227-5c32e633b493.jpg?1783921126",
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{G}"), Costs.DiscardSelf)
        activateFromZone = Zone.HAND
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand,
            destination = SearchDestination.HAND,
            reveal = true,
            shuffleAfter = true
        ) then
            Effects.GainLife(3)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "165"
        artist = "Eric Deschamps"
        imageUri = "https://cards.scryfall.io/normal/front/b/0/b0244a1f-e696-4223-9c14-22c2ca3cb738.jpg?1783921301"
    }
}
