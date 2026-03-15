package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.costs.PayCost
import com.wingedsheep.sdk.scripting.effects.PayOrSufferEffect

/**
 * Urza's Tome
 * {2}
 * Artifact
 * {3}, {T}: Draw a card. Then discard a card unless you exile a historic card from your graveyard.
 * (Artifacts, legendaries, and Sagas are historic.)
 */
val UrzasTome = card("Urza's Tome") {
    manaCost = "{2}"
    typeLine = "Artifact"
    oracleText = "{3}, {T}: Draw a card. Then discard a card unless you exile a historic card from your graveyard."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap)
        effect = Effects.DrawCards(1)
            .then(
                PayOrSufferEffect(
                    cost = PayCost.Exile(
                        filter = GameObjectFilter.Historic,
                        zone = Zone.GRAVEYARD,
                        count = 1
                    ),
                    suffer = EffectPatterns.discardCards(1)
                )
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "235"
        artist = "Aaron Miller"
        flavorText = "\"Truly understanding even one volume can be a life's work.\" —Naru Meha, master wizard"
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d6bead8b-a67f-4451-9d44-038f8688093f.jpg?1562743701"
    }
}
