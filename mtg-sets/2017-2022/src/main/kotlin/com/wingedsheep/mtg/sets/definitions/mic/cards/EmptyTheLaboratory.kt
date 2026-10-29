package com.wingedsheep.mtg.sets.definitions.mic.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Empty the Laboratory — Innistrad: Midnight Hunt Commander #14 (canonical printing)
 * {X}{U}{U} · Sorcery
 *
 * Sacrifice X Zombies, then reveal cards from the top of your library until you reveal a number of
 * Zombie creature cards equal to the number of Zombies sacrificed this way. Put those cards onto the
 * battlefield and the rest on the bottom of your library in a random order.
 *
 * The sacrifice is part of the resolution, not a cost: you choose X of your Zombies as the spell
 * resolves (all of them when you control fewer than X), and the reveal count is the number actually
 * sacrificed — `DynamicAmounts.permanentsSacrificedThisWay()`, which the pipeline `sacrifice` step
 * records. The matched Zombie cards enter simultaneously (the card's ruling); every other revealed
 * card goes to the bottom in a random order.
 */
val EmptyTheLaboratory = card("Empty the Laboratory") {
    manaCost = "{X}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Sacrifice X Zombies, then reveal cards from the top of your library until you reveal " +
        "a number of Zombie creature cards equal to the number of Zombies sacrificed this way. Put " +
        "those cards onto the battlefield and the rest on the bottom of your library in a random order."

    spell {
        effect = Effects.Pipeline {
            val zombies = gather(
                GameObjectFilter.Permanent.withSubtype(Subtype.ZOMBIE),
                player = Player.You,
            )
            val chosen = chooseExactly(
                DynamicAmounts.xValue(),
                from = zombies,
                prompt = "Choose Zombies to sacrifice",
            )
            sacrifice(chosen)
            val (found, revealed) = gatherUntilMatch(
                GameObjectFilter.Creature.withSubtype(Subtype.ZOMBIE),
                count = DynamicAmounts.permanentsSacrificedThisWay(),
            )
            reveal(revealed, fromZone = Zone.LIBRARY, toZone = Zone.BATTLEFIELD)
            val rest = exclude(revealed, found)
            move(found, CardDestination.ToZone(Zone.BATTLEFIELD))
            toLibraryBottom(rest, order = CardOrder.Random)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "14"
        artist = "Tuan Duong Chu"
        imageUri = "https://cards.scryfall.io/normal/front/1/9/191f6693-32d9-448d-8889-f98cf13c2277.jpg?1783925380"
        ruling("2021-09-24", "The Zombies will enter the battlefield simultaneously. They will see each other enter for the purposes of triggered abilities.")
    }
}
