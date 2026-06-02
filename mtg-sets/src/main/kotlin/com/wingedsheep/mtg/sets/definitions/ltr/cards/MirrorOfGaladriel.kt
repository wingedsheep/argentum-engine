package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Mirror of Galadriel
 * {2}
 * Legendary Artifact
 *
 * {5}, {T}: Scry 1, then draw a card. This ability costs {1} less to activate for each
 * legendary creature you control.
 */
val MirrorOfGaladriel = card("Mirror of Galadriel") {
    manaCost = "{2}"
    typeLine = "Legendary Artifact"
    oracleText = "{5}, {T}: Scry 1, then draw a card. This ability costs {1} less to activate for each legendary creature you control."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{5}"), Costs.Tap)
        effect = LibraryPatterns.scry(1).then(Effects.DrawCards(1))
        // "costs {1} less to activate for each legendary creature you control"
        genericCostReduction = DynamicAmount.AggregateBattlefield(
            player = Player.You,
            filter = GameObjectFilter.Creature.legendary()
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "244"
        artist = "Kasia 'Kafis' Zielińska"
        flavorText = "\"Your Quest stands upon the edge of a knife. Stray but a little and it will fail, to the ruin of all. Yet hope remains while all the Company is true.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bc95036f-98f2-4ea7-93bb-542ad7064540.jpg?1686970213"
    }
}
