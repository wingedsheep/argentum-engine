package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.GatherUntilMatchEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.RevealCollectionEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player

/**
 * The Ring Goes South
 * {3}{G}
 * Sorcery
 *
 * The Ring tempts you. Then reveal cards from the top of your library until you reveal X land
 * cards, where X is the number of legendary creatures you control. Put those land cards onto
 * the battlefield tapped and the rest on the bottom of your library in a random order.
 *
 * Composes the Ring tempt with GatherUntilMatch (X = legendary creatures you control) +
 * two filtered MoveCollections: lands → battlefield tapped, the rest → bottom of library at
 * random.
 */
val TheRingGoesSouth = card("The Ring Goes South") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "The Ring tempts you. Then reveal cards from the top of your library until you " +
        "reveal X land cards, where X is the number of legendary creatures you control. Put those " +
        "land cards onto the battlefield tapped and the rest on the bottom of your library in a " +
        "random order."

    spell {
        effect = Effects.TheRingTemptsYou()
            .then(
                GatherUntilMatchEffect(
                    player = Player.You,
                    filter = GameObjectFilter.Land,
                    count = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature.legendary()).count(),
                    storeMatch = "lands",
                    storeRevealed = "allRevealed"
                )
            )
            .then(RevealCollectionEffect(from = "allRevealed"))
            .then(
                MoveCollectionEffect(
                    from = "allRevealed",
                    filter = GameObjectFilter.Land,
                    destination = CardDestination.ToZone(
                        Zone.BATTLEFIELD,
                        player = Player.You,
                        placement = ZonePlacement.Tapped
                    )
                )
            )
            .then(
                MoveCollectionEffect(
                    from = "allRevealed",
                    filter = GameObjectFilter.Nonland,
                    destination = CardDestination.ToZone(
                        Zone.LIBRARY,
                        player = Player.You,
                        placement = ZonePlacement.Bottom
                    ),
                    order = CardOrder.Random
                )
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "186"
        artist = "Wangjie Li"
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3c8a4c7d-527c-49ea-a115-a9e747c0fd03.jpg?1686969579"
    }
}
