package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Turtles in Time
 * {5}{U}{U}
 * Sorcery
 *
 * Return all creatures to their owners' hands. Each player may shuffle their hand and
 * graveyard into their library, then each player who does draws seven cards.
 * Exile Turtles in Time.
 */
val TurtlesInTime = card("Turtles in Time") {
    manaCost = "{5}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Return all creatures to their owners' hands. Each player may shuffle their hand and graveyard into their library, then each player who does draws seven cards.\nExile Turtles in Time."

    spell {
        effect = Effects.Composite(
            listOf(
                Patterns.Group.returnAllToHand(GroupFilter.AllCreatures),
                // ForEachPlayer sets the iterated player as the controller, so Player.You inside
                // resolves to each player in turn — the gather/shuffle/draw is theirs, and the
                // "may" lets each player decide independently ("each player who does draws seven").
                Effects.ForEachPlayer(
                    Player.Each,
                    listOf(
                        MayEffect(
                            Effects.Composite(
                                listOf(
                                    GatherCardsEffect(
                                        source = CardSource.FromMultipleZones(
                                            zones = listOf(Zone.HAND, Zone.GRAVEYARD),
                                            player = Player.You
                                        ),
                                        storeAs = "turtlesInTimeShuffle"
                                    ),
                                    MoveCollectionEffect(
                                        from = "turtlesInTimeShuffle",
                                        destination = CardDestination.ToZone(Zone.LIBRARY, Player.You, ZonePlacement.Shuffled)
                                    ),
                                    Effects.DrawCards(7)
                                )
                            )
                        )
                    )
                )
            )
        )
        selfExile()
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "55"
        artist = "Inkognit"
        flavorText = "\"When are we now?\"\n—Donatello"
        imageUri = "https://cards.scryfall.io/normal/front/b/d/bdb3efe7-7b12-4503-83e9-7977eb099db5.jpg?1769005754"
    }
}
