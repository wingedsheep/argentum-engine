package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Over the Top — The Brothers' War #146
 * {5}{R}{R} · Sorcery
 *
 * Each player reveals a number of cards from the top of their library equal to the number of
 * nonland permanents they control, puts all permanent cards they revealed this way onto the
 * battlefield, and puts the rest into their graveyard.
 *
 * Every player's count and reveal happen first, in a [forEachPlayerCollecting] pass, so no
 * player's count sees permanents another player's reveal put onto the battlefield. Then every
 * player's permanent cards enter in one simultaneous move under their owners' control, and
 * the nonpermanent cards go to their owners' graveyards. An Aura with nothing it can enchant
 * stays in its owner's library (CR 303.4g) — it is a permanent card, so it isn't "the rest".
 */
val OverTheTop = card("Over the Top") {
    manaCost = "{5}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Each player reveals a number of cards from the top of their library equal to the " +
        "number of nonland permanents they control, puts all permanent cards they revealed this way " +
        "onto the battlefield, and puts the rest into their graveyard."

    spell {
        effect = Effects.Pipeline {
            val (permanentCards, rest) = forEachPlayerCollecting(Player.ActivePlayerFirst) {
                val nonland = gather(CardSource.ControlledPermanents(Player.You, GameObjectFilter.NonlandPermanent))
                val revealed = gather(CardSource.TopOfLibrary(nonland.count), revealed = true)
                val (permanents, others) = filterSplit(revealed, GameObjectFilter.Permanent)
                listOf(permanents, others)
            }
            move(permanentCards, CardDestination.ToZone(Zone.BATTLEFIELD), underOwnersControl = true)
            move(rest, CardDestination.ToZone(Zone.GRAVEYARD))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "146"
        artist = "Cristi Balanescu"
        imageUri = "https://cards.scryfall.io/normal/front/0/1/01eb9702-55c5-41b2-8bef-63d33dafa599.jpg?1783920066"
    }
}
