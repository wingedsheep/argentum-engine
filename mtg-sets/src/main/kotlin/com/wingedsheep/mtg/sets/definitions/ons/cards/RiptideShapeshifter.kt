package com.wingedsheep.mtg.sets.definitions.ons.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.ChooseCreatureTypeEffect
import com.wingedsheep.sdk.scripting.effects.GatherUntilMatchEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.RevealCollectionEffect
import com.wingedsheep.sdk.scripting.effects.ShuffleLibraryEffect
import com.wingedsheep.sdk.dsl.Effects

/**
 * Riptide Shapeshifter
 * {3}{U}{U}
 * Creature — Shapeshifter
 * 3/3
 * {2}{U}{U}, Sacrifice Riptide Shapeshifter: Choose a creature type. Reveal cards from the top
 * of your library until you reveal a creature card of that type. Put that card onto the
 * battlefield and shuffle the rest into your library.
 */
val RiptideShapeshifter = card("Riptide Shapeshifter") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Shapeshifter"
    power = 3
    toughness = 3
    oracleText = "{2}{U}{U}, Sacrifice Riptide Shapeshifter: Choose a creature type. Reveal cards from the top of your library until you reveal a creature card of that type. Put that card onto the battlefield and shuffle the rest into your library."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{U}{U}"), Costs.SacrificeSelf)
        effect = Effects.Composite(
            listOf(
                ChooseCreatureTypeEffect,
                GatherUntilMatchEffect(
                    filter = GameObjectFilter.Creature.withSubtypeFromVariable("chosenCreatureType"),
                    storeMatch = "found",
                    storeRevealed = "allRevealed"
                ),
                RevealCollectionEffect(from = "allRevealed"),
                MoveCollectionEffect(
                    from = "found",
                    destination = CardDestination.ToZone(Zone.BATTLEFIELD)
                ),
                ShuffleLibraryEffect()
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "109"
        artist = "Arnie Swekel"
        imageUri = "https://cards.scryfall.io/normal/front/8/5/85be34ac-7bc2-4da2-8d9c-2412b9946073.jpg?1562926477"
    }
}
