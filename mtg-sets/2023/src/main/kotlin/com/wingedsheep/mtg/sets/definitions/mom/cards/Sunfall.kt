package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Sunfall
 * {3}{W}{W}
 * Sorcery
 * Exile all creatures. Incubate X, where X is the number of creatures exiled this way.
 *
 * X counts only the creatures that actually reached exile ([moveTracked]), so a creature a
 * replacement effect kept out of exile doesn't count. With nothing exiled you still incubate 0.
 */
val Sunfall = card("Sunfall") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Exile all creatures. Incubate X, where X is the number of creatures exiled this way. " +
        "(Create an Incubator token with X +1/+1 counters on it and \"{2}: Transform this token.\" " +
        "It transforms into a 0/0 Phyrexian artifact creature.)"

    spell {
        effect = Effects.Pipeline {
            val creatures = gather(
                CardSource.FromZone(
                    zone = Zone.BATTLEFIELD,
                    player = Player.Each,
                    filter = GameObjectFilter.Creature,
                )
            )
            val exiled = moveTracked(creatures, CardDestination.ToZone(Zone.EXILE))
            run(Effects.Incubate(exiled.count))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "40"
        artist = "Kasia 'Kafis' Zielińska"
        flavorText = "\"Let the light scour away your imperfect flesh.\"\n—Heliod"
        imageUri = "https://cards.scryfall.io/normal/front/3/2/32e29c7d-ed4b-4eff-b3c2-d99e5b63ef8d.jpg?1783917046"
        ruling(
            "2023-04-14",
            "If Sunfall resolves but doesn't exile any creatures, you'll incubate 0, creating an Incubator " +
                "token but putting no +1/+1 counters on it. It will still have the ability to transform, but " +
                "you might need to figure out how it's going to survive first."
        )
    }
}
