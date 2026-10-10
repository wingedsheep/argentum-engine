package com.wingedsheep.mtg.sets.definitions.tmp.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Living Death
 * {3}{B}{B}
 * Sorcery
 * Each player exiles all creature cards from their graveyard, then sacrifices all creatures
 * they control, then puts all cards they exiled this way onto the battlefield.
 *
 * Three batch moves: the graveyard creatures are exiled with [moveTracked] so only the cards
 * that actually reached exile come back ("exiled this way"); the creatures sacrificed in the
 * middle step are never in that collection, so they stay in the graveyard. The returned cards
 * enter under their owners' control.
 */
val LivingDeath = card("Living Death") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Each player exiles all creature cards from their graveyard, then sacrifices all " +
        "creatures they control, then puts all cards they exiled this way onto the battlefield."

    spell {
        effect = Effects.Pipeline {
            val graveyardCreatures = gather(
                CardSource.FromZone(
                    zone = Zone.GRAVEYARD,
                    player = Player.Each,
                    filter = GameObjectFilter.Creature
                )
            )
            val exiled = moveTracked(graveyardCreatures, CardDestination.ToZone(Zone.EXILE))
            val battlefieldCreatures = gather(GameObjectFilter.Creature, player = Player.Each)
            sacrifice(battlefieldCreatures)
            move(exiled, CardDestination.ToZone(Zone.BATTLEFIELD), underOwnersControl = true)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "142"
        artist = "Charles Gillespie"
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6c820476-fbda-4073-baf6-51e71f45ed58.jpg?1783946637"
        ruling(
            "2018-03-16",
            "As Living Death resolves, all players exile their creature cards from graveyards at the same time. " +
                "Then all players sacrifice all creatures they control at the same time. Then all players put all " +
                "creatures they exiled onto the battlefield at the same time."
        )
        ruling(
            "2018-03-16",
            "Only cards exiled by Living Death's first instruction are put onto the battlefield. If a replacement " +
                "effect (such as that of Leyline of the Void) causes any of the sacrificed creatures to be exiled " +
                "instead of put into a graveyard, those cards aren't returned to the battlefield."
        )
    }
}
