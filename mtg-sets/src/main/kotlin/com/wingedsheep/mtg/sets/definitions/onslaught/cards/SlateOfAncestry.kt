package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DrawCardsEffect
import com.wingedsheep.sdk.scripting.DynamicAmount
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.Player

/**
 * Slate of Ancestry
 * {4}
 * Artifact
 * {4}, {T}, Discard your hand: Draw a card for each creature you control.
 */
val SlateOfAncestry = card("Slate of Ancestry") {
    manaCost = "{4}"
    typeLine = "Artifact"
    oracleText = "{4}, {T}, Discard your hand: Draw a card for each creature you control."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{4}"),
            Costs.Tap,
            Costs.DiscardHand
        )
        effect = DrawCardsEffect(
            count = DynamicAmount.CountBattlefield(Player.You, GameObjectFilter.Creature),
            target = EffectTarget.Controller
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "310"
        artist = "Corey D. Macourek"
        flavorText = "\"The pattern of life can be studied like a book, if you know how to read it.\""
        imageUri = "https://cards.scryfall.io/large/front/a/e/ae596e8c-04f5-48b0-b5e2-683c74912e85.jpg?1562936203"
    }
}
