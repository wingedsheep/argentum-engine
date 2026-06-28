package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Waterbending Scroll
 * {1}{U}
 * Artifact
 * {6}, {T}: Draw a card. This ability costs {1} less to activate for each Island you control.
 *
 * The cost reduction rides the existing
 * [com.wingedsheep.sdk.scripting.ActivatedAbility.genericCostReduction] field: a
 * [com.wingedsheep.sdk.scripting.values.DynamicAmount] battlefield count of Islands you control reduces
 * the generic-mana portion of the {6} cost by {1} per Island (floored at {0}, colored pips untouched),
 * evaluated once before costs are paid so the enumerator and handler stay in sync.
 */
val WaterbendingScroll = card("Waterbending Scroll") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Artifact"
    oracleText = "{6}, {T}: Draw a card. This ability costs {1} less to activate for each Island you control."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{6}"), Costs.Tap)
        effect = DrawCardsEffect(1)
        genericCostReduction =
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Land.withSubtype("Island")).count()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "81"
        artist = "Dee Nguyen"
        flavorText = "The scroll taught Katara many useful skills. So did fighting the angry pirates she stole it from."
        imageUri = "https://cards.scryfall.io/normal/front/5/0/50d0be73-a2c8-44a0-9178-9949f342f6f9.jpg?1764120553"
    }
}
