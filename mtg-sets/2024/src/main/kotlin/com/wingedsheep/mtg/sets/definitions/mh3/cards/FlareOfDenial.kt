package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.SelfAlternativeCost
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Flare of Denial {1}{U}{U}
 * Instant
 *
 * You may sacrifice a nontoken blue creature rather than pay this spell's mana cost.
 * Counter target spell.
 */
val FlareOfDenial = card("Flare of Denial") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "You may sacrifice a nontoken blue creature rather than pay this spell's mana cost.\n" +
        "Counter target spell."

    selfAlternativeCost = SelfAlternativeCost(
        manaCost = ManaCost.parse("{0}"),
        additionalCosts = listOf(
            Costs.additional.SacrificePermanent(GameObjectFilter.Creature.withColor(Color.BLUE).nontoken())
        )
    )

    spell {
        target(TargetFilter.SpellOnStack)
        effect = Effects.CounterSpell()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "62"
        artist = "Jason A. Engle"
        flavorText = "\"I see where you're going with that, and I don't like it.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/1/71a98efb-9b0a-496b-ac21-8d70527ea544.jpg?1783911290"
    }
}
