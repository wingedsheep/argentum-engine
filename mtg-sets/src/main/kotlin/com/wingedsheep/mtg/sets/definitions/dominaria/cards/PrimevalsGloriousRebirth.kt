package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Primevals' Glorious Rebirth
 * {5}{W}{B}
 * Legendary Sorcery
 *
 * (You may cast a legendary sorcery only if you control a legendary creature or planeswalker.)
 * Return all legendary permanent cards from your graveyard to the battlefield.
 */
val PrimevalsGloriousRebirth = card("Primevals' Glorious Rebirth") {
    manaCost = "{5}{W}{B}"
    typeLine = "Legendary Sorcery"
    oracleText = "(You may cast a legendary sorcery only if you control a legendary creature or planeswalker.)\nReturn all legendary permanent cards from your graveyard to the battlefield."

    spell {
        castOnlyIf(Conditions.ControlLegendaryCreatureOrPlaneswalker)
        effect = CompositeEffect(
            listOf(
                GatherCardsEffect(
                    source = CardSource.FromZone(
                        zone = Zone.GRAVEYARD,
                        player = Player.You,
                        filter = GameObjectFilter.Permanent.legendary()
                    ),
                    storeAs = "legendaryPermanents"
                ),
                MoveCollectionEffect(
                    from = "legendaryPermanents",
                    destination = CardDestination.ToZone(Zone.BATTLEFIELD)
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "201"
        artist = "Noah Bradley"
        flavorText = "Centuries ago, five dragons conquered death to rule the living."
        imageUri = "https://cards.scryfall.io/normal/front/8/0/80dd2950-502c-4859-85fe-9fbef09aef43.jpg?1562738592"
        ruling("2018-04-27", "You must return all legendary permanent cards to the battlefield, even if the \"legend rule\" will put some right back into your graveyard.")
        ruling("2018-04-27", "All of the permanents put onto the battlefield this way enter at the same time. If any have triggered abilities that trigger on something else entering the battlefield, they'll see each other.")
    }
}
