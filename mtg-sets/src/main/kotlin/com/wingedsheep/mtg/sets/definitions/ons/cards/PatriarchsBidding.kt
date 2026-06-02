package com.wingedsheep.mtg.sets.definitions.ons.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EachPlayerChoosesCreatureTypeEffect
import com.wingedsheep.sdk.scripting.effects.ForEachPlayerEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.dsl.Effects

/**
 * Patriarch's Bidding
 * {3}{B}{B}
 * Sorcery
 * Each player chooses a creature type. Each player returns all creature cards of a type
 * chosen this way from their graveyard to the battlefield.
 */
val PatriarchsBidding = card("Patriarch's Bidding") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Each player chooses a creature type. Each player returns all creature cards of a type chosen this way from their graveyard to the battlefield."

    spell {
        effect = Effects.Composite(
            listOf(
                EachPlayerChoosesCreatureTypeEffect(storeAs = "biddingTypes"),
                ForEachPlayerEffect(
                    players = Player.ActivePlayerFirst,
                    effects = listOf(
                        GatherCardsEffect(
                            source = CardSource.FromZone(
                                zone = Zone.GRAVEYARD,
                                player = Player.You,
                                filter = GameObjectFilter.Creature.withSubtypeInStoredList("biddingTypes")
                            ),
                            storeAs = "toReturn"
                        ),
                        MoveCollectionEffect(
                            from = "toReturn",
                            destination = CardDestination.ToZone(Zone.BATTLEFIELD)
                        )
                    )
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "161"
        artist = "Carl Critchlow"
        flavorText = ""
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2deba175-8c02-492d-b404-5d842910c095.jpg?1562905776"
    }
}
