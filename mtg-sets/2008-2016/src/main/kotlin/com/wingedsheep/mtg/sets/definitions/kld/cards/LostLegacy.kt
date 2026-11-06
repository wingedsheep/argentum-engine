package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.namedFromVariable
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CardNamePool
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Lost Legacy — Kaladesh #88
 * {1}{B}{B} · Sorcery
 *
 * Choose a nonartifact, nonland card name. Search target player's graveyard, hand, and library for
 * any number of cards with that name and exile them. That player shuffles, then draws a card for
 * each card exiled from their hand this way.
 *
 * The Stone Brain's shape with a narrower name pool (no artifact or land names) and no ceiling on
 * how many matches are exiled. The cards picked from the hand are noted before the exile moves
 * them, so the draw counts exactly those.
 */
val LostLegacy = card("Lost Legacy") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Choose a nonartifact, nonland card name. Search target player's graveyard, hand, " +
        "and library for any number of cards with that name and exile them. That player shuffles, " +
        "then draws a card for each card exiled from their hand this way."

    spell {
        val player = target(Targets.Player)
        effect = Effects.Pipeline {
            val chosenName = chooseCardName(pool = CardNamePool.NONARTIFACT_NONLAND)
            val matches = gather(
                CardSource.FromMultipleZones(
                    zones = listOf(Zone.GRAVEYARD, Zone.HAND, Zone.LIBRARY),
                    player = player.asPlayer,
                    filter = GameObjectFilter.Any.namedFromVariable(chosenName)
                ),
                search = true
            )
            val toExile = chooseAnyNumber(
                from = matches,
                prompt = "Choose any number of cards with that name to exile",
                selectedLabel = "Exile"
            )
            val fromHand = filter(toExile, GameObjectFilter.Any.currentlyIn(Zone.HAND))
            exile(toExile, owner = player.asPlayer)
            run(Effects.ShuffleLibrary(target = player))
            run(Effects.DrawCards(fromHand.count, player))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "88"
        artist = "Greg Opalinski"
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d5d0e447-d98e-43d5-9b53-166221c34be2.jpg?1783937205"
    }
}
